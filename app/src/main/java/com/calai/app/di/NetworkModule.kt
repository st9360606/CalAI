package com.calai.app.di

import android.content.Context
import com.calai.app.BuildConfig
import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.auth.net.AuthInterceptor
import com.calai.app.data.auth.net.TokenAuthenticator
import com.calai.app.data.fasting.api.FastingApi
import com.calai.app.data.fasting.notifications.FastingAlarmScheduler
import com.calai.app.data.fasting.repo.FastingRepository
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.users.api.UsersApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun json() = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
    private fun contentType() = "application/json".toMediaType()

    // ★ 所有請求一律帶上使用者本地時區（IANA）
    private fun tzHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val tz = ZoneId.systemDefault().id
        val req = chain.request().newBuilder()
            .header("X-Client-Timezone", tz)
            .build()
        chain.proceed(req)
    }

    // --- auth 客戶端（不可帶 Authorization、不掛 Authenticator） ---
    @Provides @Singleton @Named("authClient")
    fun provideAuthOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tzHeaderInterceptor()) // ★ FE-FP-5
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("authRetrofit")
    fun provideAuthRetrofit(@Named("authClient") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json().asConverterFactory(contentType()))
            .build()

    @Provides @Singleton @Named("authApi")
    fun provideAuthApi(@Named("authRetrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    // --- 一般 API 客戶端（自動 Bearer + 自動 refresh） ---
    @Provides @Singleton @Named("apiClient")
    fun provideApiOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tzHeaderInterceptor()) // ★ FE-FP-5
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("apiRetrofit")
    fun provideApiRetrofit(@Named("apiClient") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json().asConverterFactory(contentType()))
            .build()

    @Provides @Singleton fun provideProfileApi(@Named("apiRetrofit") retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    @Provides @Singleton fun provideUsersApi(@Named("apiRetrofit") retrofit: Retrofit): UsersApi =
        retrofit.create(UsersApi::class.java)

    @Provides @Singleton fun provideFastingApi(@Named("apiRetrofit") retrofit: Retrofit): FastingApi =
        retrofit.create(FastingApi::class.java)

    @Provides @Singleton fun provideZoneId(): ZoneId = ZoneId.systemDefault()

    @Provides @Singleton
    fun provideFastingRepository(api: FastingApi, zoneId: ZoneId): FastingRepository =
        FastingRepository(api) { zoneId }

    @Provides @Singleton
    fun provideFastingAlarmScheduler(@ApplicationContext ctx: Context): FastingAlarmScheduler =
        FastingAlarmScheduler(ctx)
}
