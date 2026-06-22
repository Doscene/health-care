package com.healthcare.family.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "healthcare_tokens")

/**
 * 基于 DataStore 的 Token 管理器。
 * 存储 accessToken / refreshToken / userId / userRole / isElderlyMode。
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.tokenDataStore

    val accessToken: Flow<String?> = dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[KEY_REFRESH_TOKEN] }
    val userId: Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }
    val userRole: Flow<String?> = dataStore.data.map { it[KEY_USER_ROLE] }
    val isElderlyMode: Flow<Boolean> = dataStore.data.map { it[KEY_ELDERLY_MODE] ?: false }
    val pushMedicationReminder: Flow<Boolean> = dataStore.data.map { it[KEY_PUSH_MED_REMINDER] ?: true }
    val pushRiskAlert: Flow<Boolean> = dataStore.data.map { it[KEY_PUSH_RISK_ALERT] ?: true }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map {
        !it[KEY_ACCESS_TOKEN].isNullOrEmpty()
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { it[KEY_USER_ID] = userId }
    }

    suspend fun saveUserRole(role: String) {
        dataStore.edit { it[KEY_USER_ROLE] = role }
    }

    suspend fun setElderlyMode(enabled: Boolean) {
        dataStore.edit { it[KEY_ELDERLY_MODE] = enabled }
    }

    suspend fun setPushMedicationReminder(enabled: Boolean) {
        dataStore.edit { it[KEY_PUSH_MED_REMINDER] = enabled }
    }

    suspend fun setPushRiskAlert(enabled: Boolean) {
        dataStore.edit { it[KEY_PUSH_RISK_ALERT] = enabled }
    }

    suspend fun getAccessTokenSync(): String? {
        return dataStore.data.first()[KEY_ACCESS_TOKEN]
    }

    suspend fun getRefreshTokenSync(): String? {
        return dataStore.data.first()[KEY_REFRESH_TOKEN]
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_ELDERLY_MODE = booleanPreferencesKey("elderly_mode")
        private val KEY_PUSH_MED_REMINDER = booleanPreferencesKey("push_med_reminder")
        private val KEY_PUSH_RISK_ALERT = booleanPreferencesKey("push_risk_alert")
    }
}
