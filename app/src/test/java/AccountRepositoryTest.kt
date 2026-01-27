import com.calai.bitecal.data.account.api.AccountApi
import com.calai.bitecal.data.account.repo.AccountRepository
import com.calai.bitecal.data.auth.repo.TokenStore
import com.calai.bitecal.data.profile.repo.UserProfileStore
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import retrofit2.Retrofit
import retrofit2.create
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

class AccountRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AccountApi

    private lateinit var tokenStore: TokenStore
    private lateinit var profileStore: UserProfileStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true }

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()

        tokenStore = mockk(relaxed = true)
        profileStore = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun deleteAccount_when200_ok_should_clear_local_and_success() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"ok":true}""")
        )

        val repo = AccountRepository(api, tokenStore, profileStore)

        val r = repo.deleteAccount()

        assertTrue(r.isSuccess)

        coVerify(exactly = 1) { tokenStore.clear() }
        coVerify(exactly = 1) { profileStore.clearHasServerProfile() }
        coVerify(exactly = 1) { profileStore.clearOnboarding() }
    }

    @Test
    fun deleteAccount_when401_should_still_clear_local_and_success() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"UNAUTHORIZED"}""")
        )

        val repo = AccountRepository(api, tokenStore, profileStore)

        val r = repo.deleteAccount()

        assertTrue(r.isSuccess)

        coVerify(exactly = 1) { tokenStore.clear() }
        coVerify(exactly = 1) { profileStore.clearHasServerProfile() }
        coVerify(exactly = 1) { profileStore.clearOnboarding() }
    }

    @Test
    fun deleteAccount_when403_should_still_clear_local_and_success() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"FORBIDDEN"}""")
        )

        val repo = AccountRepository(api, tokenStore, profileStore)

        val r = repo.deleteAccount()

        assertTrue(r.isSuccess)

        coVerify(exactly = 1) { tokenStore.clear() }
        coVerify(exactly = 1) { profileStore.clearHasServerProfile() }
        coVerify(exactly = 1) { profileStore.clearOnboarding() }
    }

    @Test
    fun deleteAccount_when500_should_fail() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"INTERNAL"}""")
        )

        val repo = AccountRepository(api, tokenStore, profileStore)

        val r = repo.deleteAccount()

        assertFalse(r.isSuccess)
        assertTrue(r.isFailure)
    }
}
