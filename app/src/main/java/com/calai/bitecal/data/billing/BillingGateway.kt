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
     */
    suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String
    ): BillingPurchaseResult

    /**
     * 後端驗證成功並開通 Premium 後，再 acknowledge。
     */
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean
}
