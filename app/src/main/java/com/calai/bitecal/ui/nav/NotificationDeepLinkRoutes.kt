package com.calai.bitecal.ui.nav

object NotificationDeepLinkRoutes {
    private const val PREMIUM_REWARDS_DEEP_LINK = "bitecal://premium-rewards"
    private const val REFERRALS_DEEP_LINK = "bitecal://referrals"

    fun routeFor(deepLink: String?): String? {
        return when (deepLink?.trim()) {
            PREMIUM_REWARDS_DEEP_LINK -> Routes.PREMIUM_REWARDS
            REFERRALS_DEEP_LINK -> Routes.REFERRALS
            else -> null
        }
    }
}
