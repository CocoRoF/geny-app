package com.geny.app.presentation.vtuber

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.AgentRepository
import com.geny.app.data.repository.ChatRepository
import com.geny.app.data.repository.VTuberRepository
import com.geny.app.domain.model.AvatarState
import com.geny.app.domain.model.ChatMessage
import com.geny.app.domain.model.ChatSseEvent
import com.geny.app.domain.model.MessageType
import com.geny.app.domain.model.VTuberModel
import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.presentation.tts.TtsPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "VTuberViewerVM"

data class BroadcastProgress(
    val broadcastId: String,
    val completed: Int,
    val total: Int
)

data class VTuberUiState(
    val avatarState: AvatarState? = null,
    val models: List<VTuberModel> = emptyList(),
    val currentModel: VTuberModel? = null,
    val agentName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isConnected: Boolean = false,
    val webViewReady: Boolean = false,
    val modelLoaded: Boolean = false,
    // Chat
    val chatMessage: String = "",
    val isSending: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val chatRoomId: String? = null,
    val broadcastProgress: BroadcastProgress? = null,
    val isThinking: Boolean = false,
    val thinkingPreview: String? = null,
    // TTS
    val ttsEnabled: Boolean = false,
    // Chat overlay visibility
    val chatOverlayVisible: Boolean = true
)

@HiltViewModel
class VTuberViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vtuberRepository: VTuberRepository,
    private val agentRepository: AgentRepository,
    private val chatRepository: ChatRepository,
    private val ttsPlaybackManager: TtsPlaybackManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val agentId: String = savedStateHandle.get<String>("agentId") ?: ""

    private val _uiState = MutableStateFlow(VTuberUiState(ttsEnabled = settingsDataStore.ttsEnabled))
    val uiState: StateFlow<VTuberUiState> = _uiState.asStateFlow()

    val serverUrl: String get() = vtuberRepository.getServerUrl()

    private var avatarSseJob: Job? = null
    private var chatSseJob: Job? = null
    private var thinkingTimeoutJob: Job? = null
    private var pollingJob: Job? = null

    // Track message IDs to prevent duplicates
    private val knownMessageIds = mutableSetOf<String>()

    init {
        loadInitialData()
        subscribeToAvatarEvents()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load agent info (name + chatRoomId)
            agentRepository.getAgent(agentId).onSuccess { agent ->
                _uiState.value = _uiState.value.copy(
                    agentName = agent.sessionName,
                    chatRoomId = agent.chatRoomId
                )
                agent.chatRoomId?.let { roomId ->
                    loadChatMessages(roomId)
                    subscribeToChatEvents(roomId)
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to load agent: ${e.message}")
            }

            // Load models
            vtuberRepository.listModels().onSuccess { models ->
                _uiState.value = _uiState.value.copy(models = models)
            }

            // Load assigned model
            vtuberRepository.getAssignedModel(agentId).onSuccess { model ->
                _uiState.value = _uiState.value.copy(currentModel = model)
            }.onFailure {
                val models = _uiState.value.models
                if (models.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(currentModel = models.first())
                }
            }

            // Load avatar state
            vtuberRepository.getAvatarState(agentId).onSuccess { state ->
                _uiState.value = _uiState.value.copy(avatarState = state)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun loadChatMessages(roomId: String) {
        chatRepository.getMessages(roomId).onSuccess { messages ->
            messages.forEach { knownMessageIds.add(it.id) }
            _uiState.value = _uiState.value.copy(messages = messages)
            Log.d(TAG, "Loaded ${messages.size} chat messages")
        }.onFailure { e ->
            Log.e(TAG, "Failed to load messages: ${e.message}")
        }
    }

    // ========================================================================
    // SSE subscription with auto-restart
    // ========================================================================

    private fun subscribeToChatEvents(roomId: String) {
        chatSseJob?.cancel()
        Log.i(TAG, ">>> SSE: subscribing to chat events for room=$roomId")
        chatSseJob = viewModelScope.launch {
            while (true) {
                try {
                    Log.d(TAG, ">>> SSE: starting streamRoomEvents...")
                    chatRepository.streamRoomEvents(roomId)
                        .collect { event ->
                            Log.d(TAG, ">>> SSE: received event: ${event::class.simpleName}")
                            handleChatSseEvent(event)
                        }
                    Log.w(TAG, ">>> SSE: flow completed normally (unexpected)")
                } catch (e: Exception) {
                    Log.w(TAG, ">>> SSE: subscription error: ${e.javaClass.simpleName}: ${e.message}, restarting in 5s...")
                    delay(5000)
                }
                Log.d(TAG, ">>> SSE: restarting chat subscription...")
            }
        }
    }

    private fun handleChatSseEvent(event: ChatSseEvent) {
        when (event) {
            is ChatSseEvent.NewMessage -> {
                val msg = event.message
                Log.d(TAG, "SSE NewMessage: id=${msg.id}, type=${msg.type}, content=${msg.content.take(50)}")

                // Deduplicate
                if (msg.id in knownMessageIds) {
                    // Update the optimistic placeholder with real data
                    val existing = _uiState.value.messages.find { it.id == msg.id }
                    if (existing != null && existing.timestamp == null && msg.timestamp != null) {
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages.map {
                                if (it.id == msg.id) msg else it
                            }
                        )
                    }
                    return
                }

                knownMessageIds.add(msg.id)

                // Remove optimistic placeholder for user messages
                val filtered = _uiState.value.messages.filter { existing ->
                    !(existing.id.startsWith("temp_") &&
                      existing.type == MessageType.USER &&
                      existing.content == msg.content)
                }

                _uiState.value = _uiState.value.copy(
                    messages = filtered + msg,
                    isThinking = if (msg.type == MessageType.AGENT) false else _uiState.value.isThinking,
                    thinkingPreview = if (msg.type == MessageType.AGENT) null else _uiState.value.thinkingPreview,
                    broadcastProgress = if (msg.type == MessageType.AGENT) null else _uiState.value.broadcastProgress
                )

                // Cancel thinking timeout since we got a response
                if (msg.type == MessageType.AGENT) {
                    thinkingTimeoutJob?.cancel()
                    pollingJob?.cancel()
                }

                // TTS for agent messages
                Log.d(TAG, "TTS check: ttsEnabled=${_uiState.value.ttsEnabled}, msgType=${msg.type}, agentId=$agentId")
                if (_uiState.value.ttsEnabled && msg.type == MessageType.AGENT) {
                    Log.i(TAG, ">>> TTS TRIGGERED: launching speak for agent=$agentId, contentLen=${msg.content.length}, emotion=${_uiState.value.avatarState?.emotion}")
                    viewModelScope.launch {
                        try {
                            ttsPlaybackManager.speak(
                                agentId,
                                msg.content,
                                _uiState.value.avatarState?.emotion
                            )
                            Log.d(TAG, ">>> TTS speak() returned successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, ">>> TTS EXCEPTION: ${e.javaClass.simpleName}: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d(TAG, "TTS skipped: enabled=${_uiState.value.ttsEnabled}, type=${msg.type}")
                }
            }
            is ChatSseEvent.BroadcastStatus -> {
                Log.d(TAG, "SSE BroadcastStatus: ${event.completed}/${event.total}")
                _uiState.value = _uiState.value.copy(
                    broadcastProgress = BroadcastProgress(
                        broadcastId = event.broadcastId,
                        completed = event.completed,
                        total = event.total
                    ),
                    isThinking = event.completed < event.total
                )
            }
            is ChatSseEvent.AgentProgress -> {
                Log.d(TAG, "SSE AgentProgress: status=${event.status}, preview=${event.thinkingPreview?.take(30)}")
                if (event.status == "executing") {
                    _uiState.value = _uiState.value.copy(
                        isThinking = true,
                        thinkingPreview = event.thinkingPreview
                    )
                }
            }
            is ChatSseEvent.BroadcastDone -> {
                Log.d(TAG, "SSE BroadcastDone: ${event.broadcastId}")
                _uiState.value = _uiState.value.copy(
                    broadcastProgress = null,
                    isThinking = false,
                    thinkingPreview = null
                )
                thinkingTimeoutJob?.cancel()
                pollingJob?.cancel()
            }
            is ChatSseEvent.Heartbeat -> {
                _uiState.value = _uiState.value.copy(isConnected = true)
            }
        }
    }

    private fun subscribeToAvatarEvents() {
        avatarSseJob?.cancel()
        avatarSseJob = viewModelScope.launch {
            while (true) {
                try {
                    vtuberRepository.streamAvatarEvents(agentId)
                        .collect { state ->
                            _uiState.value = _uiState.value.copy(
                                avatarState = state,
                                isConnected = true
                            )
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Avatar SSE ended: ${e.message}, restarting in 5s...")
                    delay(5000)
                }
            }
        }
    }

    // ========================================================================
    // Chat send with polling fallback
    // ========================================================================

    fun sendChat() {
        val message = _uiState.value.chatMessage.trim()
        if (message.isBlank()) return

        // Optimistically add user message
        val tempId = "temp_${UUID.randomUUID()}"
        val optimisticMessage = ChatMessage(
            id = tempId,
            type = MessageType.USER,
            content = message,
            timestamp = null,
            sessionId = null,
            sessionName = null,
            role = "user",
            durationMs = null,
            fileChanges = null
        )

        _uiState.value = _uiState.value.copy(
            isSending = true,
            chatMessage = "",
            messages = _uiState.value.messages + optimisticMessage,
            isThinking = true
        )

        // Start thinking timeout — auto-clear after 90 seconds
        thinkingTimeoutJob?.cancel()
        thinkingTimeoutJob = viewModelScope.launch {
            delay(90_000)
            Log.w(TAG, "Thinking timeout reached, clearing thinking state")
            _uiState.value = _uiState.value.copy(
                isThinking = false,
                thinkingPreview = null,
                broadcastProgress = null
            )
        }

        viewModelScope.launch {
            val roomId = _uiState.value.chatRoomId
            if (roomId != null) {
                chatRepository.broadcast(roomId, message)
                    .onSuccess {
                        Log.d(TAG, "Broadcast sent successfully")
                        // Start polling fallback in case SSE misses the response
                        startPollingFallback(roomId)
                    }
                    .onFailure { e ->
                        Log.w(TAG, "Broadcast failed: ${e.message}, trying direct execution")
                        agentRepository.startExecution(agentId, message)
                            .onSuccess {
                                startPollingFallback(roomId)
                            }
                            .onFailure { e2 ->
                                Log.e(TAG, "Direct execution also failed: ${e2.message}")
                                _uiState.value = _uiState.value.copy(isThinking = false)
                                thinkingTimeoutJob?.cancel()
                            }
                    }
            } else {
                agentRepository.startExecution(agentId, message)
                    .onFailure { e ->
                        Log.e(TAG, "Execution failed: ${e.message}")
                        _uiState.value = _uiState.value.copy(isThinking = false)
                        thinkingTimeoutJob?.cancel()
                    }
            }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    /**
     * Poll for new messages as a fallback in case SSE doesn't deliver them.
     * Polls every 3 seconds for up to 60 seconds, or until thinking is cleared.
     */
    private fun startPollingFallback(roomId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Initial delay — give SSE a chance first
            delay(3000)

            repeat(20) { i ->
                if (!_uiState.value.isThinking) return@launch // SSE delivered it

                Log.d(TAG, "Polling for messages (attempt ${i + 1})...")
                chatRepository.getMessages(roomId).onSuccess { allMessages ->
                    var hasNew = false
                    for (msg in allMessages) {
                        if (msg.id !in knownMessageIds) {
                            knownMessageIds.add(msg.id)
                            hasNew = true
                        }
                    }

                    if (hasNew) {
                        Log.d(TAG, "Polling found new messages, updating UI")
                        val realMessages = allMessages.filter { it.id in knownMessageIds }

                        // Capture current message IDs BEFORE updating state
                        val previousIds = _uiState.value.messages.map { m -> m.id }.toSet()
                        val newAgentMsgs = realMessages.filter {
                            it.type == MessageType.AGENT && it.id !in previousIds
                        }
                        val hasAgentResponse = newAgentMsgs.isNotEmpty()

                        Log.d(TAG, "Polling: ${realMessages.size} total, ${newAgentMsgs.size} new agent msgs, hasAgent=$hasAgentResponse")

                        _uiState.value = _uiState.value.copy(
                            messages = realMessages,
                            isThinking = if (hasAgentResponse) false else _uiState.value.isThinking,
                            thinkingPreview = if (hasAgentResponse) null else _uiState.value.thinkingPreview,
                            broadcastProgress = if (hasAgentResponse) null else _uiState.value.broadcastProgress
                        )

                        // TTS for new agent messages (using pre-computed list, not post-update state)
                        if (_uiState.value.ttsEnabled && hasAgentResponse) {
                            Log.i(TAG, ">>> POLLING TTS: ${newAgentMsgs.size} new agent messages to speak")
                            for (msg in newAgentMsgs) {
                                viewModelScope.launch {
                                    try {
                                        ttsPlaybackManager.speak(agentId, msg.content, _uiState.value.avatarState?.emotion)
                                    } catch (e: Exception) {
                                        Log.e(TAG, ">>> POLLING TTS EXCEPTION: ${e.message}", e)
                                    }
                                }
                            }
                        }

                        if (hasAgentResponse) {
                            thinkingTimeoutJob?.cancel()
                            return@launch
                        }
                    }
                }

                delay(3000)
            }
        }
    }

    // ========================================================================
    // UI
    // ========================================================================

    fun onWebViewReady() {
        _uiState.value = _uiState.value.copy(webViewReady = true)
    }

    fun onModelLoaded() {
        _uiState.value = _uiState.value.copy(modelLoaded = true, error = null)
    }

    fun onWebViewError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun interact(hitArea: String, x: Float? = null, y: Float? = null) {
        viewModelScope.launch {
            vtuberRepository.interact(agentId, hitArea, x, y)
        }
    }

    fun onChatMessageChanged(message: String) {
        _uiState.value = _uiState.value.copy(chatMessage = message)
    }

    fun toggleTts() {
        val newState = !_uiState.value.ttsEnabled
        _uiState.value = _uiState.value.copy(ttsEnabled = newState)
        if (!newState) ttsPlaybackManager.stop()
        viewModelScope.launch { settingsDataStore.setTtsEnabled(newState) }
    }

    fun toggleChatOverlay() {
        _uiState.value = _uiState.value.copy(
            chatOverlayVisible = !_uiState.value.chatOverlayVisible
        )
    }

    fun assignModel(modelName: String) {
        viewModelScope.launch {
            vtuberRepository.assignModel(agentId, modelName)
                .onSuccess {
                    val model = _uiState.value.models.find { it.name == modelName }
                    if (model != null) {
                        _uiState.value = _uiState.value.copy(
                            currentModel = model,
                            modelLoaded = false
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        avatarSseJob?.cancel()
        chatSseJob?.cancel()
        thinkingTimeoutJob?.cancel()
        pollingJob?.cancel()
    }
}
