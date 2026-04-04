package com.geny.app.data.api

import com.geny.app.data.dto.AuthStatusResponse
import com.geny.app.data.dto.AuthTokenResponse
import com.geny.app.data.dto.LoginRequest
import com.geny.app.data.dto.SetupRequest
import com.geny.app.data.dto.UserInfoResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @GET("api/auth/status")
    suspend fun getAuthStatus(): AuthStatusResponse

    @POST("api/auth/setup")
    suspend fun setup(@Body request: SetupRequest): AuthTokenResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthTokenResponse

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/me")
    suspend fun getCurrentUser(): UserInfoResponse
}
