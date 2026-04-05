package com.geny.app.domain.model

data class VTuberModel(
    val name: String,
    val displayName: String?,
    val description: String?,
    val url: String,
    val thumbnail: String?,
    val kScale: Float,
    val idleMotionGroup: String,
    val emotionMap: Map<String, Int>
)

data class AvatarState(
    val emotion: String?,
    val expressionIndex: Int?,
    val motionGroup: String?,
    val motionIndex: Int?,
    val intensity: Float?,
    val trigger: String?,
    val sessionId: String?
)
