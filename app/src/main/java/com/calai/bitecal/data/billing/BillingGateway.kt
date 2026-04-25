package com.calai.bitecal.data.billing

import android.app.Activity

data class ActiveSub(
    val productId: String,
    val purchaseToken: String,
    val acknowledged: Boolean = false
)

sealed interface BillingPurchaseResult {
    data class Success(val sub: ActiveSub) : BillingPurchaseResult
    data object Cancelled : BillingPurchaseResult
    data object Pending : BillingPurchaseResult
    data class Error(val message: String) : BillingPurchaseResult
}

interface BillingGateway {

    /**
     * App 啟動 / 登入後 restore entitlement 用。
     */
    suspend fun queryActiveSubscriptions(): List<ActiveSub>

    /**
     * 訂閱頁點擊方案後，啟動 Google Play Billing Sheet。
     *
     * @param productId Google Play subscription product id.
     * @param offerTag 若指定，會優先使用符合 offerTag 的 subscription offer。
     *                 若 null，會 fallback 到第一個可用 offer。
     */
    suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        offerTag: String? = null
    ): BillingPurchaseResult

    /**
     * 後端驗證成功並開通 Premium 後，再 acknowledge。
     */
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean
}
