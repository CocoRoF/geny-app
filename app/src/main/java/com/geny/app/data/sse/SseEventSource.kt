package com.geny.app.data.sse

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseEventSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
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

        val call = okHttpClient.newCall(requestBuilder.build())

        launch(Dispatchers.IO) {
            try {
                val response = call.execute()

                if (!response.isSuccessful) {
                    throw IOException("SSE connection failed: ${response.code}")
                }

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

                reader.close()
                response.close()
            } catch (e: Exception) {
                if (isActive) {
                    close(e)
                }
            }
        }

        awaitClose {
            call.cancel()
        }
    }.retryWhen { cause, attempt ->
        if (cause is IOException && attempt < MAX_RETRIES) {
            val delayMs = minOf(1000L * (1L shl attempt.toInt()), MAX_RETRY_DELAY_MS)
            delay(delayMs)
            true
        } else {
            false
        }
    }

    companion object {
        private const val MAX_RETRIES = 5L
        private const val MAX_RETRY_DELAY_MS = 30_000L
    }
}
