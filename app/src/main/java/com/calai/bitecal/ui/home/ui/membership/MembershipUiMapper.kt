package com.calai.bitecal.ui.home.ui.membership

import com.calai.bitecal.data.entitlement.model.PremiumStatus

data class MembershipDisplay(
    val title: String,
    val subtitle: String
)

object MembershipUiMapper {

    fun map(
        status: PremiumStatus,
        until: String?,
        trialDaysLeft: Int?
    ): MembershipDisplay {

        return when (status) {
            PremiumStatus.FREE -> {
                MembershipDisplay(
                    title = "FREE",
                    subtitle = "Upgrade to unlock"
                )
            }

            PremiumStatus.TRIAL -> {
                MembershipDisplay(
                    title = "TRIAL",
                    subtitle = "${trialDaysLeft ?: "-"} days left"
                )
            }

            PremiumStatus.PREMIUM -> {
                MembershipDisplay(
                    title = "PREMIUM",
                    subtitle = "Until ${until ?: "—"}"
                )
            }
        }
    }
}
