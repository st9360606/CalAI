import com.calai.app.data.billing.ActiveSub
import com.calai.app.data.billing.BillingGateway
import com.calai.app.data.entitlement.EntitlementSyncer
import com.calai.app.data.entitlement.api.EntitlementApi
import com.calai.app.data.entitlement.api.EntitlementSyncRequest
import com.calai.app.data.entitlement.api.EntitlementSyncResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class EntitlementSyncerTest {

    @Test
    fun sync_whenNoSubs_shouldNotCallApi() = runBlocking {
        val billing = object : BillingGateway {
            override suspend fun queryActiveSubscriptions(): List<ActiveSub> = emptyList()
        }
        val api = mockk<EntitlementApi>(relaxed = true)

        val s = EntitlementSyncer(billing, api)
        s.syncAfterLoginSilently()

        coVerify(exactly = 0) { api.sync(any()) }
    }

    @Test
    fun sync_whenHasSubs_shouldCallApiOnce() = runBlocking {
        val billing = object : BillingGateway {
            override suspend fun queryActiveSubscriptions(): List<ActiveSub> =
                listOf(ActiveSub(productId = "monthly", purchaseToken = "tok123"))
        }
        val api = mockk<EntitlementApi>()

        coEvery { api.sync(any()) } returns EntitlementSyncResponse(
            status = "ACTIVE",
            entitlementType = "MONTHLY"
        )

        val s = EntitlementSyncer(billing, api)
        s.syncAfterLoginSilently()

        coVerify(exactly = 1) {
            api.sync(match { req: EntitlementSyncRequest ->
                req.purchases.size == 1 &&
                        req.purchases[0].productId == "monthly" &&
                        req.purchases[0].purchaseToken == "tok123"
            })
        }
    }
}
