package com.geny.app.data.repository

import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.core.storage.TokenManager
import com.geny.app.data.api.AgentApi
import com.geny.app.data.dto.CreateAgentRequest
import com.geny.app.data.dto.ExecuteRequestDto
import com.geny.app.data.dto.SessionInfoDto
import com.geny.app.data.sse.SseEventSource
import com.geny.app.domain.model.Agent
import com.geny.app.domain.model.AgentRole
import com.geny.app.domain.model.AgentStatus
import com.geny.app.domain.model.ExecutionEvent
import com.geny.app.domain.model.ExecutionResult
import com.geny.app.domain.model.StorageFile
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val agentApi: AgentApi,
    private val sseEventSource: SseEventSource,
    private val tokenManager: TokenManager,
    private val settingsDataStore: SettingsDataStore
) {
    private val gson = Gson()

    suspend fun listAgents(): Result<List<Agent>> = runCatching {
        agentApi.listAgents().map { it.toDomain() }
    }

    suspend fun getAgent(id: String): Result<Agent> = runCatching {
        agentApi.getAgent(id).toDomain()
    }

    suspend fun createAgent(
        name: String,
        workingDir: String? = null,
        model: String? = null,
        systemPrompt: String? = null,
        role: String? = null,
        maxIterations: Int? = null
    ): Result<Agent> = runCatching {
        agentApi.createAgent(
            CreateAgentRequest(
                sessionName = name,
                workingDir = workingDir,
                model = model,
                systemPrompt = systemPrompt,
                role = role,
                maxIterations = maxIterations
            )
        ).toDomain()
    }

    suspend fun deleteAgent(id: String): Result<Unit> = runCatching {
        agentApi.deleteAgent(id)
    }

    suspend fun restoreAgent(id: String): Result<Unit> = runCatching {
        agentApi.restoreAgent(id)
    }

    suspend fun startExecution(
        id: String,
        prompt: String,
        timeout: Float? = null
    ): Result<ExecutionResult> = runCatching {
        val response = agentApi.startExecution(
            id,
            ExecuteRequestDto(prompt = prompt, timeout = timeout)
        )
        ExecutionResult(
            success = response.success,
            output = response.output,
            error = response.error,
            costUsd = response.costUsd,
            durationMs = response.durationMs,
            numTurns = response.numTurns,
            isTaskComplete = response.isTaskComplete
        )
    }

    suspend fun stopExecution(id: String): Result<Unit> = runCatching {
        agentApi.stopExecution(id)
    }

    fun streamExecutionEvents(agentId: String): Flow<ExecutionEvent> {
        val url = "${settingsDataStore.serverUrl}/api/agents/$agentId/execute/events"
        return sseEventSource.streamSse(url, tokenManager.getToken())
            .mapNotNull { event -> parseExecutionEvent(event.event, event.data) }
    }

    suspend fun listStorage(id: String): Result<List<StorageFile>> = runCatching {
        agentApi.listStorage(id).files.map { file ->
            StorageFile(
                name = file.name,
                path = file.path,
                isDirectory = file.type == "directory",
                size = file.size,
                modifiedAt = file.modifiedAt
            )
        }
    }

    suspend fun readFile(id: String, path: String): Result<String> = runCatching {
        agentApi.readFile(id, path).content
    }

    private fun parseExecutionEvent(eventType: String, data: String): ExecutionEvent? {
        return try {
            val json = gson.fromJson(data, JsonObject::class.java)
            when (eventType) {
                "log" -> ExecutionEvent.Log(
                    message = json.get("message")?.asString ?: data,
                    level = json.get("level")?.asString,
                    timestamp = json.get("timestamp")?.asString,
                    toolName = json.get("tool_name")?.asString
                )
                "status" -> ExecutionEvent.Status(
                    status = json.get("status")?.asString ?: "",
                    message = json.get("message")?.asString
                )
                "result" -> ExecutionEvent.Result(
                    success = json.get("success")?.asBoolean ?: false,
                    output = json.get("output")?.asString,
                    error = json.get("error")?.asString,
                    costUsd = json.get("cost_usd")?.asDouble,
                    durationMs = json.get("duration_ms")?.asLong,
                    isTaskComplete = json.get("is_task_complete")?.asBoolean
                )
                "heartbeat" -> ExecutionEvent.Heartbeat
                "error" -> ExecutionEvent.Error(
                    message = json.get("message")?.asString ?: json.get("error")?.asString ?: data
                )
                "done" -> ExecutionEvent.Done
                else -> ExecutionEvent.Log(message = data)
            }
        } catch (_: Exception) {
            when (eventType) {
                "heartbeat" -> ExecutionEvent.Heartbeat
                "done" -> ExecutionEvent.Done
                else -> ExecutionEvent.Log(message = data)
            }
        }
    }

    private fun SessionInfoDto.toDomain(): Agent = Agent(
        sessionId = sessionId,
        sessionName = sessionName,
        status = AgentStatus.fromString(status),
        role = AgentRole.fromString(role ?: "WORKER"),
        model = model,
        systemPrompt = systemPrompt,
        workingDir = workingDir,
        maxTurns = maxTurns,
        timeout = timeout,
        maxIterations = maxIterations,
        createdAt = createdAt,
        totalCost = totalCost,
        storagePath = storagePath,
        errorMessage = errorMessage,
        chatRoomId = chatRoomId,
        workflowId = workflowId,
        graphName = graphName,
        isDeleted = isDeleted ?: false
    )
}
