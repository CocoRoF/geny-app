package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class SetupRequest(
    val username: String,
    val password: String,
    @SerializedName("display_name") val displayName: String? = null
)

data class AuthStatusResponse(
    @SerializedName("has_users") val hasUsers: Boolean,
    @SerializedName("is_authenticated") val isAuthenticated: Boolean
)

data class AuthTokenResponse(
    @SerializedName("access_token") val token: String,
    @SerializedName("token_type") val tokenType: String? = null,
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null
)

data class UserInfoResponse(
    val username: String,
    @SerializedName("display_name") val displayName: String? = null
)
