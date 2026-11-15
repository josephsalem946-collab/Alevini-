package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aleviniSecurityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alevini_security_prefs"
)

class SecurityPreferencesRepository(private val context: Context) {

    private val biometricLockKey = booleanPreferencesKey("biometric_lock_enabled")
    private val checkedSecurityItemsKey = stringSetPreferencesKey("checked_security_items")
    private val balanceHiddenKey = booleanPreferencesKey("balance_hidden")
    private val languageCodeKey = stringPreferencesKey("selected_language_code")
    private val dismissedNotificationsKey = stringSetPreferencesKey("dismissed_notification_ids")

    val isBiometricLockEnabled: Flow<Boolean> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[biometricLockKey] ?: false }

    val checkedSecurityItems: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[checkedSecurityItemsKey] ?: DEFAULT_DONE_ITEMS }

    val isBalanceHidden: Flow<Boolean> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[balanceHiddenKey] ?: false }

    val selectedLanguageCode: Flow<String> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[languageCodeKey] ?: "fr" }

    val dismissedNotificationIds: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[dismissedNotificationsKey] ?: emptySet() }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[biometricLockKey] = enabled
        }
    }

    suspend fun toggleSecurityItem(id: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[checkedSecurityItemsKey] ?: DEFAULT_DONE_ITEMS
            prefs[checkedSecurityItemsKey] = if (id in current) current - id else current + id
        }
    }

    suspend fun resetSecurityChecklist() {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[checkedSecurityItemsKey] = emptySet()
        }
    }

    suspend fun setBalanceHidden(hidden: Boolean) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[balanceHiddenKey] = hidden
        }
    }

    suspend fun setSelectedLanguageCode(code: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[languageCodeKey] = code
        }
    }

    suspend fun dismissNotification(id: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedNotificationsKey] ?: emptySet()
            prefs[dismissedNotificationsKey] = current + id
        }
    }

    suspend fun dismissAllNotifications(ids: Set<String>) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedNotificationsKey] ?: emptySet()
            prefs[dismissedNotificationsKey] = current + ids
        }
    }

    companion object {
        // Items already enforced natively by Alevini's architecture & Firestore security rules
        val DEFAULT_DONE_ITEMS = setOf(
            "0-0-0", "0-0-1", "0-0-2",
            "0-1-0", "0-1-1",
            "0-2-0", "0-2-1",
            "0-5-0"
        )
    }
}
