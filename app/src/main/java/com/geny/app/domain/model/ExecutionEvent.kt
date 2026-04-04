package com.geny.app.domain.model

sealed class ExecutionEvent {
    data class Log(
        val message: String,
        val level: String? = null,
        val timestamp: String? = null,
        val toolName: String? = null
    ) : ExecutionEvent()

    data class Status(
        val status: String,
        val message: String? = null
    ) : ExecutionEvent()

    data class Result(
        val success: Boolean,
        val output: String?,
        val error: String?,
        val costUsd: Double?,
        val durationMs: Long?,
        val isTaskComplete: Boolean?
    ) : ExecutionEvent()

    data object Heartbeat : ExecutionEvent()

    data class Error(
        val message: String,
        val code: String? = null
    ) : ExecutionEvent()

    data object Done : ExecutionEvent()
}
