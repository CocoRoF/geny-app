package com.geny.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.core.storage.TokenManager
import com.geny.app.data.repository.AuthRepository
import com.geny.app.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val themeMode: String = "system",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val username: String? = null,
    val autoLoginEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val healthRepository: HealthRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    serverUrl = settings.serverUrl,
                    themeMode = settings.themeMode,
                    autoLoginEnabled = tokenManager.isAutoLoginEnabled(),
                    username = tokenManager.getSavedCredentials()?.username
                )
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            healthRepository.checkHealth()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        testResult = "Connected successfully!"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        testResult = "Connection failed: ${e.message}"
                    )
                }
        }
    }

    /**
     * Logout but keep saved credentials (can re-login).
     */
    fun logout() {
        authRepository.logout()
    }

    /**
     * Full logout - clear everything including saved credentials.
     */
    fun fullLogout() {
        authRepository.fullLogout()
    }
}
