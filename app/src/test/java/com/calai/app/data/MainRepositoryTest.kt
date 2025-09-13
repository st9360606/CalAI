package com.calai.app.data

import com.calai.app.core.net.NetworkResult
import com.calai.app.net.ApiService
import com.calai.app.net.InfoDTO
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class MainRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: MainRepository

    @Before fun setup() {
        server = MockWebServer().apply { start() }

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val api = retrofit.create(ApiService::class.java)
        repo = MainRepository(api)
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun hello_success() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))
        val r = repo.hello()
        assertTrue(r is NetworkResult.Success && r.data == "OK")
    }

    @Test fun hello_http_500() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val r = repo.hello()
        assertTrue(r is NetworkResult.HttpError && r.code == 500)
    }

    @Test fun info_success_json() = runTest {
        val json = """{"message":"hi","serverTime":"2025-09-13T00:00:00"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))
        val r = repo.info()
        assertTrue(r is NetworkResult.Success && (r as NetworkResult.Success<InfoDTO>).data.message == "hi")
    }
}
