package com.lazycart.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.RestockPrediction
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import com.lazycart.app.domain.usecase.RestockPredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * UI State for the Pantry screen.
 *
 * @param items Filtered list of active pantry items with predictions attached.
 * @param searchQuery Current text entered in the search bar.
 * @param selectedCategory Currently selected category filter chip, or null for "All".
 * @param isLoading True when initial load or an async mutation is in flight.
 * @param errorMessage User-visible error message if an operation failed.
 */
data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ItemCategory? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel responsible for managing pantry items, reactivity, filtering,
 * restock prediction attachment, and consumption logging.
 */
@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val consumptionRepository: ConsumptionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
    val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<PantryUiState> = combine(
        pantryRepository.observeActivePantryItems(),
        _searchQuery,
        _selectedCategory
    ) { activeItems, query, category ->
        // Attach predictions to each item using its consumption history
        val itemsWithPredictions = activeItems.map { item ->
            val events = consumptionRepository.getAllEventsForItem(item.id)
            val prediction = RestockPredictionEngine.predict(item = item, consumptionHistory = events)
            item.copy(prediction = prediction)
        }

        // Apply search query and category filters
        val filteredItems = itemsWithPredictions.filter { item ->
            val matchesSearch = query.isBlank() || item.name.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            matchesSearch && matchesCategory
        }

        PantryUiState(
            items = filteredItems,
            searchQuery = query,
            selectedCategory = category,
            isLoading = false,
            errorMessage = _errorMessage.value
        )
    }.combine(_errorMessage) { state, error ->
        state.copy(errorMessage = error)
    }.combine(_isLoading) { state, loading ->
        state.copy(isLoading = loading)
    }.catch { throwable ->
        _errorMessage.value = throwable.localizedMessage ?: "Failed to load pantry items"
        emit(
            PantryUiState(
                isLoading = false,
                errorMessage = throwable.localizedMessage ?: "Failed to load pantry items"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PantryUiState(isLoading = true)
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ItemCategory?) {
        _selectedCategory.value = category
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                pantryRepository.deleteItem(item)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsFinished(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                pantryRepository.markAsFinished(id)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to mark item as finished"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logConsumption(itemId: Long, quantity: Float, unit: ItemUnit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentItem = pantryRepository.getItemById(itemId)
                if (currentItem != null) {
                    val event = ConsumptionEvent(
                        pantryItemId = itemId,
                        quantityConsumed = quantity,
                        unit = unit,
                        timestamp = Instant.now()
                    )
                    consumptionRepository.logConsumption(event)

                    val remainingQty = (currentItem.quantity - quantity).coerceAtLeast(0f)
                    pantryRepository.updateQuantity(itemId, remainingQty)
                } else {
                    _errorMessage.value = "Item not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to log consumption"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
