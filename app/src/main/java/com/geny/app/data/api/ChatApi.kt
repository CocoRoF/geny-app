package com.geny.app.data.api

import com.geny.app.data.dto.BroadcastRequest
import com.geny.app.data.dto.BroadcastResponse
import com.geny.app.data.dto.ChatRoomDto
import com.geny.app.data.dto.CreateRoomRequest
import com.geny.app.data.dto.ChatMessageDto
import com.geny.app.data.dto.UpdateRoomRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {
    @GET("api/chat/rooms")
    suspend fun listRooms(): List<ChatRoomDto>

    @POST("api/chat/rooms")
    suspend fun createRoom(@Body request: CreateRoomRequest): ChatRoomDto

    @GET("api/chat/rooms/{id}")
    suspend fun getRoom(@Path("id") id: String): ChatRoomDto

    @PATCH("api/chat/rooms/{id}")
    suspend fun updateRoom(
        @Path("id") id: String,
        @Body request: UpdateRoomRequest
    ): ChatRoomDto

    @DELETE("api/chat/rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: String)

    @GET("api/chat/rooms/{id}/messages")
    suspend fun getMessages(@Path("id") id: String): List<ChatMessageDto>

    @POST("api/chat/rooms/{id}/broadcast")
    suspend fun broadcast(
        @Path("id") id: String,
        @Body request: BroadcastRequest
    ): BroadcastResponse
}
