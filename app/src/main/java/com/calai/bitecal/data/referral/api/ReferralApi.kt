package com.calai.bitecal.data.referral.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ReferralApi {
    @GET("/api/v1/referrals/me")
    suspend fun me(): ReferralSummaryDto

    @POST("/api/v1/referrals/claim")
    suspend fun claim(@Body request: ClaimReferralRequest)
}

@Serializable
data class ClaimReferralRequest(
    val promoCode: String
)

@Serializable
data class ReferralSummaryDto(
    val promoCode: String,
    val successCount: Long,
    val pendingVerificationCount: Long,
    val rejectedCount: Long,
    val recentClaims: List<ReferralClaimItemDto> = emptyList()
)

@Serializable
data class ReferralClaimItemDto(
    val claimId: Long,
    val displayName: String,
    val status: String,
    val verificationDeadlineUtc: String? = null,
    val rewardedAtUtc: String? = null,
    val rejectReason: String? = null
)
