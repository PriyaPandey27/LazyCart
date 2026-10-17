package com.lazycart.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazycart.app.data.local.datastore.UserPreferencesDataStore
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import com.lazycart.app.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Settings screen.
 *
 * @param restockRemindersEnabled Whether notifications for low stock predictions are enabled.
 * @param expiryRemindersEnabled Whether notifications for approaching item expiration are enabled.
 * @param restockDaysBefore Days in advance to notify before restock predicted date (1..7).
 * @param expiryDaysBefore Days in advance to notify before item expiry date (1..7).
 * @param showClearDataConfirm Whether the destructive confirmation dialog for wiping data is visible.
 * @param isClearingData Indicates an active clear-all operation across repositories.
 * @param errorMessage Any error encountered during data operations to display to the user.
 */
data class SettingsUiState(
    val restockRemindersEnabled: Boolean = true,
    val expiryRemindersEnabled: Boolean = true,
    val restockDaysBefore: Int = 3,
    val expiryDaysBefore: Int = 2,
    val showClearDataConfirm: Boolean = false,
    val isClearingData: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Transient state for UI controls not persisted to DataStore.
 */
private data class TransientSettingsState(
    val showClearDataConfirm: Boolean = false,
    val isClearingData: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing notification preferences and data management actions.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesDataStore,
    private val pantryRepository: PantryRepository,
    private val consumptionRepository: ConsumptionRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _transientState = MutableStateFlow(TransientSettingsState())

    val uiState: StateFlow<SettingsUiState> = combine(
        userPrefs.isRestockRemindersEnabled,
        userPrefs.isExpiryRemindersEnabled,
        userPrefs.restockReminderDaysBefore,
        userPrefs.expiryReminderDaysBefore,
        _transientState
    ) { restockEnabled, expiryEnabled, restockDays, expiryDays, transient ->
        SettingsUiState(
            restockRemindersEnabled = restockEnabled,
            expiryRemindersEnabled = expiryEnabled,
            restockDaysBefore = restockDays,
            expiryDaysBefore = expiryDays,
            showClearDataConfirm = transient.showClearDataConfirm,
            isClearingData = transient.isClearingData,
            errorMessage = transient.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setRestockRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setRestockRemindersEnabled(enabled)
        }
    }

    fun setExpiryRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setExpiryRemindersEnabled(enabled)
        }
    }

    fun setRestockDaysBefore(days: Int) {
        viewModelScope.launch {
            userPrefs.setRestockReminderDaysBefore(days)
        }
    }

    fun setExpiryDaysBefore(days: Int) {
        viewModelScope.launch {
            userPrefs.setExpiryReminderDaysBefore(days)
        }
    }

    fun showClearDataConfirmation() {
        _transientState.update { it.copy(showClearDataConfirm = true) }
    }

    fun dismissClearDataConfirmation() {
        _transientState.update { it.copy(showClearDataConfirm = false) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _transientState.update { it.copy(isClearingData = true, errorMessage = null) }
            try {
                pantryRepository.deleteAllItems()
                consumptionRepository.deleteAllEvents()
                shoppingListRepository.deleteAllItems()
                _transientState.update {
                    it.copy(
                        isClearingData = false,
                        showClearDataConfirm = false
                    )
                }
            } catch (e: Exception) {
                _transientState.update {
                    it.copy(
                        isClearingData = false,
                        errorMessage = e.localizedMessage ?: "Failed to clear all data"
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _transientState.update { it.copy(errorMessage = null) }
    }
}
