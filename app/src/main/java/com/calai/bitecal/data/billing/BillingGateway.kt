// BillingGateway.kt
package com.calai.bitecal.data.billing

data class ActiveSub(
    val productId: String,
    val purchaseToken: String
)

interface BillingGateway {
    /** 回傳目前「已購買且有效」的訂閱（可能 0..n） */
    suspend fun queryActiveSubscriptions(): List<ActiveSub>
}
