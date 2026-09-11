package com.pixijoy.ai.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pixijoy.ai.BuildConfig
import com.pixijoy.ai.core.network.GeminiApi
import com.pixijoy.ai.core.network.MockPixiJoyBackendApi
import com.pixijoy.ai.core.network.PixiJoyBackendApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    @Named("backendConfigured")
    fun provideBackendConfigured(): Boolean =
        BuildConfig.PIXIJOY_API_BASE_URL.isNotBlank()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Never log bodies (tokens / PII). BASIC only in debug builds.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApi(client: OkHttpClient, gson: Gson): GeminiApi {
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GeminiApi::class.java)
    }

    @Provides
    @Singleton
    @Named("backendBaseUrl")
    fun provideBackendBaseUrl(@Named("backendConfigured") configured: Boolean): String? =
        BuildConfig.PIXIJOY_API_BASE_URL.takeIf { configured }

    @Provides
    @Singleton
    fun providePixiJoyBackendApi(
        client: OkHttpClient,
        gson: Gson,
        @Named("backendBaseUrl") baseUrl: String?,
        mockApi: MockPixiJoyBackendApi
    ): PixiJoyBackendApi {
        if (baseUrl.isNullOrBlank()) return mockApi
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PixiJoyBackendApi::class.java)
    }
}
