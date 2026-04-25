package com.calai.bitecal.data.entitlement

import android.app.Activity
import com.calai.bitecal.data.billing.BillingGateway
import com.calai.bitecal.data.billing.BillingPurchaseResult
import com.calai.bitecal.data.entitlement.api.EntitlementApi
import com.calai.bitecal.data.entitlement.api.EntitlementSyncRequest
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import com.calai.bitecal.data.entitlement.api.PurchaseTokenPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PurchaseEntitlementResult(
    val success: Boolean,
    val response: EntitlementSyncResponse? = null,
    val message: String? = null
)

class EntitlementSyncer(
    private val billing: BillingGateway,
    private val api: EntitlementApi,
) {
    /**
     * 登入後 restore 用。
     * 不阻塞登入流程，失敗只吞掉。
     */
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
        }
    }

    /**
     * onboarding 完成後發 3 天 Trial。
     *
     * 注意：
     * - 後端會判斷是否 eligible
     * - 已付費、已領過 Trial、device 不合規，都應該由後端拒絕
     * - APP 這邊吞錯，不阻塞登入 / onboarding 完成流程
     */
    suspend fun grantTrialIfEligibleSilently() = withContext(Dispatchers.IO) {
        runCatching {
            api.grantTrial()
        }
    }

    /**
     * 訂閱頁付款成功後，立刻同步後端 entitlement。
     *
     * 正確順序：
     * 1. 啟動 Google Play Billing Sheet
     * 2. 收到 purchaseToken
     * 3. 呼叫 backend /api/v1/entitlements/sync
     * 4. backend 回 PREMIUM 後才 acknowledge
     * 5. UI refresh membership
     */
    suspend fun purchaseSubscriptionAndSync(
        activity: Activity,
        productId: String
    ): PurchaseEntitlementResult {
        val purchaseResult = billing.launchSubscriptionPurchase(
            activity = activity,
            productId = productId
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

                val openedPremium =
                    response.status == "ACTIVE" &&
                            response.premiumStatus == "PREMIUM" &&
                            !response.entitlementType.isNullOrBlank() &&
                            response.currentPremiumUntil != null

                if (!openedPremium) {
                    return PurchaseEntitlementResult(
                        success = false,
                        response = response,
                        message = "Payment verified, but Premium was not activated. Please try restoring purchases."
                    )
                }

                if (!sub.acknowledged) {
                    withContext(Dispatchers.IO) {
                        billing.acknowledgePurchase(sub.purchaseToken)
                    }
                }

                PurchaseEntitlementResult(
                    success = true,
                    response = response
                )
            }
        }
    }
}
