package com.geny.app.data.api

import com.geny.app.data.dto.AvatarStateDto
import com.geny.app.data.dto.EmotionOverrideRequest
import com.geny.app.data.dto.InteractRequest
import com.geny.app.data.dto.ModelAssignRequest
import com.geny.app.data.dto.AgentModelResponse
import com.geny.app.data.dto.ModelAssignmentsResponse
import com.geny.app.data.dto.ModelsResponse
import com.geny.app.data.dto.VTuberModelDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface VTuberApi {
    @GET("api/vtuber/models")
    suspend fun listModels(): ModelsResponse

    @GET("api/vtuber/models/{name}")
    suspend fun getModel(@Path("name") name: String): VTuberModelDto

    @PUT("api/vtuber/agents/{id}/model")
    suspend fun assignModel(
        @Path("id") id: String,
        @Body request: ModelAssignRequest
    )

    @GET("api/vtuber/agents/{id}/model")
    suspend fun getAssignedModel(@Path("id") id: String): AgentModelResponse

    @DELETE("api/vtuber/agents/{id}/model")
    suspend fun unassignModel(@Path("id") id: String)

    @GET("api/vtuber/assignments")
    suspend fun getAssignments(): ModelAssignmentsResponse

    @GET("api/vtuber/agents/{id}/state")
    suspend fun getAvatarState(@Path("id") id: String): AvatarStateDto

    @POST("api/vtuber/agents/{id}/interact")
    suspend fun interact(
        @Path("id") id: String,
        @Body request: InteractRequest
    )

    @POST("api/vtuber/agents/{id}/emotion")
    suspend fun setEmotion(
        @Path("id") id: String,
        @Body request: EmotionOverrideRequest
    )
}
