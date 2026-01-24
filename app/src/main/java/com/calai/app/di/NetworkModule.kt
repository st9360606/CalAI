package com.calai.app.di

import android.content.Context
import com.calai.app.BuildConfig
import com.calai.app.data.account.api.AccountApi
import com.calai.app.data.activity.api.DailyActivityApi
import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.auth.net.AuthInterceptor
import com.calai.app.data.auth.net.TokenAuthenticator
import com.calai.app.data.fasting.api.FastingApi
import com.calai.app.data.fasting.notifications.FastingAlarmScheduler
import com.calai.app.data.fasting.repo.FastingRepository
import com.calai.app.data.foodlog.api.FoodLogsApi
import com.calai.app.data.profile.api.AutoGoalsApi
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.users.api.UsersApi
import com.calai.app.data.water.api.WaterApi
import com.calai.app.data.weight.api.WeightApi
import com.calai.app.data.workout.api.WorkoutApi
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

    private fun json() = Json {
        ignoreUnknownKeys = true
        explicitNulls = true     // ✅ 讓 null 真的送出（符合 PUT 全量覆蓋）
        encodeDefaults = false
    }
    private fun contentType() = "application/json".toMediaType()

    // ★ 所有請求一律帶上使用者本地時區（IANA）
    private fun tzHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val tz = ZoneId.systemDefault().id
        val req = chain.request().newBuilder()
            .header("X-Client-Timezone", tz)
            .build()
        chain.proceed(req)
    }

    private fun logging(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // Debug 顯示標頭；Release 關閉或降到 BASIC
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
    }

    @Provides @Singleton @Named("authClient")
    fun provideAuthOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tzHeaderInterceptor())
            .addInterceptor(logging())              // ← 改用封裝好的 logging()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("apiClient")
    fun provideApiOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tzHeaderInterceptor())
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging())              // ← 同上
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

    @Provides @Singleton
    fun provideWaterApi(@Named("apiRetrofit") retrofit: Retrofit): WaterApi =
        retrofit.create(WaterApi::class.java)

    @Provides @Singleton
    fun provideWorkoutApi(
        @Named("apiRetrofit") retrofit: Retrofit
    ): WorkoutApi = retrofit.create(WorkoutApi::class.java)

    @Provides @Singleton
    fun provideWeightApi(@Named("apiRetrofit") retrofit: Retrofit): WeightApi =
        retrofit.create(WeightApi::class.java)

    @Provides
    @Singleton
    fun provideAutoGoalsApi(@Named("apiRetrofit") retrofit: Retrofit): AutoGoalsApi =
        retrofit.create(AutoGoalsApi::class.java)

    @Provides
    @Singleton
    fun provideDailyActivityApi(
        @Named("apiRetrofit") retrofit: Retrofit
    ): DailyActivityApi = retrofit.create(DailyActivityApi::class.java)

    @Provides
    @Singleton
    fun provideFoodLogsApi(@Named("apiRetrofit") retrofit: Retrofit): FoodLogsApi =
        retrofit.create(FoodLogsApi::class.java)

    @Provides
    @Singleton
    fun provideAccountApi(@Named("apiRetrofit") retrofit: Retrofit): AccountApi =
        retrofit.create(AccountApi::class.java)

}
