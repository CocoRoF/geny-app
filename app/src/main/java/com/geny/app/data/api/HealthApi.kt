package com.geny.app.data.api

import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): Map<String, Any>
}
