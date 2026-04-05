package com.geny.app.data.repository

import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.core.storage.TokenManager
import com.geny.app.data.api.VTuberApi
import com.geny.app.data.dto.EmotionOverrideRequest
import com.geny.app.data.dto.InteractRequest
import com.geny.app.data.dto.ModelAssignRequest
import com.geny.app.data.dto.VTuberModelDto
import com.geny.app.data.sse.SseEventSource
import com.geny.app.domain.model.AvatarState
import com.geny.app.domain.model.VTuberModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VTuberRepository @Inject constructor(
    private val vtuberApi: VTuberApi,
    private val sseEventSource: SseEventSource,
    private val tokenManager: TokenManager,
    private val settingsDataStore: SettingsDataStore
) {
    private val gson = Gson()

    fun getServerUrl(): String = settingsDataStore.serverUrl

    suspend fun listModels(): Result<List<VTuberModel>> = runCatching {
        vtuberApi.listModels().models.map { it.toDomain() }
    }

    suspend fun getAssignedModel(agentId: String): Result<VTuberModel> = runCatching {
        val response = vtuberApi.getAssignedModel(agentId)
        response.model?.toDomain() ?: throw NoSuchElementException("No model assigned")
    }

    suspend fun assignModel(agentId: String, modelName: String): Result<Unit> = runCatching {
        vtuberApi.assignModel(agentId, ModelAssignRequest(modelName))
    }

    suspend fun getAvatarState(agentId: String): Result<AvatarState> = runCatching {
        val dto = vtuberApi.getAvatarState(agentId)
        AvatarState(
            emotion = dto.emotion,
            expressionIndex = dto.expressionIndex,
            motionGroup = dto.motionGroup,
            motionIndex = dto.motionIndex,
            intensity = dto.intensity,
            trigger = dto.trigger,
            sessionId = dto.sessionId
        )
    }

    suspend fun interact(agentId: String, hitArea: String, x: Float?, y: Float?): Result<Unit> = runCatching {
        vtuberApi.interact(agentId, InteractRequest(hitArea, x, y))
    }

    suspend fun setEmotion(agentId: String, emotion: String, intensity: Float?): Result<Unit> = runCatching {
        vtuberApi.setEmotion(agentId, EmotionOverrideRequest(emotion, intensity))
    }

    fun streamAvatarEvents(agentId: String): Flow<AvatarState> {
        val url = "${settingsDataStore.serverUrl}/api/vtuber/agents/$agentId/events"
        return sseEventSource.streamSse(url, tokenManager.getToken())
            .mapNotNull { event ->
                if (event.event == "heartbeat") return@mapNotNull null
                try {
                    val json = gson.fromJson(event.data, JsonObject::class.java)
                    AvatarState(
                        emotion = json.get("emotion")?.asString,
                        expressionIndex = json.get("expression_index")?.asInt,
                        motionGroup = json.get("motion_group")?.asString,
                        motionIndex = json.get("motion_index")?.asInt,
                        intensity = json.get("intensity")?.asFloat,
                        trigger = json.get("trigger")?.asString,
                        sessionId = json.get("session_id")?.asString
                    )
                } catch (_: Exception) { null }
            }
    }

    private fun VTuberModelDto.toDomain(): VTuberModel = VTuberModel(
        name = name,
        displayName = displayName,
        description = description,
        url = url ?: path ?: "",
        thumbnail = thumbnail,
        kScale = kScale ?: 0.5f,
        idleMotionGroup = idleMotionGroupName ?: "Idle",
        emotionMap = emotionMap ?: emptyMap()
    )
}
