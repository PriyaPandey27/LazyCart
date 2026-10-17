package com.lazycart.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "LazyCart_prefs")

/**
 * Lightweight preference store backed by DataStore.
 * Only stores settings that don't need Room-level querying.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val RESTOCK_REMINDERS_ENABLED = booleanPreferencesKey("restock_reminders_enabled")
        val EXPIRY_REMINDERS_ENABLED = booleanPreferencesKey("expiry_reminders_enabled")
        val RESTOCK_REMINDER_DAYS_BEFORE = intPreferencesKey("restock_reminder_days_before")
        val EXPIRY_REMINDER_DAYS_BEFORE = intPreferencesKey("expiry_reminder_days_before")
        val SHOPPING_BUDGET = floatPreferencesKey("shopping_budget")
    }

    val isOnboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    val isRestockRemindersEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.RESTOCK_REMINDERS_ENABLED] ?: true }

    val isExpiryRemindersEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.EXPIRY_REMINDERS_ENABLED] ?: true }

    val restockReminderDaysBefore: Flow<Int> =
        context.dataStore.data.map { it[Keys.RESTOCK_REMINDER_DAYS_BEFORE] ?: 3 }

    val expiryReminderDaysBefore: Flow<Int> =
        context.dataStore.data.map { it[Keys.EXPIRY_REMINDER_DAYS_BEFORE] ?: 2 }

    val shoppingBudget: Flow<Float> =
        context.dataStore.data.map { it[Keys.SHOPPING_BUDGET] ?: 0f }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setRestockRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESTOCK_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setExpiryRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EXPIRY_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setRestockReminderDaysBefore(days: Int) {
        context.dataStore.edit { it[Keys.RESTOCK_REMINDER_DAYS_BEFORE] = days }
    }

    suspend fun setExpiryReminderDaysBefore(days: Int) {
        context.dataStore.edit { it[Keys.EXPIRY_REMINDER_DAYS_BEFORE] = days }
    }

    suspend fun setShoppingBudget(budget: Float) {
        context.dataStore.edit { it[Keys.SHOPPING_BUDGET] = budget }
    }
}
