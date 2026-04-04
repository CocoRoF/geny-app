package com.geny.app.domain.model

data class ChatRoom(
    val id: String,
    val name: String,
    val sessionIds: List<String>,
    val createdAt: String?,
    val updatedAt: String?,
    val messageCount: Int
)

data class ChatMessage(
    val id: String,
    val type: MessageType,
    val content: String,
    val timestamp: String?,
    val sessionId: String?,
    val sessionName: String?,
    val role: String?,
    val durationMs: Long?,
    val fileChanges: List<String>?
)

enum class MessageType {
    USER, AGENT, SYSTEM;

    companion object {
        fun fromString(value: String): MessageType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

sealed class ChatSseEvent {
    data class NewMessage(val message: ChatMessage) : ChatSseEvent()
    data class BroadcastStatus(
        val broadcastId: String,
        val completed: Int,
        val total: Int,
        val agentStates: Map<String, String>
    ) : ChatSseEvent()
    data class AgentProgress(
        val sessionId: String,
        val sessionName: String?,
        val status: String,
        val thinkingPreview: String?
    ) : ChatSseEvent()
    data class BroadcastDone(val broadcastId: String) : ChatSseEvent()
    data object Heartbeat : ChatSseEvent()
}
