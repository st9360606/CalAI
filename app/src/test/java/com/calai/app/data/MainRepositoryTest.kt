package com.calai.app.data

import com.calai.app.core.AppResult
import com.calai.app.net.ApiService
import com.calai.app.net.InfoDTO
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MainRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: MainRepository

    @Before
    fun setup() {
        server = MockWebServer().apply { start() }

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        // ★ 與正式程式一致：先 JSON、後 Scalars
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(ScalarsConverterFactory.create())   // ✅ 先 Scalars
            .addConverterFactory(GsonConverterFactory.create())      // ✅ 再 Gson
            .client(client)
            .build()

        val api = retrofit.create(ApiService::class.java)
        repo = MainRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun hello_success() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("Cal AI backend is up!")  // 跟後端一致
        )

        val result = repo.hello()

        // 要先呼叫 repo.hello() 再拿 request
        val req = server.takeRequest()
        assertEquals("/api/hello", req.path)

        when (result) {
            is AppResult.Success<*> ->
                assertEquals("Cal AI backend is up!", (result.data as String).trim())
            is AppResult.Error ->
                fail("Expected Success, but got Error: ${result.message}; cause=${result.cause}")
        }
    }



    @Test
    fun hello_http_500() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val r = repo.hello()
        assertTrue(r is AppResult.Error, "Expected AppResult.Error, got $r")
        if (r is AppResult.Error) {
            val code = (r.cause as? HttpException)?.code()
            assertEquals(500, code, "Expected HttpException(500), got $code with message='${r.message}'")
        }
    }

    @Test
    fun info_success_json() = runTest {
        val json = """{"message":"hi","serverTime":"2025-09-13T00:00:00"}"""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(json)
        )
        val r = repo.info()
        when (r) {
            is AppResult.Success<*> -> {
                val dto = r.data as InfoDTO
                assertEquals("hi", dto.message)
            }
            is AppResult.Error -> fail("Expected Success, but got Error: ${r.message}")
        }
    }
}
