package com.geny.app.presentation.vtuber

import android.content.Context
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Caches Live2D model files (model3.json, textures, motions, expressions, cubism core)
 * on local disk so subsequent loads are instant.
 *
 * Flow:
 * 1. WebView requests a model file URL
 * 2. shouldInterceptRequest -> ModelCacheHelper.getOrFetch(context, url)
 * 3. If cached locally -> return from disk (instant)
 * 4. If not cached -> fetch from server, save to disk, return
 */
object ModelCacheHelper {

    private const val TAG = "ModelCache"
    private const val CACHE_DIR = "live2d_cache"

    fun getOrFetch(context: Context, url: String): WebResourceResponse? {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            val cacheFile = File(cacheDir, urlToFileName(url))

            val bytes: ByteArray
            if (cacheFile.exists() && cacheFile.length() > 0) {
                // Cache hit — serve from local disk
                bytes = cacheFile.readBytes()
                Log.d(TAG, "Cache HIT: ${cacheFile.name} (${bytes.size} bytes)")
            } else {
                // Cache miss — fetch from server and store
                Log.d(TAG, "Cache MISS: $url")
                bytes = fetchBytes(url) ?: return null
                cacheDir.mkdirs()
                cacheFile.writeBytes(bytes)
                Log.d(TAG, "Cached: ${cacheFile.name} (${bytes.size} bytes)")
            }

            val mimeType = guessMimeType(url)
            WebResourceResponse(mimeType, "UTF-8", ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            Log.w(TAG, "Cache error for $url: ${e.message}")
            null // Fall back to normal WebView loading
        }
    }

    /**
     * Clear all cached model files.
     */
    fun clearCache(context: Context) {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            Log.i(TAG, "Cache cleared")
        }
    }

    /**
     * Get total cache size in bytes.
     */
    fun getCacheSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (!cacheDir.exists()) return 0
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun fetchBytes(url: String): ByteArray? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        return try {
            if (connection.responseCode == 200) {
                connection.inputStream.readBytes()
            } else {
                Log.w(TAG, "Fetch failed: ${connection.responseCode} for $url")
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun urlToFileName(url: String): String {
        // Use SHA-256 hash of URL + readable suffix for debugging
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        val suffix = url.substringAfterLast("/").take(40)
        return "${hash}_${suffix}"
    }

    private fun guessMimeType(url: String): String {
        return when {
            url.endsWith(".json") -> "application/json"
            url.endsWith(".png") -> "image/png"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".js") -> "application/javascript"
            url.endsWith(".moc3") -> "application/octet-stream"
            url.endsWith(".motion3.json") -> "application/json"
            url.endsWith(".exp3.json") -> "application/json"
            url.endsWith(".physics3.json") -> "application/json"
            url.endsWith(".pose3.json") -> "application/json"
            url.endsWith(".userdata3.json") -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
