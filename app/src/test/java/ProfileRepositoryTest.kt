import com.calai.app.data.profile.ProfileApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ProfileRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ProfileApi

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProfileApi::class.java)
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun getMyProfile_200() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"gender":"male","age":30}""")
        )
        val dto = api.getMyProfile()
        Assert.assertEquals("male", dto.gender)
        Assert.assertEquals(30, dto.age)
    }
}