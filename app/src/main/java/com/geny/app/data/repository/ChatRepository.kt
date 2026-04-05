package com.geny.app.data.repository

import android.util.Log
import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.core.storage.TokenManager
import com.geny.app.data.api.ChatApi
import com.geny.app.data.dto.BroadcastRequest
import com.geny.app.data.dto.ChatMessageDto
import com.geny.app.data.dto.CreateRoomRequest
import com.geny.app.data.sse.SseEventSource
import com.geny.app.domain.model.ChatMessage
import com.geny.app.domain.model.ChatRoom
import com.geny.app.domain.model.ChatSseEvent
import com.geny.app.domain.model.MessageType
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val sseEventSource: SseEventSource,
    private val tokenManager: TokenManager,
    private val settingsDataStore: SettingsDataStore
) {
    private val gson = Gson()

    suspend fun listRooms(): Result<List<ChatRoom>> = runCatching {
        chatApi.listRooms().rooms.map { dto ->
            ChatRoom(
                id = dto.id,
                name = dto.name,
                sessionIds = dto.sessionIds,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                messageCount = dto.messageCount ?: 0
            )
        }
    }

    suspend fun createRoom(name: String, sessionIds: List<String>): Result<ChatRoom> = runCatching {
        val dto = chatApi.createRoom(CreateRoomRequest(name, sessionIds))
        ChatRoom(
            id = dto.id,
            name = dto.name,
            sessionIds = dto.sessionIds,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            messageCount = dto.messageCount ?: 0
        )
    }

    suspend fun deleteRoom(id: String): Result<Unit> = runCatching {
        chatApi.deleteRoom(id)
    }

    suspend fun getMessages(roomId: String): Result<List<ChatMessage>> = runCatching {
        chatApi.getMessages(roomId).messages.map { it.toDomain() }
    }

    suspend fun broadcast(roomId: String, message: String): Result<String> = runCatching {
        val response = chatApi.broadcast(roomId, BroadcastRequest(message))
        response.broadcastId ?: ""
    }

    fun streamRoomEvents(roomId: String): Flow<ChatSseEvent> {
        val url = "${settingsDataStore.serverUrl}/api/chat/rooms/$roomId/events"
        return sseEventSource.streamSse(url, tokenManager.getToken())
            .mapNotNull { event -> parseChatEvent(event.event, event.data) }
    }

    private fun parseChatEvent(eventType: String, data: String): ChatSseEvent? {
        return try {
            when (eventType) {
                "message" -> {
                    val msg = gson.fromJson(data, ChatMessageDto::class.java)
                    Log.d(TAG, "SSE parsed message: type=${msg.type}, id=${msg.id}, content=${msg.content.take(40)}")
                    ChatSseEvent.NewMessage(msg.toDomain())
                }
                "broadcast_status" -> {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    ChatSseEvent.BroadcastStatus(
                        broadcastId = json.get("broadcast_id")?.asString ?: "",
                        completed = json.get("completed")?.asInt ?: 0,
                        total = json.get("total")?.asInt ?: 0,
                        agentStates = emptyMap()
                    )
                }
                "agent_progress" -> {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    // Backend may send as array or object — handle both
                    val agentState = if (json.isJsonArray) {
                        json.asJsonArray.firstOrNull()?.asJsonObject
                    } else {
                        json
                    }
                    ChatSseEvent.AgentProgress(
                        sessionId = agentState?.get("session_id")?.asString ?: "",
                        sessionName = agentState?.get("session_name")?.asString,
                        status = agentState?.get("status")?.asString ?: "",
                        thinkingPreview = agentState?.get("thinking_preview")?.asString
                    )
                }
                "broadcast_done" -> {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    ChatSseEvent.BroadcastDone(
                        broadcastId = json.get("broadcast_id")?.asString ?: ""
                    )
                }
                "heartbeat" -> ChatSseEvent.Heartbeat
                else -> {
                    Log.d(TAG, "SSE unknown event type: $eventType, data: ${data.take(100)}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSE parse error for event '$eventType': ${e.message}, data: ${data.take(200)}")
            null
        }
    }

    companion object {
        private const val TAG = "ChatRepository"
    }

    private fun ChatMessageDto.toDomain(): ChatMessage = ChatMessage(
        id = id,
        type = MessageType.fromString(type),
        content = content,
        timestamp = timestamp,
        sessionId = sessionId,
        sessionName = sessionName,
        role = role,
        durationMs = durationMs,
        fileChanges = fileChanges
    )
}
