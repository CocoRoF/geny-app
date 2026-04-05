package com.geny.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.data.repository.AgentRepository
import com.geny.app.domain.model.Agent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val vtuberAgents: List<Agent> = emptyList(),
    val selectedAgentId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAgents()
    }

    fun loadAgents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            agentRepository.listAgents()
                .onSuccess { agents ->
                    val vtuberAgents = agents.filter { it.isVTuber && !it.isLinkedCli }
                    val lastId = settingsDataStore.lastVTuberAgentId
                    val selectedId = when {
                        vtuberAgents.isEmpty() -> null
                        vtuberAgents.any { it.sessionId == lastId } -> lastId
                        else -> vtuberAgents.first().sessionId
                    }
                    _uiState.value = _uiState.value.copy(
                        vtuberAgents = vtuberAgents,
                        selectedAgentId = selectedId,
                        isLoading = false
                    )
                    if (selectedId != null && selectedId != lastId) {
                        settingsDataStore.setLastVTuberAgentId(selectedId)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load agents"
                    )
                }
        }
    }

    fun selectAgent(agentId: String) {
        _uiState.value = _uiState.value.copy(selectedAgentId = agentId)
        viewModelScope.launch {
            settingsDataStore.setLastVTuberAgentId(agentId)
        }
    }
}
