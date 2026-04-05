package com.geny.app.presentation.tts

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.geny.app.data.repository.TtsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "TtsPlayback"

@Singleton
class TtsPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsRepository: TtsRepository
) {
    private var currentPlayer: ExoPlayer? = null
    private val queue = LinkedList<TtsRequest>()
    private val mutex = Mutex()
    @Volatile private var isProcessing = false

    private data class TtsRequest(
        val agentId: String,
        val text: String,
        val emotion: String?
    )

    suspend fun speak(agentId: String, text: String, emotion: String? = null) {
        Log.i(TAG, "=== speak() CALLED: agentId=$agentId, textLen=${text.length}, emotion=$emotion ===")

        if (text.isBlank() || text.length < 2) {
            Log.d(TAG, "Skipping TTS: text too short (len=${text.length})")
            return
        }

        // Strip markdown/emoji for cleaner TTS
        val cleanText = cleanTextForTts(text)
        if (cleanText.isBlank()) {
            Log.d(TAG, "Skipping TTS: cleaned text is empty")
            return
        }

        Log.d(TAG, "Queuing TTS: agent=$agentId, emotion=$emotion, text=\"${cleanText.take(80)}\"")
        mutex.withLock {
            queue.add(TtsRequest(agentId, cleanText, emotion))
            Log.d(TAG, "Queue size after add: ${queue.size}")
        }
        processQueue()
    }

    private suspend fun processQueue() {
        mutex.withLock {
            if (isProcessing) {
                Log.d(TAG, "processQueue: already processing, returning (queue will be drained by active loop)")
                return
            }
            isProcessing = true
        }

        Log.d(TAG, "processQueue: starting processing loop")
        try {
            while (true) {
                val request = mutex.withLock { queue.poll() } ?: break
                Log.d(TAG, "processQueue: dequeued request, remaining=${queue.size}")
                try {
                    playOne(request)
                } catch (e: Exception) {
                    Log.e(TAG, "Playback error: ${e.javaClass.simpleName}: ${e.message}", e)
                }
            }
            Log.d(TAG, "processQueue: queue drained, exiting loop")
        } finally {
            mutex.withLock { isProcessing = false }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun playOne(request: TtsRequest) {
        Log.i(TAG, "=== playOne START: agent=${request.agentId}, text=\"${request.text.take(60)}\" ===")

        // === Step 1: Download audio from server ===
        Log.d(TAG, "[1/3] Calling ttsRepository.speak()...")

        val body = try {
            val result = ttsRepository.speak(request.agentId, request.text, request.emotion)
            Log.d(TAG, "[1/3] ttsRepository.speak() returned: isSuccess=${result.isSuccess}")
            result.getOrElse { e ->
                Log.e(TAG, "[1/3] FAILED - API error: ${e.javaClass.simpleName}: ${e.message}", e)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "[1/3] FAILED - Exception: ${e.javaClass.simpleName}: ${e.message}", e)
            return
        }

        Log.d(TAG, "[1/3] Got ResponseBody: contentType=${body.contentType()}, contentLength=${body.contentLength()}")

        // === Step 2: Save to temp file ===
        Log.d(TAG, "[2/3] Saving audio to temp file...")
        val tempFile = try {
            withContext(Dispatchers.IO) {
                val contentType = body.contentType()
                val extension = when {
                    contentType?.subtype?.contains("wav") == true -> ".wav"
                    contentType?.subtype?.contains("ogg") == true -> ".ogg"
                    contentType?.subtype?.contains("mpeg") == true -> ".mp3"
                    contentType?.subtype?.contains("mp3") == true -> ".mp3"
                    else -> ".wav" // Default to wav (GPT-SoVITS typically returns wav)
                }
                Log.d(TAG, "[2/3] Content-Type: $contentType, chosen extension: $extension")

                val file = File.createTempFile("tts_", extension, context.cacheDir)
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val copied = input.copyTo(output)
                        Log.d(TAG, "[2/3] Copied $copied bytes to stream")
                    }
                }
                val size = file.length()
                Log.d(TAG, "[2/3] File saved: ${file.absolutePath}, size=$size bytes")

                if (size == 0L) {
                    file.delete()
                    throw Exception("Server returned empty audio (0 bytes)")
                }
                if (size < 100) {
                    // Probably an error response, not audio
                    val content = file.readText().take(200)
                    file.delete()
                    throw Exception("Server returned non-audio response ($size bytes): $content")
                }
                file
            }
        } catch (e: Exception) {
            Log.e(TAG, "[2/3] FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            return
        }

        // === Step 3: Play with ExoPlayer ===
        Log.i(TAG, "[3/3] Playing audio: file=${tempFile.name}, size=${tempFile.length()} bytes")
        try {
            withContext(Dispatchers.Main) {
                suspendCoroutine<Unit> { continuation ->
                    // Release any previous player safely
                    releasePlayer()

                    val exo = ExoPlayer.Builder(context).build()
                    val uri = tempFile.toUri()
                    Log.d(TAG, "[3/3] ExoPlayer created, setting MediaItem: $uri")
                    exo.setMediaItem(MediaItem.fromUri(uri))

                    var resumed = false

                    exo.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val stateName = when (playbackState) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                else -> "UNKNOWN($playbackState)"
                            }
                            Log.d(TAG, "[3/3] ExoPlayer state: $stateName")

                            when (playbackState) {
                                Player.STATE_READY -> {
                                    Log.i(TAG, "[3/3] PLAYING: duration=${exo.duration}ms, volume=${exo.volume}")
                                }
                                Player.STATE_ENDED -> {
                                    Log.i(TAG, "[3/3] Playback COMPLETED")
                                    releasePlayer()
                                    if (!resumed) {
                                        resumed = true
                                        continuation.resume(Unit)
                                    }
                                }
                                else -> {}
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "[3/3] ExoPlayer ERROR: code=${error.errorCode}, msg=${error.message}", error)
                            releasePlayer()
                            if (!resumed) {
                                resumed = true
                                continuation.resume(Unit)
                            }
                        }
                    })

                    currentPlayer = exo
                    exo.prepare()
                    exo.play()
                    Log.d(TAG, "[3/3] ExoPlayer prepare()+play() called")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[3/3] FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
        } finally {
            try {
                tempFile.delete()
                Log.d(TAG, "[3/3] Temp file deleted")
            } catch (_: Exception) {}
        }
        Log.i(TAG, "=== playOne END ===")
    }

    private fun releasePlayer() {
        try {
            currentPlayer?.let { player ->
                Log.d(TAG, "releasePlayer: isPlaying=${player.isPlaying}, state=${player.playbackState}")
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing player: ${e.message}")
        }
        currentPlayer = null
    }

    fun stop() {
        Log.d(TAG, "stop() called — clearing queue and releasing player")
        queue.clear()
        releasePlayer()
    }

    fun isPlaying(): Boolean = currentPlayer?.isPlaying == true

    /**
     * Clean text for TTS: remove markdown formatting, emoji patterns, etc.
     */
    private fun cleanTextForTts(text: String): String {
        return text
            // Remove [emotion] tags like [joy], [anger], etc.
            .replace(Regex("\\[(joy|anger|sadness|fear|surprise|disgust|smirk|neutral)\\]\\s*"), "")
            // Remove markdown bold/italic
            .replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
            // Remove markdown headers
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            // Remove markdown links
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            // Remove markdown code blocks
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("`([^`]+)`"), "$1")
            // Remove common emoji (keep text content)
            .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
            .replace(Regex("[\\u2600-\\u27BF\\u2B50\\u2764\\uFE0F]+"), "")
            // Collapse whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
