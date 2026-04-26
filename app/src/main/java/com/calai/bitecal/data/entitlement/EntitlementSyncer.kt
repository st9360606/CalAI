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
            val subs = billing.queryActiveSubscriptions()
            if (subs.isEmpty()) return@runCatching

            val payload = subs.map {
                PurchaseTokenPayload(
                    productId = it.productId,
                    purchaseToken = it.purchaseToken
                )
            }

            api.sync(EntitlementSyncRequest(purchases = payload))

            // Acknowledge 也放在 restore path：如果前一次 acknowledge 因網路失敗，登入/啟動後還能補償重試。
            subs.filter { !it.acknowledged }
                .forEach { acknowledgeWithRetry(it.purchaseToken) }
        }.onFailure {
            Log.w(TAG, "syncAfterLoginSilently failed: ${it.message}")
        }
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
