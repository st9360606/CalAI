package com.calai.bitecal.data.entitlement

import android.app.Activity
import android.util.Log
import com.calai.bitecal.data.billing.BillingGateway
import com.calai.bitecal.data.billing.BillingPurchaseResult
import com.calai.bitecal.data.billing.FakeBillingGateway
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.api.EntitlementApi
import com.calai.bitecal.data.entitlement.api.EntitlementSyncRequest
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import com.calai.bitecal.data.entitlement.api.PurchaseTokenPayload
import com.calai.bitecal.data.entitlement.model.PremiumStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

data class PurchaseEntitlementResult(
    val success: Boolean,
    val response: EntitlementSyncResponse? = null,
    val message: String? = null
)

class EntitlementSyncer(
    private val billing: BillingGateway,
    private val api: EntitlementApi,
) {
    suspend fun syncAfterLoginSilently() = withContext(Dispatchers.IO) {
        runCatching {
            refreshEntitlementSummary()
        }.onFailure {
            Log.w(TAG, "syncAfterLoginSilently failed: ${it.message}")
        }
        Unit
    }

    /**
     * 登入/Onboarding 完成後用來判斷是否應該略過 paywall。
     *
     * 順序：
     * 1. 先查 Play Billing 本機 active subscriptions。
     * 2. 若有 active purchase token，送後端 /sync，避免後端 entitlement 狀態過期或尚未建立。
     * 3. 若本機沒有 active purchase，仍查 /me，支援後端已存在的有效權益。
     */
    suspend fun refreshEntitlementSummary(): EntitlementSyncResponse = withContext(Dispatchers.IO) {
        val subs = runCatching { billing.queryActiveSubscriptions() }
            .onFailure { Log.w(TAG, "queryActiveSubscriptions failed: ${it.message}") }
            .getOrDefault(emptyList())

        if (subs.isNotEmpty()) {
            val payload = subs.map {
                PurchaseTokenPayload(
                    productId = it.productId,
                    purchaseToken = it.purchaseToken
                )
            }

            val response = runCatching {
                api.sync(EntitlementSyncRequest(purchases = payload))
            }.getOrElse { syncError ->
                Log.w(TAG, "entitlement sync failed, fallback to /me: ${syncError.message}")
                api.me()
            }

            // Acknowledge 也放在 restore path：如果前一次 acknowledge 因網路失敗，登入/啟動後還能補償重試。
            subs.filter { !it.acknowledged }
                .forEach { acknowledgeWithRetry(it.purchaseToken) }

            response
        } else {
            api.me()
        }
    }

    suspend fun hasActivePremiumAccess(): Boolean {
        return runCatching {
            val response = refreshEntitlementSummary()
            response.status.equals("ACTIVE", ignoreCase = true) &&
                    PremiumStatus.from(response.premiumStatus).let {
                        it == PremiumStatus.TRIAL || it == PremiumStatus.PREMIUM
                    }
        }.onFailure {
            Log.w(TAG, "hasActivePremiumAccess failed: ${it.message}")
        }.getOrDefault(false)
    }

    suspend fun purchaseSubscriptionAndSync(
        activity: Activity,
        productId: String,
        offerTag: String? = null
    ): PurchaseEntitlementResult {
        val purchaseResult = billing.launchSubscriptionPurchase(
            activity = activity,
            productId = productId,
            offerTag = offerTag
        )

        return when (purchaseResult) {
            BillingPurchaseResult.Cancelled -> {
                PurchaseEntitlementResult(
                    success = false,
                    message = "Purchase cancelled"
                )
            }

            BillingPurchaseResult.Pending -> {
                PurchaseEntitlementResult(
                    success = false,
                    message = "Purchase is pending. Please wait until Google Play confirms the payment."
                )
            }

            is BillingPurchaseResult.Error -> {
                PurchaseEntitlementResult(
                    success = false,
                    message = purchaseResult.message
                )
            }

            is BillingPurchaseResult.Success -> {
                val sub = purchaseResult.sub
                //devDebug下 fake purchase 後 HOME 用戶狀態會變PREMIUM嗎?
                /**
                會，但只是在 App 當下記憶體裡變成 PREMIUM / TRIAL。

                devDebug fake purchase 後：
                如果 trialEnabled = true
                App 會收到 fake response：premiumStatus = "TRIAL"
                會導到 HOME
                        如果 trialEnabled = false
                App 會收到 fake response：premiumStatus = "PREMIUM"
                會導到 HOME

                但重點是：不會真的寫入後端 DB。下次重新登入、重開 App、或重新查 /entitlements/me，還是會以後端資料為準。
                */
                if (billing is FakeBillingGateway) {
                    if (!sub.acknowledged) {
                        acknowledgeWithRetry(sub.purchaseToken)
                    }

                    val isTrialOffer =
                        offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_TRIAL_DISCOUNT_YEARLY
                    val fakeUntil = Instant.now()
                        .plus(if (isTrialOffer) 3 else 30, ChronoUnit.DAYS)
                        .toString()

                    return PurchaseEntitlementResult(
                        success = true,
                        response = EntitlementSyncResponse(
                            status = "ACTIVE",
                            entitlementType = if (isTrialOffer) "TRIAL" else "DEV_FAKE",
                            premiumStatus = if (isTrialOffer) "TRIAL" else "PREMIUM",
                            currentPremiumUntil = fakeUntil,
                            trialEndsAt = if (isTrialOffer) fakeUntil else null,
                            trialDaysLeft = if (isTrialOffer) 3 else null
                        )
                    )
                }

                val response = withContext(Dispatchers.IO) {
                    api.sync(
                        EntitlementSyncRequest(
                            purchases = listOf(
                                PurchaseTokenPayload(
                                    productId = sub.productId,
                                    purchaseToken = sub.purchaseToken
                                )
                            )
                        )
                    )
                }

                val premiumStatus = response.premiumStatus.uppercase()

                val openedEntitlement =
                    response.status.equals("ACTIVE", ignoreCase = true) &&
                            (premiumStatus == "PREMIUM" || premiumStatus == "TRIAL") &&
                            !response.entitlementType.isNullOrBlank() &&
                            response.currentPremiumUntil != null

                if (!openedEntitlement) {
                    return PurchaseEntitlementResult(
                        success = false,
                        response = response,
                        message = "Payment verified, but entitlement was not activated. premiumStatus=${response.premiumStatus}, status=${response.status}"
                    )
                }

                if (!sub.acknowledged) {
                    val acknowledged = acknowledgeWithRetry(sub.purchaseToken)
                    if (!acknowledged) {
                        Log.w(TAG, "acknowledge failed after retry. purchaseToken=${sub.purchaseToken.take(8)}***")
                        // 不阻斷使用者；syncAfterLoginSilently 會在下次啟動/登入補重試。
                    }
                }

                PurchaseEntitlementResult(
                    success = true,
                    response = response
                )
            }
        }
    }

    private suspend fun acknowledgeWithRetry(
        purchaseToken: String,
        maxAttempts: Int = 3
    ): Boolean {
        repeat(maxAttempts) { index ->
            val ok = withContext(Dispatchers.IO) {
                runCatching { billing.acknowledgePurchase(purchaseToken) }
                    .getOrDefault(false)
            }

            if (ok) {
                return true
            }

            val nextDelayMs = when (index) {
                0 -> 600L
                1 -> 1_500L
                else -> 3_000L
            }

            Log.w(TAG, "acknowledge attempt ${index + 1}/$maxAttempts failed, retryAfterMs=$nextDelayMs")
            delay(nextDelayMs)
        }

        return false
    }

    private companion object {
        const val TAG = "EntitlementSyncer"
    }
}
