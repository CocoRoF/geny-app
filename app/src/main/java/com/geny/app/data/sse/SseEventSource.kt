package com.geny.app.data.sse

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SseEventSource"

@Singleton
class SseEventSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    // Dedicated client with long read timeout for SSE
    private val sseClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .readTimeout(5, TimeUnit.MINUTES)  // SSE needs long timeout
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun streamSse(
        url: String,
        token: String? = null,
        lastEventId: String? = null
    ): Flow<SseEvent> = callbackFlow {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")

        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        lastEventId?.let { requestBuilder.header("Last-Event-ID", it) }

        val call = sseClient.newCall(requestBuilder.build())

        launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting SSE: $url")
                val response = call.execute()

                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    throw IOException("SSE connection failed: $code")
                }

                Log.d(TAG, "SSE connected: $url")
                val body = response.body ?: throw IOException("Empty SSE response body")
                val reader = BufferedReader(InputStreamReader(body.byteStream()))

                var currentEvent = ""
                val currentData = StringBuilder()

                while (isActive) {
                    val line = reader.readLine() ?: break

                    when {
                        line.startsWith("event:") -> {
                            currentEvent = line.removePrefix("event:").trim()
                        }
                        line.startsWith("data:") -> {
                            if (currentData.isNotEmpty()) currentData.append("\n")
                            currentData.append(line.removePrefix("data:").trim())
                        }
                        line.isEmpty() && (currentEvent.isNotEmpty() || currentData.isNotEmpty()) -> {
                            val event = SseEvent(
                                event = currentEvent.ifEmpty { "message" },
                                data = currentData.toString()
                            )
                            trySend(event)
                            currentEvent = ""
                            currentData.clear()
                        }
                    }
                }

                Log.d(TAG, "SSE stream ended: $url")
                reader.close()
                response.close()
                // Stream ended normally — throw to trigger retry
                throw IOException("SSE stream closed by server")
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(TAG, "SSE error ($url): ${e.message}")
                    close(e)
                }
            }
        }

        awaitClose {
            call.cancel()
        }
    }.retryWhen { cause, attempt ->
        // Retry indefinitely for IO errors (SSE connections can drop)
        if (cause is IOException) {
            val delayMs = minOf(1000L * (1L shl minOf(attempt.toInt(), 5)), MAX_RETRY_DELAY_MS)
            Log.d(TAG, "SSE retry #$attempt in ${delayMs}ms: ${cause.message}")
            delay(delayMs)
            true
        } else {
            Log.e(TAG, "SSE non-retryable error: ${cause.message}")
            false
        }
    }

    companion object {
        private const val MAX_RETRY_DELAY_MS = 30_000L
    }
}
