package com.geny.app.presentation.vtuber

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.VTuberRepository
import com.geny.app.domain.model.AvatarState
import com.geny.app.domain.model.VTuberModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VTuberUiState(
    val avatarState: AvatarState? = null,
    val models: List<VTuberModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedEmotion: String = "neutral",
    val isConnected: Boolean = false
)

@HiltViewModel
class VTuberViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vtuberRepository: VTuberRepository
) : ViewModel() {

    private val agentId: String = savedStateHandle.get<String>("agentId") ?: ""

    private val _uiState = MutableStateFlow(VTuberUiState())
    val uiState: StateFlow<VTuberUiState> = _uiState.asStateFlow()

    private var sseJob: Job? = null

    init {
        loadInitialState()
        subscribeToAvatarEvents()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load models and avatar state in parallel
            val modelsResult = vtuberRepository.listModels()
            val stateResult = vtuberRepository.getAvatarState(agentId)

            modelsResult.onSuccess { models ->
                _uiState.value = _uiState.value.copy(models = models)
            }
            stateResult.onSuccess { state ->
                _uiState.value = _uiState.value.copy(avatarState = state)
            }
            stateResult.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun subscribeToAvatarEvents() {
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            vtuberRepository.streamAvatarEvents(agentId)
                .catch { /* auto-retry handled by SseEventSource */ }
                .collect { state ->
                    _uiState.value = _uiState.value.copy(
                        avatarState = state,
                        isConnected = true
                    )
                }
        }
    }

    fun interact(hitArea: String, x: Float? = null, y: Float? = null) {
        viewModelScope.launch {
            vtuberRepository.interact(agentId, hitArea, x, y)
        }
    }

    fun setEmotion(emotion: String, intensity: Float? = 1.0f) {
        _uiState.value = _uiState.value.copy(selectedEmotion = emotion)
        viewModelScope.launch {
            vtuberRepository.setEmotion(agentId, emotion, intensity)
        }
    }

    fun assignModel(modelName: String) {
        viewModelScope.launch {
            vtuberRepository.assignModel(agentId, modelName)
                .onSuccess { loadInitialState() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }
}
