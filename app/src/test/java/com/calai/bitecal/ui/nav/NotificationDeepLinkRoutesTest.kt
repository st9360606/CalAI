package com.calai.bitecal.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationDeepLinkRoutesTest {

    @Test
    fun premiumRewardsDeepLink_mapsToPremiumRewardsRoute() {
        assertEquals(
            Routes.PREMIUM_REWARDS,
            NotificationDeepLinkRoutes.routeFor("bitecal://premium-rewards")
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
