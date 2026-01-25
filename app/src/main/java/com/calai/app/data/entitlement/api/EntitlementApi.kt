package com.calai.app.data.entitlement.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface EntitlementApi {

    @POST("/api/v1/entitlements/sync")
    suspend fun sync(@Body req: EntitlementSyncRequest): EntitlementSyncResponse
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
    val status: String,           // ACTIVE / INACTIVE
    val entitlementType: String? = null // MONTHLY / YEARLY（可選）
)
