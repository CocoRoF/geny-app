package com.geny.app.data.repository

import com.geny.app.data.api.HealthApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthApi: HealthApi
) {
    suspend fun checkHealth(): Result<Boolean> = runCatching {
        healthApi.checkHealth()
        true
    }
}
