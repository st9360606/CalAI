package com.calai.bitecal.data.entitlement

import android.app.Activity
import android.util.Log
import com.calai.bitecal.data.billing.BillingGateway
import com.calai.bitecal.data.billing.BillingPurchaseResult
import com.calai.bitecal.data.entitlement.api.EntitlementApi
import com.calai.bitecal.data.entitlement.api.EntitlementSyncRequest
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import com.calai.bitecal.data.entitlement.api.PurchaseTokenPayload
import com.calai.bitecal.data.entitlement.model.PremiumStatus
import com.calai.bitecal.data.membership.api.MembershipApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
data class PurchaseEntitlementResult(
    val success: Boolean,
    val response: EntitlementSyncResponse? = null,
    val message: String? = null,
    val restoreRequired: Boolean = false
)

class EntitlementSyncer(
    private val billing: BillingGateway,
    private val api: EntitlementApi,
    private val membershipApi: MembershipApi,
) {
    suspend fun syncAfterLoginSilently() = withContext(Dispatchers.IO) {
        runCatching {
            refreshServerEntitlementSummaryOnly()
        }.onFailure {
            Log.w(TAG, "syncAfterLoginSilently failed: ${it.message}")
        }
        Unit
    }

    suspend fun hasActiveSubscriptionOnDevice(): Boolean = withContext(Dispatchers.IO) {
        runCatching { billing.queryActiveSubscriptions().isNotEmpty() }
            .onFailure { Log.w(TAG, "hasActiveSubscriptionOnDevice failed: ${it.message}") }
            .getOrDefault(false)
    }

    suspend fun restoreSubscription(): RestoreSubscriptionResult = withContext(Dispatchers.IO) {
        val subs = runCatching { billing.queryActiveSubscriptions() }
            .onFailure { Log.w(TAG, "restore queryActiveSubscriptions failed: ${it.message}") }
            .getOrElse { return@withContext RestoreSubscriptionResult.Failed(it.message) }

        if (subs.isEmpty()) {
            refreshMembershipSummarySilently()
            return@withContext RestoreSubscriptionResult.NoActivePurchase
        }

        val payload = subs.map {
            PurchaseTokenPayload(
                productId = it.productId,
                purchaseToken = it.purchaseToken
            )
        }

        val syncResponse = runCatching {
            api.sync(EntitlementSyncRequest(purchases = payload))
        }.getOrElse { ex ->
            Log.w(TAG, "restore entitlement sync failed: ${ex.message}")
            val message = ex.message.orEmpty()
            return@withContext if (
                message.contains("PURCHASE_TOKEN_ALREADY_BOUND", ignoreCase = true) ||
                message.contains("409")
            ) {
                RestoreSubscriptionResult.BoundToAnotherAccount
            } else {
                RestoreSubscriptionResult.Failed(ex.message)
            }
        }

        subs.filter { !it.acknowledged }
            .forEach { acknowledgeWithRetry(it.purchaseToken) }

        val summary = runCatching { membershipApi.me() }
            .onFailure { Log.w(TAG, "restore membership refresh failed: ${it.message}") }
            .getOrElse { return@withContext RestoreSubscriptionResult.Failed(it.message) }

        val summaryStatus = PremiumStatus.from(summary.premiumStatus)
        if (summaryStatus == PremiumStatus.PREMIUM || summaryStatus == PremiumStatus.TRIAL) {
            return@withContext RestoreSubscriptionResult.Restored(summary)
        }

        val syncStatus = PremiumStatus.from(syncResponse.premiumStatus)
        if (syncStatus == PremiumStatus.PREMIUM || syncStatus == PremiumStatus.TRIAL) {
            return@withContext RestoreSubscriptionResult.Failed("Membership refresh did not reflect restored entitlement.")
        }

        RestoreSubscriptionResult.NoActivePurchase
    }


    suspend fun refreshServerEntitlementSummaryOnly(): EntitlementSyncResponse = withContext(Dispatchers.IO) {
        val response = api.me()
        refreshMembershipSummarySilently()
        response
    }

    /**
     * Explicit purchase/restore path.
     *
     * This method may call Google Play queryPurchasesAsync() and /entitlements/sync, so it must
     * not be used by passive premium gates. Account-deletion restore must only happen after the
     * user explicitly taps a restore action.
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

            refreshMembershipSummarySilently()

            response
        } else {
            val response = api.me()
            refreshMembershipSummarySilently()
            response
        }
    }

    private suspend fun refreshMembershipSummarySilently() {
        runCatching { membershipApi.me() }
            .onFailure { Log.w(TAG, "membership summary refresh failed: ${it.message}") }
    }


    suspend fun hasServerPremiumAccess(): Boolean {
        return runCatching {
            val response = refreshServerEntitlementSummaryOnly()
            response.status.equals("ACTIVE", ignoreCase = true) &&
                    PremiumStatus.from(response.premiumStatus).let {
                        it == PremiumStatus.TRIAL || it == PremiumStatus.PREMIUM
                    }
        }.onFailure {
            Log.w(TAG, "hasServerPremiumAccess failed: ${it.message}")
        }.getOrDefault(false)
    }

    /**
     * Checks whether the current BiteCal account already has server-side Premium/Trial access.
     *
     * Important: this method must not call Google Play queryPurchasesAsync() or /entitlements/sync.
     * Account-deletion restore must only happen after the user explicitly taps Restore Subscription.
     */
    suspend fun hasActivePremiumAccess(): Boolean {
        return hasServerPremiumAccess()
    }

    suspend fun purchaseSubscriptionAndSync(
        activity: Activity,
        productId: String,
        offerTag: String? = null
    ): PurchaseEntitlementResult {
        val existingActiveSubscriptions = runCatching {
            withContext(Dispatchers.IO) {
                billing.queryActiveSubscriptions()
            }
        }.onFailure { ex ->
            Log.w(TAG, "pre-purchase queryActiveSubscriptions failed: ${ex.message}")
        }.getOrDefault(emptyList())

        if (existingActiveSubscriptions.isNotEmpty()) {
            return PurchaseEntitlementResult(
                success = false,
                message = PURCHASE_ALREADY_OWNED_RESTORE_REQUIRED,
                restoreRequired = true
            )
        }

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

            BillingPurchaseResult.AlreadyOwned -> {
                PurchaseEntitlementResult(
                    success = false,
                    message = PURCHASE_ALREADY_OWNED_RESTORE_REQUIRED,
                    restoreRequired = true
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

                val response = runCatching {
                    withContext(Dispatchers.IO) {
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
                }.getOrElse { ex ->
                    Log.w(TAG, "purchase entitlement sync failed: ${ex.message}")

                    return PurchaseEntitlementResult(
                        success = false,
                        message = "Purchase completed, but entitlement sync failed. Please retry.",
                        restoreRequired = true
                    )
                }

                if (!isOpenedEntitlement(response)) {
                    return PurchaseEntitlementResult(
                        success = false,
                        response = response,
                        message = "Payment verified, but entitlement was not activated. premiumStatus=${response.premiumStatus}, status=${response.status}"
                    )
                }

                if (!sub.acknowledged) {
                    val acknowledged = acknowledgeWithRetry(sub.purchaseToken)
                    if (!acknowledged) {
                        Log.w(
                            TAG,
                            "acknowledge failed after retry. purchaseToken=${sub.purchaseToken.take(16)}***"
                        )
                    }
                }

                PurchaseEntitlementResult(
                    success = true,
                    response = response
                )
            }
        }
    }

    private fun isOpenedEntitlement(response: EntitlementSyncResponse?): Boolean {
        if (response == null) return false

        val premiumStatus = response.premiumStatus.uppercase(Locale.US)

        return response.status.equals("ACTIVE", ignoreCase = true) &&
                (premiumStatus == "PREMIUM" || premiumStatus == "TRIAL") &&
                !response.entitlementType.isNullOrBlank() &&
                response.currentPremiumUntil != null
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

    companion object {
        private const val TAG = "EntitlementSyncer"
        const val PURCHASE_ALREADY_OWNED_RESTORE_REQUIRED = "PURCHASE_ALREADY_OWNED_RESTORE_REQUIRED"
    }
}
