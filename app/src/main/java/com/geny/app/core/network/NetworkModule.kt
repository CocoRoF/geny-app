package com.geny.app.core.network

import com.geny.app.data.api.AgentApi
import com.geny.app.data.api.AuthApi
import com.geny.app.data.api.ChatApi
import com.geny.app.data.api.HealthApi
import com.geny.app.data.api.TtsApi
import com.geny.app.data.api.VTuberApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        serverUrlInterceptor: ServerUrlInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(serverUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost:8000/") // overridden by ServerUrlInterceptor
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAgentApi(retrofit: Retrofit): AgentApi = retrofit.create(AgentApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideVTuberApi(retrofit: Retrofit): VTuberApi = retrofit.create(VTuberApi::class.java)

    @Provides
    @Singleton
    fun provideTtsApi(retrofit: Retrofit): TtsApi = retrofit.create(TtsApi::class.java)

    @Provides
    @Singleton
    fun provideHealthApi(retrofit: Retrofit): HealthApi = retrofit.create(HealthApi::class.java)
}
