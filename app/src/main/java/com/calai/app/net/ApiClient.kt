package com.calai.app.net

import com.calai.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            // 先讓純文字的 hello() 命中 Scalars
            .addConverterFactory(ScalarsConverterFactory.create())
            // 再讓 JSON 的 info() 命中 Gson
            .addConverterFactory(GsonConverterFactory.create())
            .client(http)
            .build()
            .create(ApiService::class.java)
    }

}
