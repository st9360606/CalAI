package com.calai.bitecal.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationDeepLinkRoutesTest {

    @Test
    fun referralsDeepLink_mapsToReferralsRoute() {
        assertEquals(
            Routes.REFERRALS,
            NotificationDeepLinkRoutes.routeFor("bitecal://referrals")
        )
    }

    @Test
    fun premiumRewardsDeepLink_mapsToReferralsRouteForBackwardCompatibility() {
        assertEquals(
            Routes.REFERRALS,
            NotificationDeepLinkRoutes.routeFor("bitecal://premium-rewards")
        )
    }

    @Test
    fun deepLinkWithExtraSpaces_isTrimmedBeforeMapping() {
        assertEquals(
            Routes.REFERRALS,
            NotificationDeepLinkRoutes.routeFor("  bitecal://referrals  ")
        )
    }

    @Test
    fun unknownDeepLink_returnsNull() {
        assertNull(NotificationDeepLinkRoutes.routeFor("bitecal://unknown"))
    }

    @Test
    fun nullDeepLink_returnsNull() {
        assertNull(NotificationDeepLinkRoutes.routeFor(null))
    }
}