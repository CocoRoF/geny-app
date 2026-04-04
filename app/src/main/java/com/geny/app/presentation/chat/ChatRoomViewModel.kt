package com.geny.app.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.ChatRepository
import com.geny.app.domain.model.ChatMessage
import com.geny.app.domain.model.ChatSseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BroadcastProgress(
    val broadcastId: String,
    val completed: Int,
    val total: Int,
    val agentStates: Map<String, AgentProgressInfo> = emptyMap()
)

data class AgentProgressInfo(
    val sessionName: String?,
    val status: String,
    val thinkingPreview: String?
)

data class ChatRoomUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val inputMessage: String = "",
    val isSending: Boolean = false,
    val broadcastProgress: BroadcastProgress? = null,
    val isConnected: Boolean = false
)

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val roomId: String = savedStateHandle.get<String>("roomId") ?: ""

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    private var sseJob: Job? = null

    init {
        loadMessages()
        subscribeToEvents()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            chatRepository.getMessages(roomId)
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    private fun subscribeToEvents() {
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            chatRepository.streamRoomEvents(roomId)
                .catch { /* reconnection handled by SseEventSource */ }
                .collect { event ->
                    when (event) {
                        is ChatSseEvent.NewMessage -> {
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + event.message
                            )
                        }
                        is ChatSseEvent.BroadcastStatus -> {
                            val current = _uiState.value.broadcastProgress
                            _uiState.value = _uiState.value.copy(
                                broadcastProgress = BroadcastProgress(
                                    broadcastId = event.broadcastId,
                                    completed = event.completed,
                                    total = event.total,
                                    agentStates = current?.agentStates ?: emptyMap()
                                )
                            )
                        }
                        is ChatSseEvent.AgentProgress -> {
                            val current = _uiState.value.broadcastProgress ?: return@collect
                            val updated = current.agentStates.toMutableMap()
                            updated[event.sessionId] = AgentProgressInfo(
                                sessionName = event.sessionName,
                                status = event.status,
                                thinkingPreview = event.thinkingPreview
                            )
                            _uiState.value = _uiState.value.copy(
                                broadcastProgress = current.copy(agentStates = updated)
                            )
                        }
                        is ChatSseEvent.BroadcastDone -> {
                            _uiState.value = _uiState.value.copy(
                                broadcastProgress = null
                            )
                        }
                        is ChatSseEvent.Heartbeat -> {
                            _uiState.value = _uiState.value.copy(isConnected = true)
                        }
                    }
                }
        }
    }

    fun onInputChanged(message: String) {
        _uiState.value = _uiState.value.copy(inputMessage = message)
    }

    fun sendBroadcast() {
        val message = _uiState.value.inputMessage.trim()
        if (message.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            chatRepository.broadcast(roomId, message)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        inputMessage = "",
                        isSending = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = e.message
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }
}
