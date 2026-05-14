package com.calai.bitecal.data.onboarding.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface OnboardingApi {
    @GET("/api/v1/onboarding/bootstrap")
    suspend fun bootstrap(): OnboardingBootstrapDto
}

@Serializable
data class OnboardingBootstrapDto(
    val premiumStatus: String = "FREE",
    val paymentIssue: Boolean = false,
    val paymentRecoveryRequired: Boolean = false,
    val hasPaidSubscriptionHistory: Boolean = false,
    val trialActive: Boolean = false,
    val trialEligible: Boolean = false,
    val referralClaimEligible: Boolean = false,
    val hasReferralClaim: Boolean = false,
    val referralClaimStatus: String? = null,
    val referralClaimIneligibleReason: String? = null,
    val nextRecommendedRoute: String? = null,
)
