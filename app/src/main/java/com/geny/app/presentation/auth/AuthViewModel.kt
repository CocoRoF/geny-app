package com.geny.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.core.storage.TokenManager
import com.geny.app.data.repository.AuthRepository
import com.geny.app.domain.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val authState: AuthState = AuthState.Unauthenticated,
    // Connection fields - all on one screen
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val displayName: String = "",
    val autoLogin: Boolean = false,
    // UI state
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val autoLoginAttempted: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE
)

enum class ConnectionStatus {
    IDLE,
    CHECKING,
    REACHABLE,
    UNREACHABLE
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        attemptAutoLogin()
    }

    /**
     * On app start: try auto-login with saved credentials.
     * If it fails, pre-fill the form with saved values.
     */
    private fun attemptAutoLogin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Pre-fill from saved credentials
            val saved = tokenManager.getSavedCredentials()
            val autoLoginEnabled = tokenManager.isAutoLoginEnabled()

            if (saved != null) {
                _uiState.value = _uiState.value.copy(
                    serverUrl = saved.serverUrl,
                    username = saved.username,
                    password = saved.password,
                    autoLogin = autoLoginEnabled
                )
            }

            if (autoLoginEnabled && saved != null) {
                // Try auto-login
                authRepository.tryAutoLogin()
                    .onSuccess { state ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authState = state,
                            autoLoginAttempted = true
                        )
                    }
                    .onFailure { e ->
                        // Auto-login failed - show form with pre-filled values
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            autoLoginAttempted = true,
                            error = "Auto-login failed: ${e.message}"
                        )
                    }
            } else {
                // No auto-login - just show form
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    autoLoginAttempted = true
                )
            }
        }
    }

    fun onServerUrlChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            serverUrl = value,
            connectionStatus = ConnectionStatus.IDLE
        )
    }

    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value)
    }

    fun onAutoLoginChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoLogin = enabled)
    }

    /**
     * Test server connection and check if setup is needed.
     */
    fun checkServer() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Server URL is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CHECKING,
                error = null
            )
            authRepository.checkAuthState(url)
                .onSuccess { state ->
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = ConnectionStatus.REACHABLE,
                        authState = state
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = ConnectionStatus.UNREACHABLE,
                        error = "Cannot connect: ${e.message}"
                    )
                }
        }
    }

    /**
     * Connect: set URL + login in one step.
     */
    fun connect() {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) {
            _uiState.value = state.copy(error = "Server URL is required")
            return
        }
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Username and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            // Check if we need setup or login
            val isSetup = state.authState is AuthState.NoUsers

            val result = if (isSetup) {
                authRepository.setup(
                    serverUrl = state.serverUrl.trim(),
                    username = state.username,
                    password = state.password,
                    displayName = state.displayName.takeIf { it.isNotBlank() },
                    autoLogin = state.autoLogin
                )
            } else {
                authRepository.login(
                    serverUrl = state.serverUrl.trim(),
                    username = state.username,
                    password = state.password,
                    autoLogin = state.autoLogin
                )
            }

            result
                .onSuccess { authenticated ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        authState = authenticated
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = e.message ?: "Connection failed"
                    )
                }
        }
    }

    fun logout() {
        authRepository.logout()
        val saved = tokenManager.getSavedCredentials()
        _uiState.value = AuthUiState(
            isLoading = false,
            authState = AuthState.Unauthenticated,
            serverUrl = saved?.serverUrl ?: "",
            username = saved?.username ?: "",
            password = "", // don't pre-fill password after logout
            autoLogin = false
        )
    }

    fun fullLogout() {
        authRepository.fullLogout()
        _uiState.value = AuthUiState(
            isLoading = false,
            authState = AuthState.Unauthenticated
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
