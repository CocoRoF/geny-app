package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

data class ChatRoomDto(
    val id: String,
    val name: String,
    @SerializedName("session_ids") val sessionIds: List<String> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("message_count") val messageCount: Int? = 0
)

data class CreateRoomRequest(
    val name: String,
    @SerializedName("session_ids") val sessionIds: List<String> = emptyList()
)

data class UpdateRoomRequest(
    val name: String? = null,
    @SerializedName("session_ids") val sessionIds: List<String>? = null
)

data class ChatMessageDto(
    val id: String,
    val type: String, // "user", "agent", "system"
    val content: String,
    val timestamp: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("session_name") val sessionName: String? = null,
    val role: String? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    @SerializedName("file_changes") val fileChanges: List<String>? = null,
    val meta: Map<String, Any>? = null
)

data class BroadcastRequest(
    val message: String
)

data class BroadcastResponse(
    val success: Boolean,
    @SerializedName("broadcast_id") val broadcastId: String? = null,
    @SerializedName("message_id") val messageId: String? = null,
    @SerializedName("session_count") val sessionCount: Int? = null
)

data class MessageListResponse(
    val messages: List<ChatMessageDto>
)

data class RoomListResponse(
    val rooms: List<ChatRoomDto>,
    val total: Int? = null
)
