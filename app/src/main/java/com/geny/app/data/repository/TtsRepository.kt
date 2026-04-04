package com.geny.app.data.repository

import com.geny.app.data.api.TtsApi
import com.geny.app.data.dto.SpeakRequest
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepository @Inject constructor(
    private val ttsApi: TtsApi
) {
    suspend fun speak(agentId: String, text: String, emotion: String? = null): Result<ResponseBody> = runCatching {
        ttsApi.speak(agentId, SpeakRequest(text, emotion))
    }

    suspend fun getVoices() = runCatching { ttsApi.getVoices() }

    suspend fun getProfiles() = runCatching { ttsApi.getProfiles() }
}
