package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

data class SpeakRequest(
    val text: String,
    val emotion: String? = null,
    val language: String? = null
)

data class VoiceInfoDto(
    val id: String,
    val name: String,
    val engine: String,
    val language: String? = null,
    val gender: String? = null,
    val preview: String? = null
)

data class VoiceProfileDto(
    val name: String,
    val engine: String,
    @SerializedName("voice_id") val voiceId: String,
    val language: String? = null,
    val speed: Float? = null,
    val pitch: Float? = null,
    val volume: Float? = null
)

data class VoicesResponse(
    val voices: List<VoiceInfoDto>
)

data class ProfilesResponse(
    val profiles: List<VoiceProfileDto>
)

data class TtsStatusResponse(
    val status: String,
    val engines: List<String>? = null
)
