// NetworkModule.kt
package com.calai.app.di

import com.calai.app.BuildConfig
import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.users.api.UsersApi
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

    private fun json() = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
    private fun contentType() = "application/json".toMediaType()

    // ✅ 專供 /auth/*：不可帶 Authorization，不掛 Authenticator
    @Provides @Singleton @Named("authClient")
    fun provideAuthOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("authRetrofit")
    fun provideAuthRetrofit(@Named("authClient") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // ⚠️ 一定以 / 結尾
            .client(client)
            .addConverterFactory(json().asConverterFactory(contentType()))
            .build()

    @Provides @Singleton @Named("authApi")
    fun provideAuthApi(@Named("authRetrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    // ✅ 一般 API：自動加 Bearer + 自動 refresh
    @Provides @Singleton @Named("apiClient")
    fun provideApiOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)                 // 加入 Authorization: Bearer <access>
            .authenticator(tokenAuthenticator)               // 401 時自動 refresh
            .addInterceptor(HttpLoggingInterceptor().apply { // 臨時觀察 HEADERS 方便驗證
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("apiRetrofit")
    fun provideApiRetrofit(@Named("apiClient") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // ⚠️ 也要以 / 結尾
            .client(client)
            .addConverterFactory(json().asConverterFactory(contentType()))
            .build()

    @Provides @Singleton
    fun provideProfileApi(@Named("apiRetrofit") retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    @Provides @Singleton
    fun provideUsersApi(@Named("apiRetrofit") retrofit: Retrofit): UsersApi =
        retrofit.create(UsersApi::class.java)
}
