package com.geny.app.data.api

import com.geny.app.data.dto.MemoryFileResponse
import com.geny.app.data.dto.MemoryGraphResponse
import com.geny.app.data.dto.MemoryIndexResponse
import com.geny.app.data.dto.MemorySearchResponse
import com.geny.app.data.dto.MemoryTagsResponse
import com.geny.app.data.dto.WriteNoteRequest
import com.geny.app.data.dto.UpdateNoteRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MemoryApi {
    // Index & Stats
    @GET("api/agents/{id}/memory")
    suspend fun getMemoryIndex(@Path("id") agentId: String): MemoryIndexResponse

    @GET("api/agents/{id}/memory/tags")
    suspend fun getMemoryTags(@Path("id") agentId: String): MemoryTagsResponse

    @GET("api/agents/{id}/memory/graph")
    suspend fun getMemoryGraph(@Path("id") agentId: String): MemoryGraphResponse

    // Search
    @GET("api/agents/{id}/memory/search")
    suspend fun searchMemory(
        @Path("id") agentId: String,
        @Query("q") query: String,
        @Query("max_results") maxResults: Int? = 20
    ): MemorySearchResponse

    // CRUD
    @GET("api/agents/{id}/memory/files/{filename}")
    suspend fun getMemoryFile(
        @Path("id") agentId: String,
        @Path("filename", encoded = true) filename: String
    ): MemoryFileResponse

    @POST("api/agents/{id}/memory/files")
    suspend fun createMemoryFile(
        @Path("id") agentId: String,
        @Body request: WriteNoteRequest
    ): Map<String, String>

    @PUT("api/agents/{id}/memory/files/{filename}")
    suspend fun updateMemoryFile(
        @Path("id") agentId: String,
        @Path("filename", encoded = true) filename: String,
        @Body request: UpdateNoteRequest
    ): Map<String, String>

    @DELETE("api/agents/{id}/memory/files/{filename}")
    suspend fun deleteMemoryFile(
        @Path("id") agentId: String,
        @Path("filename", encoded = true) filename: String
    ): Map<String, String>

    // Promote to global
    @POST("api/agents/{id}/memory/promote")
    suspend fun promoteToGlobal(
        @Path("id") agentId: String,
        @Body request: Map<String, String>
    ): Map<String, String>

    // Reindex
    @POST("api/agents/{id}/memory/reindex")
    suspend fun reindex(@Path("id") agentId: String): Map<String, Any>
}

// Global Memory API
interface GlobalMemoryApi {
    @GET("api/memory/global")
    suspend fun getGlobalIndex(): MemoryIndexResponse

    @GET("api/memory/global/files/{filename}")
    suspend fun getGlobalFile(
        @Path("filename", encoded = true) filename: String
    ): MemoryFileResponse

    @POST("api/memory/global/files")
    suspend fun createGlobalFile(@Body request: WriteNoteRequest): Map<String, String>

    @PUT("api/memory/global/files/{filename}")
    suspend fun updateGlobalFile(
        @Path("filename", encoded = true) filename: String,
        @Body request: UpdateNoteRequest
    ): Map<String, String>

    @DELETE("api/memory/global/files/{filename}")
    suspend fun deleteGlobalFile(
        @Path("filename", encoded = true) filename: String
    ): Map<String, String>

    @GET("api/memory/global/search")
    suspend fun searchGlobal(
        @Query("q") query: String,
        @Query("max_results") maxResults: Int? = 5
    ): MemorySearchResponse
}

// User Opsidian API — personal knowledge vault
interface UserOpsidianApi {
    @GET("api/opsidian")
    suspend fun getIndex(): MemoryIndexResponse

    @GET("api/opsidian/graph")
    suspend fun getGraph(): MemoryGraphResponse

    @GET("api/opsidian/tags")
    suspend fun getTags(): MemoryTagsResponse

    @GET("api/opsidian/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("max_results") maxResults: Int? = 10
    ): MemorySearchResponse

    @GET("api/opsidian/files/{filename}")
    suspend fun getFile(
        @Path("filename", encoded = true) filename: String
    ): MemoryFileResponse

    @POST("api/opsidian/files")
    suspend fun createFile(@Body request: WriteNoteRequest): Map<String, String>

    @PUT("api/opsidian/files/{filename}")
    suspend fun updateFile(
        @Path("filename", encoded = true) filename: String,
        @Body request: UpdateNoteRequest
    ): Map<String, String>

    @DELETE("api/opsidian/files/{filename}")
    suspend fun deleteFile(
        @Path("filename", encoded = true) filename: String
    ): Map<String, String>

    @POST("api/opsidian/reindex")
    suspend fun reindex(): Map<String, Any>
}

// Curated Knowledge API — quality-refined knowledge vault
interface CuratedKnowledgeApi {
    @GET("api/curated")
    suspend fun getIndex(): MemoryIndexResponse

    @GET("api/curated/graph")
    suspend fun getGraph(): MemoryGraphResponse

    @GET("api/curated/tags")
    suspend fun getTags(): MemoryTagsResponse

    @GET("api/curated/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("max_results") maxResults: Int? = 10
    ): MemorySearchResponse

    @GET("api/curated/files/{filename}")
    suspend fun getFile(
        @Path("filename", encoded = true) filename: String
    ): MemoryFileResponse

    @POST("api/curated/files")
    suspend fun createFile(@Body request: WriteNoteRequest): Map<String, String>

    @PUT("api/curated/files/{filename}")
    suspend fun updateFile(
        @Path("filename", encoded = true) filename: String,
        @Body request: UpdateNoteRequest
    ): Map<String, String>

    @DELETE("api/curated/files/{filename}")
    suspend fun deleteFile(
        @Path("filename", encoded = true) filename: String
    ): Map<String, String>

    @POST("api/curated/reindex")
    suspend fun reindex(): Map<String, Any>
}
