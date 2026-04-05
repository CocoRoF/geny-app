package com.geny.app.domain.model

enum class AgentStatus {
    STARTING, RUNNING, IDLE, STOPPED, ERROR;

    companion object {
        fun fromString(value: String): AgentStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: IDLE
        }
    }
}

enum class AgentRole {
    WORKER, DEVELOPER, RESEARCHER, PLANNER, VTUBER;

    companion object {
        fun fromString(value: String): AgentRole {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: WORKER
        }
    }
}

data class Agent(
    val sessionId: String,
    val sessionName: String,
    val status: AgentStatus,
    val role: AgentRole,
    val model: String?,
    val systemPrompt: String?,
    val workingDir: String?,
    val maxTurns: Int?,
    val timeout: Float?,
    val maxIterations: Int?,
    val createdAt: String?,
    val totalCost: Double?,
    val storagePath: String?,
    val errorMessage: String?,
    val chatRoomId: String?,
    val workflowId: String?,
    val graphName: String?,
    val linkedSessionId: String?,
    val sessionType: String?,
    val isDeleted: Boolean
) {
    /** CLI session bound to a VTuber — should be hidden from main list */
    val isLinkedCli: Boolean
        get() = sessionType == "cli" && linkedSessionId != null

    val isVTuber: Boolean
        get() = role == AgentRole.VTUBER || sessionType == "vtuber"
}

data class ExecutionResult(
    val success: Boolean,
    val output: String?,
    val error: String?,
    val costUsd: Double?,
    val durationMs: Long?,
    val numTurns: Int?,
    val isTaskComplete: Boolean?
)

data class StorageFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long?,
    val modifiedAt: String?
)
