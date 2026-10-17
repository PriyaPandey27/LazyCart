package com.lazycart.app.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI State for the Add/Edit Pantry Item screen.
 */
data class AddEditUiState(
    val name: String = "",
    val category: ItemCategory = ItemCategory.OTHER,
    val quantity: String = "",
    val unit: ItemUnit = ItemUnit.PIECES,
    val purchaseDate: LocalDate = LocalDate.now(),
    val expiryDate: LocalDate? = null,
    val priceHint: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val nameError: String? = null,
    val quantityError: String? = null
)

/**
 * ViewModel responsible for creating new pantry items or editing existing items.
 */
@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialItemId: Long? = savedStateHandle.get<Long>("itemId")?.takeIf { it > 0L }
        ?: savedStateHandle.get<String>("itemId")?.toLongOrNull()?.takeIf { it > 0L }

    private var activeItemId: Long? = initialItemId

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (initialItemId != null) {
            loadItem(initialItemId)
        }
    }

    /**
     * Loads the pantry item data to edit into state.
     */
    fun loadItem(id: Long) {
        activeItemId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val item = pantryRepository.getItemById(id)
                if (item != null) {
                    _uiState.update {
                        it.copy(
                            name = item.name,
                            category = item.category,
                            quantity = if (item.quantity % 1f == 0f) item.quantity.toInt().toString() else item.quantity.toString(),
                            unit = item.unit,
                            purchaseDate = item.purchaseDate,
                            expiryDate = item.expiryDate,
                            priceHint = if (item.priceHint > 0f) {
                                if (item.priceHint % 1f == 0f) item.priceHint.toInt().toString() else item.priceHint.toString()
                            } else "",
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Item not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load item"
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = if (name.isNotBlank()) null else it.nameError
            )
        }
    }

    fun updateCategory(category: ItemCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun updateQuantity(quantity: String) {
        _uiState.update {
            it.copy(
                quantity = quantity,
                quantityError = if (quantity.toFloatOrNull()?.let { q -> q > 0f } == true) null else it.quantityError
            )
        }
    }

    fun updateUnit(unit: ItemUnit) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun updatePurchaseDate(date: LocalDate) {
        _uiState.update { it.copy(purchaseDate = date) }
    }

    fun updateExpiryDate(date: LocalDate?) {
        _uiState.update { it.copy(expiryDate = date) }
    }

    fun updatePriceHint(priceHint: String) {
        _uiState.update { it.copy(priceHint = priceHint) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Validates form inputs and saves or updates the pantry item.
     */
    fun saveItem() {
        val currentState = _uiState.value

        var hasError = false
        var nameError: String? = null
        var quantityError: String? = null

        if (currentState.name.isBlank()) {
            nameError = "Item name cannot be empty"
            hasError = true
        }

        val parsedQuantity = currentState.quantity.toFloatOrNull()
        if (parsedQuantity == null || parsedQuantity <= 0f) {
            quantityError = "Enter a valid quantity greater than 0"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    quantityError = quantityError
                )
            }
            return
        }

        val safeQuantity = parsedQuantity ?: return
        val parsedPrice = currentState.priceHint.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentId = activeItemId
                if (currentId != null && currentId > 0L) {
                    val existingItem = pantryRepository.getItemById(currentId)
                    val updatedItem = if (existingItem != null) {
                        existingItem.copy(
                            name = currentState.name.trim(),
                            category = currentState.category,
                            quantity = safeQuantity,
                            unit = currentState.unit,
                            purchaseDate = currentState.purchaseDate,
                            expiryDate = currentState.expiryDate,
                            priceHint = parsedPrice
                        )
                    } else {
                        PantryItem(
                            id = currentId,
                            name = currentState.name.trim(),
                            category = currentState.category,
                            quantity = safeQuantity,
                            unit = currentState.unit,
                            purchaseDate = currentState.purchaseDate,
                            expiryDate = currentState.expiryDate,
                            priceHint = parsedPrice
                        )
                    }
                    pantryRepository.updateItem(updatedItem)
                } else {
                    val newItem = PantryItem(
                        name = currentState.name.trim(),
                        category = currentState.category,
                        quantity = safeQuantity,
                        unit = currentState.unit,
                        purchaseDate = currentState.purchaseDate,
                        expiryDate = currentState.expiryDate,
                        priceHint = parsedPrice
                    )
                    pantryRepository.addItem(newItem)
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to save item"
                    )
                }
            }
        }
    }
}
