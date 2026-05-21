// app/src/main/java/com/calai/app/data/account/api/AccountApi.kt
package com.calai.bitecal.data.account.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AccountApi {

    @POST("/api/v1/account/deletion-request")
    suspend fun requestDeletion(
        @Body request: AccountDeletionRequest = AccountDeletionRequest()
    ): AccountDeletionResponse

    @GET("/api/v1/account/deletion-request")
    suspend fun deletionStatus(): AccountDeletionStatusResponse

    @GET("/api/v1/account/deletion-preview")
    suspend fun deletionPreview(): AccountDeletionPreviewResponse
}

@Serializable
data class AccountDeletionRequest(
    val subscriptionWarningAcknowledged: Boolean = false,
    val userRequestedGooglePlayCancel: Boolean = false
)

@Serializable
data class AccountDeletionResponse(
    val ok: Boolean,
    val status: String = "REQUESTED",
    val requestedAtUtc: String? = null
)

@Serializable
data class AccountDeletionStatusResponse(
    val status: String,
    val requestedAtUtc: String? = null
)

@Serializable
data class AccountDeletionPreviewResponse(
    val canDelete: Boolean = true,
    val hasActiveGooglePlaySubscription: Boolean = false,
    val premiumStatus: String = "FREE",
    val entitlementType: String? = null,
    val currentPremiumUntil: String? = null,
    val subscriptionManagementUrl: String = "https://play.google.com/store/account/subscriptions",
    val requiresSubscriptionWarning: Boolean = false
)
