package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

data class CreateAgentRequest(
    @SerializedName("session_name") val sessionName: String,
    @SerializedName("working_dir") val workingDir: String? = null,
    val model: String? = null,
    @SerializedName("system_prompt") val systemPrompt: String? = null,
    val role: String? = null,
    @SerializedName("max_turns") val maxTurns: Int? = null,
    val timeout: Float? = null,
    @SerializedName("max_iterations") val maxIterations: Int? = null
)

data class SessionInfoDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("session_name") val sessionName: String,
    val status: String,
    val role: String? = null,
    val model: String? = null,
    @SerializedName("system_prompt") val systemPrompt: String? = null,
    @SerializedName("working_dir") val workingDir: String? = null,
    @SerializedName("max_turns") val maxTurns: Int? = null,
    val timeout: Float? = null,
    @SerializedName("max_iterations") val maxIterations: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("total_cost") val totalCost: Double? = null,
    @SerializedName("storage_path") val storagePath: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("chat_room_id") val chatRoomId: String? = null,
    @SerializedName("workflow_id") val workflowId: String? = null,
    @SerializedName("graph_name") val graphName: String? = null,
    @SerializedName("is_deleted") val isDeleted: Boolean? = false
)

data class ExecuteRequestDto(
    val prompt: String,
    val timeout: Float? = null,
    @SerializedName("system_prompt") val systemPrompt: String? = null,
    @SerializedName("max_turns") val maxTurns: Int? = null
)

data class ExecuteResponseDto(
    val success: Boolean,
    @SerializedName("session_id") val sessionId: String? = null,
    val output: String? = null,
    val error: String? = null,
    @SerializedName("cost_usd") val costUsd: Double? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    @SerializedName("num_turns") val numTurns: Int? = null,
    val model: String? = null,
    @SerializedName("is_task_complete") val isTaskComplete: Boolean? = null
)

data class ExecutionStatusDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("is_executing") val isExecuting: Boolean,
    @SerializedName("has_result") val hasResult: Boolean? = null
)

data class StorageFileDto(
    val name: String,
    val path: String,
    val type: String, // "file" or "directory"
    val size: Long? = null,
    @SerializedName("modified_at") val modifiedAt: String? = null
)

data class StorageListResponse(
    val files: List<StorageFileDto>
)

data class StorageFileContentDto(
    val content: String,
    val path: String,
    val encoding: String? = null
)
