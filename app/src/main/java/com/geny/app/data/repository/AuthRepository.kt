package com.geny.app.data.repository

import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.core.storage.TokenManager
import com.geny.app.data.api.AuthApi
import com.geny.app.data.dto.LoginRequest
import com.geny.app.data.dto.SetupRequest
import com.geny.app.domain.model.AuthState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val settingsDataStore: SettingsDataStore
) {
    /**
     * Attempt auto-login using saved credentials.
     * 1. Check if auto-login is enabled and credentials exist
     * 2. Set the server URL
     * 3. Try existing token first, then re-login with saved password
     */
    suspend fun tryAutoLogin(): Result<AuthState> = runCatching {
        if (!tokenManager.isAutoLoginEnabled()) {
            return@runCatching AuthState.NoServerUrl
        }

        val credentials = tokenManager.getSavedCredentials()
            ?: return@runCatching AuthState.NoServerUrl

        // Set server URL so interceptors pick it up
        settingsDataStore.setServerUrl(credentials.serverUrl)

        // Try existing token
        if (tokenManager.isLoggedIn()) {
            try {
                val user = authApi.getCurrentUser()
                return@runCatching AuthState.Authenticated(
                    username = user.username,
                    displayName = user.displayName
                )
            } catch (_: Exception) {
                tokenManager.clearToken()
            }
        }

        // Re-login with saved credentials
        val response = authApi.login(
            LoginRequest(credentials.username, credentials.password)
        )
        tokenManager.saveToken(response.token)
        AuthState.Authenticated(
            username = response.username ?: credentials.username,
            displayName = response.displayName
        )
    }

    /**
     * Check auth state for a given server URL (manual flow).
     */
    suspend fun checkAuthState(serverUrl: String): Result<AuthState> = runCatching {
        if (serverUrl.isBlank()) {
            return@runCatching AuthState.NoServerUrl
        }

        // Set server URL
        settingsDataStore.setServerUrl(serverUrl)

        val status = authApi.getAuthStatus()
        if (!status.hasUsers) {
            AuthState.NoUsers
        } else {
            AuthState.Unauthenticated
        }
    }

    /**
     * Login with URL + credentials. Optionally saves for auto-login.
     */
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        autoLogin: Boolean
    ): Result<AuthState.Authenticated> = runCatching {
        // Ensure URL is set
        settingsDataStore.setServerUrl(serverUrl)

        val response = authApi.login(LoginRequest(username, password))
        tokenManager.saveToken(response.token)

        // Save auto-login
        if (autoLogin) {
            tokenManager.saveCredentials(serverUrl, username, password)
            tokenManager.setAutoLogin(true)
        } else {
            tokenManager.setAutoLogin(false)
            tokenManager.clearCredentials()
        }

        AuthState.Authenticated(
            username = response.username ?: username,
            displayName = response.displayName
        )
    }

    /**
     * Initial setup (first-time admin creation).
     */
    suspend fun setup(
        serverUrl: String,
        username: String,
        password: String,
        displayName: String?,
        autoLogin: Boolean
    ): Result<AuthState.Authenticated> = runCatching {
        settingsDataStore.setServerUrl(serverUrl)

        val response = authApi.setup(SetupRequest(username, password, displayName))
        tokenManager.saveToken(response.token)

        if (autoLogin) {
            tokenManager.saveCredentials(serverUrl, username, password)
            tokenManager.setAutoLogin(true)
        }

        AuthState.Authenticated(
            username = response.username ?: username,
            displayName = response.displayName ?: displayName
        )
    }

    fun logout() {
        tokenManager.clearToken()
        // Keep credentials if auto-login was enabled (user can re-login from the screen)
        // Only clear auto-login flag
        tokenManager.setAutoLogin(false)
    }

    fun fullLogout() {
        tokenManager.clearAll()
    }
}
