package com.geny.app.data.api

import com.geny.app.data.dto.CreateAgentRequest
import com.geny.app.data.dto.ExecuteRequestDto
import com.geny.app.data.dto.ExecuteResponseDto
import com.geny.app.data.dto.ExecutionStatusDto
import com.geny.app.data.dto.SessionInfoDto
import com.geny.app.data.dto.StorageFileContentDto
import com.geny.app.data.dto.StorageListResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

interface AgentApi {
    @GET("api/agents")
    suspend fun listAgents(): List<SessionInfoDto>

    @POST("api/agents")
    suspend fun createAgent(@Body request: CreateAgentRequest): SessionInfoDto

    @GET("api/agents/{id}")
    suspend fun getAgent(@Path("id") id: String): SessionInfoDto

    @DELETE("api/agents/{id}")
    suspend fun deleteAgent(@Path("id") id: String)

    @POST("api/agents/{id}/restore")
    suspend fun restoreAgent(@Path("id") id: String)

    @DELETE("api/agents/{id}/permanent")
    suspend fun permanentDeleteAgent(@Path("id") id: String)

    @POST("api/agents/{id}/execute/start")
    suspend fun startExecution(
        @Path("id") id: String,
        @Body request: ExecuteRequestDto
    ): ExecuteResponseDto

    @POST("api/agents/{id}/stop")
    suspend fun stopExecution(@Path("id") id: String)

    @GET("api/agents/{id}/execute/status")
    suspend fun getExecutionStatus(@Path("id") id: String): ExecutionStatusDto

    @GET("api/agents/{id}/storage")
    suspend fun listStorage(@Path("id") id: String): StorageListResponse

    @GET("api/agents/{id}/storage/{path}")
    suspend fun readFile(
        @Path("id") id: String,
        @Path("path", encoded = true) path: String
    ): StorageFileContentDto

    @Streaming
    @GET("api/agents/{id}/download-folder")
    suspend fun downloadFolder(@Path("id") id: String): ResponseBody

    @GET("api/agents/{id}/system-prompt")
    suspend fun getSystemPrompt(@Path("id") id: String): Map<String, String>

    @PUT("api/agents/{id}/system-prompt")
    suspend fun updateSystemPrompt(
        @Path("id") id: String,
        @Body request: Map<String, String>
    )
}
