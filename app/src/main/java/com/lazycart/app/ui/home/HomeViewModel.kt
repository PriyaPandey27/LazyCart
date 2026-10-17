package com.lazycart.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazycart.app.data.local.datastore.UserPreferencesDataStore
import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import com.lazycart.app.domain.repository.ShoppingListRepository
import com.lazycart.app.domain.usecase.RestockPredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * UI state for the Home / Dashboard screen.
 *
 * @param greeting Time-of-day greeting ("Good morning", "Good afternoon", "Good evening").
 * @param todayDate Human-friendly formatted date, e.g. "Saturday, Oct 17".
 * @param totalItemCount Total number of active pantry items.
 * @param itemsRunningSoon Items predicted to run out within 5 days, sorted by days remaining.
 * @param itemsExpiringSoon Items with expiry date within 3 days, sorted by expiry date.
 * @param isLoading Whether data is currently being fetched or calculated.
 * @param errorMessage Any error message to be displayed in a snackbar or banner.
 * @param allItems Complete list of active pantry items (with predictions attached) for quick actions.
 */
data class HomeUiState(
    val greeting: String = "",
    val todayDate: String = "",
    val totalItemCount: Int = 0,
    val itemsRunningSoon: List<PantryItem> = emptyList(),
    val itemsExpiringSoon: List<PantryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allItems: List<PantryItem> = emptyList()
)

/**
 * ViewModel for the Home screen.
 * Combines active pantry items with consumption history to run restock predictions,
 * tracks expiring items, and provides quick consumption logging.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val consumptionRepository: ConsumptionRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            greeting = computeGreeting(),
            todayDate = computeTodayDate(),
            isLoading = true
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    /**
     * Observes active pantry items combined with user preferences,
     * computes restock predictions using [RestockPredictionEngine],
     * and filters running soon / expiring soon items.
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                pantryRepository.observeActivePantryItems(),
                userPreferencesDataStore.restockReminderDaysBefore,
                userPreferencesDataStore.expiryReminderDaysBefore
            ) { items, restockDaysPref, expiryDaysPref ->
                Triple(items, restockDaysPref, expiryDaysPref)
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load pantry data"
                    )
                }
            }
            .collect { (activeItems, _, _) ->
                try {
                    val today = LocalDate.now()

                    // Attach predictions by combining each pantry item with its consumption history
                    val itemsWithPredictions = activeItems.map { item ->
                        val history = consumptionRepository.getAllEventsForItem(item.id)
                        val prediction = RestockPredictionEngine.predict(
                            item = item,
                            consumptionHistory = history,
                            today = today
                        )
                        item.copy(prediction = prediction)
                    }

                    // itemsRunningSoon = items where prediction.daysRemaining != null && daysRemaining <= 5, sorted by daysRemaining
                    val runningSoon = itemsWithPredictions
                        .filter { item ->
                            val days = item.prediction?.daysRemaining
                            days != null && days <= 5f
                        }
                        .sortedBy { it.prediction?.daysRemaining ?: Float.MAX_VALUE }

                    // itemsExpiringSoon = items that isExpiringSoon(3), sorted by expiryDate
                    val expiringSoon = itemsWithPredictions
                        .filter { it.isExpiringSoon(3) }
                        .sortedWith(compareBy(nullsLast()) { it.expiryDate })

                    _uiState.update { current ->
                        current.copy(
                            greeting = computeGreeting(),
                            todayDate = computeTodayDate(today),
                            totalItemCount = activeItems.size,
                            itemsRunningSoon = runningSoon,
                            itemsExpiringSoon = expiringSoon,
                            isLoading = false,
                            errorMessage = null,
                            allItems = itemsWithPredictions
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Failed to compute predictions"
                        )
                    }
                }
            }
        }
    }

    /**
     * Clears any active error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Quick action to record a consumption event and reduce quantity in pantry.
     */
    fun logConsumption(itemId: Long, quantityConsumed: Float, note: String = "") {
        viewModelScope.launch {
            try {
                val item = pantryRepository.getItemById(itemId) ?: return@launch
                consumptionRepository.logConsumption(
                    ConsumptionEvent(
                        pantryItemId = itemId,
                        quantityConsumed = quantityConsumed,
                        unit = item.unit,
                        timestamp = Instant.now(),
                        note = note
                    )
                )

                val newQuantity = maxOf(0f, item.quantity - quantityConsumed)
                pantryRepository.updateQuantity(itemId, newQuantity)
                if (newQuantity == 0f) {
                    pantryRepository.markAsFinished(itemId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to record consumption")
                }
            }
        }
    }

    /**
     * Quick action to add a pantry item to the shopping list if not already present.
     */
    fun addToShoppingList(item: PantryItem) {
        viewModelScope.launch {
            try {
                shoppingListRepository.addFromPantryItemIfNotPresent(item)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to add to shopping list")
                }
            }
        }
    }

    companion object {
        /**
         * Computes a time-of-day greeting:
         * - "Good morning" before noon
         * - "Good afternoon" 12:00–17:59
         * - "Good evening" otherwise (18:00+)
         */
        fun computeGreeting(now: LocalDateTime = LocalDateTime.now()): String {
            val hour = now.hour
            return when {
                hour < 12 -> "Good morning"
                hour in 12..17 -> "Good afternoon"
                else -> "Good evening"
            }
        }

        /**
         * Formats date as 'Saturday, Oct 17'
         */
        fun computeTodayDate(today: LocalDate = LocalDate.now()): String {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
            return today.format(formatter)
        }
    }
}
