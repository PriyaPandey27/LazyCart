package com.lazycart.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazycart.app.data.local.datastore.UserPreferencesDataStore
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.ShoppingListItem
import com.lazycart.app.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Shopping List screen.
 */
data class ShoppingListUiState(
    val pendingItems: List<ShoppingListItem> = emptyList(),
    val purchasedItems: List<ShoppingListItem> = emptyList(),
    val budget: Float = 0f,
    val totalEstimatedCost: Float = 0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing shopping list items, purchase states, budget tracking,
 * and cost estimations.
 */
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShoppingListUiState> = combine(
        shoppingListRepository.observeAllShoppingItems(),
        userPreferencesDataStore.shoppingBudget,
        _isLoading,
        _errorMessage
    ) { allItems, budget, loading, error ->
        val pending = allItems.filter { !it.isPurchased }
        val purchased = allItems.filter { it.isPurchased }
        val estimatedTotal = pending.sumOf { it.estimatedCost.toDouble() }.toFloat()

        ShoppingListUiState(
            pendingItems = pending,
            purchasedItems = purchased,
            budget = budget,
            totalEstimatedCost = estimatedTotal,
            isLoading = loading,
            errorMessage = error
        )
    }.catch { throwable ->
        _errorMessage.value = throwable.localizedMessage ?: "Failed to load shopping list"
        emit(
            ShoppingListUiState(
                isLoading = false,
                errorMessage = throwable.localizedMessage ?: "Failed to load shopping list"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ShoppingListUiState(isLoading = true)
    )

    fun addManualItem(
        name: String,
        quantity: Float,
        unit: ItemUnit,
        category: ItemCategory = ItemCategory.OTHER,
        priceHint: Float = 0f
    ) {
        if (name.isBlank() || quantity <= 0f) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newItem = ShoppingListItem(
                    pantryItemId = null,
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    priceHint = priceHint,
                    isPurchased = false
                )
                shoppingListRepository.addItem(newItem)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to add shopping item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markPurchased(id: Long) {
        viewModelScope.launch {
            try {
                shoppingListRepository.setItemPurchased(id, true)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to mark item as purchased"
            }
        }
    }

    fun unmarkPurchased(id: Long) {
        viewModelScope.launch {
            try {
                shoppingListRepository.setItemPurchased(id, false)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update item"
            }
        }
    }

    fun removeItem(id: Long) {
        viewModelScope.launch {
            try {
                shoppingListRepository.removeItem(id)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to remove item"
            }
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                shoppingListRepository.clearPurchasedItems()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to clear purchased items"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateBudget(budget: Float) {
        viewModelScope.launch {
            try {
                userPreferencesDataStore.setShoppingBudget(budget.coerceAtLeast(0f))
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update budget"
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
