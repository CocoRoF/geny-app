package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

data class VTuberModelDto(
    val name: String,
    @SerializedName("display_name") val displayName: String? = null,
    val description: String? = null,
    val url: String? = null,
    val path: String? = null,
    val thumbnail: String? = null,
    @SerializedName("kScale") val kScale: Float? = null,
    @SerializedName("initialXshift") val initialXshift: Float? = null,
    @SerializedName("initialYshift") val initialYshift: Float? = null,
    @SerializedName("idleMotionGroupName") val idleMotionGroupName: String? = null,
    @SerializedName("emotionMap") val emotionMap: Map<String, Int>? = null,
    @SerializedName("tapMotions") val tapMotions: Map<String, Map<String, Int>>? = null
)

data class ModelsResponse(
    val models: List<VTuberModelDto>
)

data class ModelAssignRequest(
    @SerializedName("model_name") val modelName: String
)

data class AvatarStateDto(
    val emotion: String? = null,
    @SerializedName("expression_index") val expressionIndex: Int? = null,
    @SerializedName("motion_group") val motionGroup: String? = null,
    @SerializedName("motion_index") val motionIndex: Int? = null,
    val intensity: Float? = null,
    val trigger: String? = null,
    @SerializedName("session_id") val sessionId: String? = null
)

data class InteractRequest(
    @SerializedName("hit_area") val hitArea: String,
    val x: Float? = null,
    val y: Float? = null
)

data class EmotionOverrideRequest(
    val emotion: String,
    val intensity: Float? = 1.0f,
    @SerializedName("transition_ms") val transitionMs: Int? = 300
)

data class AgentModelResponse(
    @SerializedName("session_id") val sessionId: String,
    val model: VTuberModelDto?
)

data class ModelAssignmentsResponse(
    val assignments: Map<String, String>
)
