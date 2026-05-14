package com.calai.bitecal.data.onboarding.repo

import com.calai.bitecal.data.onboarding.api.OnboardingBootstrapDto

object OnboardingRouteDecider {
    const val BACKEND_HOME = "HOME"
    const val BACKEND_ONBOARD_REFERRAL_CODE = "ONBOARD_REFERRAL_CODE"
    const val BACKEND_ONBOARD_SUBSCRIPTION = "ONBOARD_SUBSCRIPTION"
    const val BACKEND_SUBSCRIPTION = "SUBSCRIPTION"

    fun decideBackendRoute(response: OnboardingBootstrapDto): String {
        val status = response.premiumStatus.uppercase()

        if (status == "PREMIUM" || status == "TRIAL") return BACKEND_HOME
        if (response.paymentIssue) return BACKEND_HOME
        if (response.paymentRecoveryRequired || response.hasPaidSubscriptionHistory) return BACKEND_SUBSCRIPTION
        if (response.referralClaimEligible) return BACKEND_ONBOARD_REFERRAL_CODE
        return response.nextRecommendedRoute ?: BACKEND_ONBOARD_SUBSCRIPTION
    }
}
