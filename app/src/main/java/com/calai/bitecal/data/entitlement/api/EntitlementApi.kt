package com.calai.bitecal.data.entitlement.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EntitlementApi {

    @POST("/api/v1/entitlements/sync")
    suspend fun sync(@Body req: EntitlementSyncRequest): EntitlementSyncResponse

    @GET("/api/v1/entitlements/me")
    suspend fun me(): EntitlementSyncResponse
}

@Serializable
data class EntitlementSyncRequest(
    val purchases: List<PurchaseTokenPayload>
)

@Serializable
data class PurchaseTokenPayload(
    val productId: String,
    val purchaseToken: String
)

@Serializable
data class EntitlementSyncResponse(
    val status: String,
    val entitlementType: String? = null,
    val premiumStatus: String = "FREE",
    val currentPremiumUntil: String? = null,
    val trialEndsAt: String? = null,
    val trialDaysLeft: Int? = null,
    val paymentIssue: Boolean = false
)
