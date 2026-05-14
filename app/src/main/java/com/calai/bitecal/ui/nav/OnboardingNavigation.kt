package com.calai.bitecal.ui.nav

import android.util.Log
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.onboarding.api.OnboardingBootstrapDto
import com.calai.bitecal.data.onboarding.repo.OnboardingRepository
import com.calai.bitecal.data.onboarding.repo.OnboardingRouteDecider

private const val TAG = "OnboardingNav"

suspend fun resolveOnboardingDestination(
    entitlementSyncer: EntitlementSyncer,
    onboardingRepository: OnboardingRepository,
    allowHomeAfterRejectedPaywall: Boolean,
): String {
    if (allowHomeAfterRejectedPaywall) return Routes.HOME

    // 先 restore/sync Google Play 權益，再讓後端 bootstrap 用最新 entitlement 做導頁決策。
    runCatching { entitlementSyncer.refreshEntitlementSummary() }
        .onFailure { Log.w(TAG, "refresh entitlement before bootstrap failed: ${it.message}") }

    return runCatching {
        onboardingRepository.bootstrap().toBiteCalRoute()
    }.onFailure {
        Log.w(TAG, "onboarding bootstrap failed, fallback to legacy entitlement gate: ${it.message}")
    }.getOrElse {
        if (entitlementSyncer.hasActivePremiumAccess()) Routes.HOME else Routes.ONBOARD_SUBSCRIPTION
    }
}

private fun OnboardingBootstrapDto.toBiteCalRoute(): String {
    return when (OnboardingRouteDecider.decideBackendRoute(this)) {
        OnboardingRouteDecider.BACKEND_HOME -> Routes.HOME
        OnboardingRouteDecider.BACKEND_ONBOARD_REFERRAL_CODE -> Routes.ONBOARD_REFERRAL_CODE
        OnboardingRouteDecider.BACKEND_SUBSCRIPTION -> Routes.ONBOARD_SUBSCRIPTION
        OnboardingRouteDecider.BACKEND_ONBOARD_SUBSCRIPTION -> Routes.ONBOARD_SUBSCRIPTION
        else -> Routes.ONBOARD_SUBSCRIPTION
    }
}
