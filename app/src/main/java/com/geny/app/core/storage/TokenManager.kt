package com.geny.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SavedCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "geny_secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // --- JWT Token ---

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null

    // --- Auto Login Credentials ---

    fun saveCredentials(serverUrl: String, username: String, password: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl.trimEnd('/'))
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getSavedCredentials(): SavedCredentials? {
        val url = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        if (url.isBlank() || username.isBlank()) return null
        return SavedCredentials(url, username, password)
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_SERVER_URL)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    // --- Auto Login Toggle ---

    fun setAutoLogin(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }

    fun isAutoLoginEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOGIN, false)
    }

    // --- Clear All ---

    fun clearAll() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_SERVER_URL)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_AUTO_LOGIN)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_SERVER_URL = "saved_server_url"
        private const val KEY_USERNAME = "saved_username"
        private const val KEY_PASSWORD = "saved_password"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
    }
}
