package com.geny.app.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geny_settings")

data class AppSettings(
    val serverUrl: String = "",
    val themeMode: String = "system" // "system", "light", "dark"
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val lastVTuberAgentIdKey = stringPreferencesKey("last_vtuber_agent_id")
    private val ttsEnabledKey = booleanPreferencesKey("tts_enabled")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serverUrl = prefs[serverUrlKey] ?: "",
            themeMode = prefs[themeModeKey] ?: "system"
        )
    }

    val serverUrl: String
        get() = runBlocking {
            context.dataStore.data.first()[serverUrlKey] ?: ""
        }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = url.trimEnd('/')
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode
        }
    }

    val lastVTuberAgentId: String
        get() = runBlocking {
            context.dataStore.data.first()[lastVTuberAgentIdKey] ?: ""
        }

    suspend fun setLastVTuberAgentId(agentId: String) {
        context.dataStore.edit { prefs ->
            prefs[lastVTuberAgentIdKey] = agentId
        }
    }

    val ttsEnabled: Boolean
        get() = runBlocking {
            context.dataStore.data.first()[ttsEnabledKey] ?: false
        }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ttsEnabledKey] = enabled
        }
    }
}
