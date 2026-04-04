package com.geny.app.core.network

import com.geny.app.core.storage.SettingsDataStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val serverUrl = settingsDataStore.serverUrl

        if (serverUrl.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val baseUrl = serverUrl.toHttpUrlOrNull() ?: return chain.proceed(originalRequest)

        val newUrl = originalRequest.url.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
