package com.geny.app.domain.model

data class VTuberModel(
    val name: String,
    val path: String?,
    val thumbnail: String?,
    val description: String?
)

data class AvatarState(
    val emotion: String?,
    val motionGroup: String?,
    val intensity: Float?,
    val trigger: String?,
    val sessionId: String?
)
