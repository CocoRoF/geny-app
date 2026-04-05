package com.geny.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.AgentRepository
import com.geny.app.domain.model.Agent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val agents: List<Agent> = emptyList(),       // visible agents (CLI filtered out)
    val allAgents: List<Agent> = emptyList(),     // all agents including CLI
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newAgentName: String = "",
    val newAgentModel: String = "",
    val newAgentRole: String = "WORKER",
    val isCreating: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadAgents()
    }

    fun loadAgents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            agentRepository.listAgents()
                .onSuccess { agents ->
                    val active = agents.filter { !it.isDeleted }
                    _uiState.value = _uiState.value.copy(
                        allAgents = active,
                        agents = active.filter { !it.isLinkedCli },
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load agents"
                    )
                }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadAgents()
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            newAgentName = "",
            newAgentModel = "",
            newAgentRole = "WORKER"
        )
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun onNewAgentNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newAgentName = name)
    }

    fun onNewAgentModelChanged(model: String) {
        _uiState.value = _uiState.value.copy(newAgentModel = model)
    }

    fun onNewAgentRoleChanged(role: String) {
        _uiState.value = _uiState.value.copy(newAgentRole = role)
    }

    fun createAgent() {
        val state = _uiState.value
        if (state.newAgentName.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            agentRepository.createAgent(
                name = state.newAgentName,
                model = state.newAgentModel.takeIf { it.isNotBlank() },
                role = state.newAgentRole
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        showCreateDialog = false,
                        isCreating = false
                    )
                    loadAgents()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = e.message ?: "Failed to create agent"
                    )
                }
        }
    }

    fun deleteAgent(id: String) {
        viewModelScope.launch {
            agentRepository.deleteAgent(id)
                .onSuccess { loadAgents() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to delete agent"
                    )
                }
        }
    }
}
