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

    // 先只刷新後端既有權益摘要，避免未經使用者確認就把 Google Play token 轉移到新帳號。
    runCatching { entitlementSyncer.refreshServerEntitlementSummaryOnly() }
        .onFailure { Log.w(TAG, "refresh entitlement before bootstrap failed: ${it.message}") }

    return runCatching {
        onboardingRepository.bootstrap().toBiteCalRoute()
    }.onFailure {
        Log.w(TAG, "onboarding bootstrap failed, fallback to legacy entitlement gate: ${it.message}")
    }.getOrElse {
        if (entitlementSyncer.hasServerPremiumAccess()) Routes.HOME else Routes.ONBOARD_SUBSCRIPTION
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
