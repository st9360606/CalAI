package com.calai.bitecal.ui.nav

object NotificationDeepLinkRoutes {
    private const val PREMIUM_REWARDS_DEEP_LINK = "bitecal://premium-rewards"
    private const val REFERRALS_DEEP_LINK = "bitecal://referrals"

    fun routeFor(deepLink: String?): String? {
        return when (deepLink?.trim()) {
            REFERRALS_DEEP_LINK -> Routes.REFERRALS

            // Backward compatibility:
            // Older backend notifications may still contain bitecal://premium-rewards.
            // Since the current app entry users can actually find is Settings > Invite friends,
            // route old premium reward links to the referral screen as well.
            PREMIUM_REWARDS_DEEP_LINK -> Routes.REFERRALS

            else -> null
        }
    }
}