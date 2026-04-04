package com.geny.app.data.api

import com.geny.app.data.dto.ProfilesResponse
import com.geny.app.data.dto.SpeakRequest
import com.geny.app.data.dto.TtsStatusResponse
import com.geny.app.data.dto.VoicesResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface TtsApi {
    @Streaming
    @POST("api/tts/agents/{id}/speak")
    suspend fun speak(
        @Path("id") id: String,
        @Body request: SpeakRequest
    ): ResponseBody

    @GET("api/tts/voices")
    suspend fun getVoices(): VoicesResponse

    @GET("api/tts/profiles")
    suspend fun getProfiles(): ProfilesResponse

    @GET("api/tts/status")
    suspend fun getStatus(): TtsStatusResponse
}
