package com.geny.app.presentation.tts

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.geny.app.core.storage.SettingsDataStore
import com.geny.app.core.storage.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val settingsDataStore: SettingsDataStore
) {
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    fun speak(agentId: String, text: String, emotion: String? = null) {
        stop()

        val serverUrl = settingsDataStore.serverUrl
        if (serverUrl.isBlank()) return

        val url = "$serverUrl/api/tts/agents/$agentId/speak"

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(
                buildMap {
                    put("Content-Type", "application/json")
                    tokenManager.getToken()?.let { put("Authorization", "Bearer $it") }
                }
            )

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        player = ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            play()
        }
    }

    fun stop() {
        player?.release()
        player = null
    }

    fun isPlaying(): Boolean = player?.isPlaying == true
}
