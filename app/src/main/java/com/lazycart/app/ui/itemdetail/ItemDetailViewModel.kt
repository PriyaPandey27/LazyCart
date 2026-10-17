package com.lazycart.app.ui.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.RestockPrediction
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import com.lazycart.app.domain.repository.ShoppingListRepository
import com.lazycart.app.domain.usecase.RestockPredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * UI state for the Item Detail screen.
 */
data class ItemDetailUiState(
    val item: PantryItem? = null,
    val prediction: RestockPrediction? = null,
    val consumptionHistory: List<ConsumptionEvent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAddedToShoppingList: Boolean = false
)

/**
 * ViewModel managing the details, consumption history, and restock predictions
 * for a specific pantry item.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val consumptionRepository: ConsumptionRepository,
    private val shoppingListRepository: ShoppingListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialItemId: Long = savedStateHandle.get<Long>("itemId")
        ?: savedStateHandle.get<String>("itemId")?.toLongOrNull()
        ?: 0L

    private val _itemId = MutableStateFlow(initialItemId)
    private val _isAddedToShoppingList = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ItemDetailUiState> = _itemId.flatMapLatest { id ->
        if (id <= 0L) {
            flowOf(ItemDetailUiState(isLoading = false, errorMessage = "Item not found"))
        } else {
            combine(
                pantryRepository.observeItemById(id),
                consumptionRepository.observeEventsForItem(id),
                _isAddedToShoppingList,
                _isLoading,
                _errorMessage
            ) { item, events, isAdded, loading, error ->
                val prediction = if (item != null) {
                    RestockPredictionEngine.predict(item = item, consumptionHistory = events)
                } else null

                ItemDetailUiState(
                    item = item?.copy(prediction = prediction),
                    prediction = prediction,
                    consumptionHistory = events.sortedByDescending { it.timestamp },
                    isLoading = loading,
                    errorMessage = error,
                    isAddedToShoppingList = isAdded
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ItemDetailUiState(isLoading = true)
    )

    fun setItemId(id: Long) {
        if (id > 0L && _itemId.value != id) {
            _itemId.value = id
        }
    }

    fun logConsumption(quantity: Float, unit: ItemUnit, note: String = "") {
        val currentItemId = _itemId.value
        if (currentItemId <= 0L) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentItem = pantryRepository.getItemById(currentItemId)
                if (currentItem != null) {
                    val event = ConsumptionEvent(
                        pantryItemId = currentItemId,
                        quantityConsumed = quantity,
                        unit = unit,
                        timestamp = Instant.now(),
                        note = note
                    )
                    consumptionRepository.logConsumption(event)

                    val remainingQty = (currentItem.quantity - quantity).coerceAtLeast(0f)
                    pantryRepository.updateQuantity(currentItemId, remainingQty)
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

    fun deleteEvent(event: ConsumptionEvent) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                consumptionRepository.deleteEvent(event)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete log entry"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToShoppingList() {
        val currentItemId = _itemId.value
        if (currentItemId <= 0L) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentItem = pantryRepository.getItemById(currentItemId)
                if (currentItem != null) {
                    shoppingListRepository.addFromPantryItemIfNotPresent(currentItem)
                    _isAddedToShoppingList.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to add to shopping list"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsFinished() {
        val currentItemId = _itemId.value
        if (currentItemId <= 0L) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                pantryRepository.markAsFinished(currentItemId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to mark item as finished"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
