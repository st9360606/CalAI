package com.calai.app.di

import com.calai.app.BuildConfig
import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.auth.net.AuthInterceptor
import com.calai.app.data.auth.net.TokenAuthenticator
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 共用 logging
    private fun logging() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // 1) 專用於 /auth/* 的 OkHttp（不掛 authenticator，避免循環）
    @Provides @Singleton @Named("authClient")
    fun provideAuthOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            // 可選：/auth/* 通常不需要帶 Authorization；若你想乾淨，這行也可拿掉
            // .addInterceptor(authInterceptor)
            .addInterceptor(logging())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    // 2) 一般 API 用的 OkHttp（掛 AuthInterceptor + TokenAuthenticator）
    @Provides @Singleton @Named("apiClient")
    fun provideApiOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    private fun json() = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private fun contentType() = "application/json".toMediaType()

    // 3) auth 用 Retrofit（不含 authenticator）
    @Provides @Singleton @Named("authRetrofit")
    fun provideAuthRetrofit(@Named("authClient") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)       // 確保結尾有 /
            .client(client)
            .addConverterFactory(json().asConverterFactory(contentType()))
            .build()

    // 4) 一般 API 用 Retrofit（含 authenticator）
    @Provides @Singleton @Named("apiRetrofit")
    fun provideApiRetrofit(@Named("apiClient") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json().asConverterFactory(contentType()))
            .build()

    // 5) 提供 AuthApi 使用「authRetrofit」
    @Provides @Singleton @Named("authApi")
    fun provideAuthApi(@Named("authRetrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}
