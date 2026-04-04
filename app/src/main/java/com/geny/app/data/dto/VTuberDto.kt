package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

data class VTuberModelDto(
    val name: String,
    val path: String? = null,
    val thumbnail: String? = null,
    val description: String? = null
)

data class ModelsResponse(
    val models: List<VTuberModelDto>
)

data class ModelAssignRequest(
    @SerializedName("model_name") val modelName: String
)

data class AvatarStateDto(
    val emotion: String? = null,
    @SerializedName("motion_group") val motionGroup: String? = null,
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
    val duration: Float? = null
)

data class ModelAssignmentsResponse(
    val assignments: Map<String, String>
)
