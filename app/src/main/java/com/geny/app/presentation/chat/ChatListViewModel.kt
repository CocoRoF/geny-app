package com.geny.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.AgentRepository
import com.geny.app.data.repository.ChatRepository
import com.geny.app.domain.model.Agent
import com.geny.app.domain.model.ChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newRoomName: String = "",
    val selectedAgentIds: Set<String> = emptySet(),
    val isCreating: Boolean = false
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            chatRepository.listRooms()
                .onSuccess { rooms ->
                    _uiState.value = _uiState.value.copy(
                        rooms = rooms,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message
                    )
                }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadRooms()
    }

    fun showCreateDialog() {
        viewModelScope.launch {
            agentRepository.listAgents().onSuccess { agents ->
                _uiState.value = _uiState.value.copy(
                    showCreateDialog = true,
                    agents = agents.filter { !it.isDeleted },
                    newRoomName = "",
                    selectedAgentIds = emptySet()
                )
            }
        }
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun onRoomNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newRoomName = name)
    }

    fun toggleAgentSelection(agentId: String) {
        val current = _uiState.value.selectedAgentIds.toMutableSet()
        if (current.contains(agentId)) current.remove(agentId) else current.add(agentId)
        _uiState.value = _uiState.value.copy(selectedAgentIds = current)
    }

    fun createRoom() {
        val state = _uiState.value
        if (state.newRoomName.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            chatRepository.createRoom(state.newRoomName, state.selectedAgentIds.toList())
                .onSuccess {
                    _uiState.value = _uiState.value.copy(showCreateDialog = false, isCreating = false)
                    loadRooms()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = e.message
                    )
                }
        }
    }

    fun deleteRoom(id: String) {
        viewModelScope.launch {
            chatRepository.deleteRoom(id).onSuccess { loadRooms() }
        }
    }
}
