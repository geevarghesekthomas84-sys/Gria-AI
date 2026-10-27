package com.griaai.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.griaai.app.domain.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gria_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_IP_ADDRESS = stringPreferencesKey("ip_address")
        val KEY_PORT = intPreferencesKey("port")
        val KEY_USE_HTTPS = booleanPreferencesKey("use_https")
        val KEY_TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val KEY_DEFAULT_MODEL = stringPreferencesKey("default_model")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_MAX_CONTEXT_MESSAGES = intPreferencesKey("max_context_messages")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val serverConfigFlow: Flow<ServerConfig> = context.dataStore.data.map { prefs ->
        val rawPrompt = prefs[KEY_SYSTEM_PROMPT]
        val cleanPrompt = when {
            rawPrompt.isNullOrBlank() -> "You are Gria-AI, a helpful, precise, and thoughtful on-device & local AI assistant."
            rawPrompt.contains("Nexus", ignoreCase = true) -> rawPrompt
                .replace("Nexus AI", "Gria-AI", ignoreCase = true)
                .replace("nexus ai", "Gria-AI", ignoreCase = true)
                .replace("Nexus", "Gria-AI", ignoreCase = true)
            else -> rawPrompt
        }
        ServerConfig(
            ipAddress = prefs[KEY_IP_ADDRESS] ?: "192.168.1.100",
            port = prefs[KEY_PORT] ?: 1234,
            useHttps = prefs[KEY_USE_HTTPS] ?: false,
            timeoutSeconds = prefs[KEY_TIMEOUT_SECONDS] ?: 60,
            defaultModel = prefs[KEY_DEFAULT_MODEL] ?: "",
            temperature = prefs[KEY_TEMPERATURE] ?: 0.7f,
            maxTokens = prefs[KEY_MAX_TOKENS] ?: 2048,
            systemPrompt = cleanPrompt,
            isDarkTheme = prefs[KEY_IS_DARK_THEME] ?: false,
            maxContextMessages = prefs[KEY_MAX_CONTEXT_MESSAGES] ?: 20
        )
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun migrateOldSystemPrompt() {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_SYSTEM_PROMPT]
            if (raw != null && raw.contains("Nexus", ignoreCase = true)) {
                prefs[KEY_SYSTEM_PROMPT] = raw
                    .replace("Nexus AI", "Gria-AI", ignoreCase = true)
                    .replace("nexus ai", "Gria-AI", ignoreCase = true)
                    .replace("Nexus", "Gria-AI", ignoreCase = true)
            }
        }
    }

    suspend fun updateServerConfig(config: ServerConfig) {
        val sanitizedPrompt = if (config.systemPrompt.contains("Nexus", ignoreCase = true)) {
            config.systemPrompt
                .replace("Nexus AI", "Gria-AI", ignoreCase = true)
                .replace("nexus ai", "Gria-AI", ignoreCase = true)
                .replace("Nexus", "Gria-AI", ignoreCase = true)
        } else {
            config.systemPrompt
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_IP_ADDRESS] = config.ipAddress
            prefs[KEY_PORT] = config.port
            prefs[KEY_USE_HTTPS] = config.useHttps
            prefs[KEY_TIMEOUT_SECONDS] = config.timeoutSeconds
            prefs[KEY_DEFAULT_MODEL] = config.defaultModel
            prefs[KEY_TEMPERATURE] = config.temperature
            prefs[KEY_MAX_TOKENS] = config.maxTokens
            prefs[KEY_SYSTEM_PROMPT] = sanitizedPrompt
            prefs[KEY_IS_DARK_THEME] = config.isDarkTheme
            prefs[KEY_MAX_CONTEXT_MESSAGES] = config.maxContextMessages
        }
    }

    suspend fun setDefaultModel(modelId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_MODEL] = modelId
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_THEME] = isDark
        }
    }
}
