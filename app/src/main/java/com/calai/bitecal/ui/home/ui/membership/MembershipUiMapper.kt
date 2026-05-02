package com.calai.bitecal.ui.home.ui.membership

import com.calai.bitecal.data.entitlement.model.PremiumStatus

data class MembershipDisplay(
    val title: String,
    val subtitle: String = ""
)

object MembershipUiMapper {

    fun map(
        status: PremiumStatus,
        currentPremiumUntil: String? = null,
        trialDaysLeft: Int? = null,
        paymentIssue: Boolean = false
    ): MembershipDisplay {
        if (paymentIssue && status == PremiumStatus.PREMIUM) {
            return MembershipDisplay(
                title = "Payment Issue",
                subtitle = "Update payment"
            )
        }

        return when (status) {
            PremiumStatus.FREE -> MembershipDisplay(
                title = "FREE",
                subtitle = "Upgrade"
            )

            PremiumStatus.TRIAL -> MembershipDisplay(
                title = "TRIAL",
                subtitle = "${trialDaysLeft ?: 0} days left"
            )

            PremiumStatus.PREMIUM -> MembershipDisplay(
                title = "PREMIUM",
                subtitle = "Until ${formatDate(currentPremiumUntil)}"
            )
        }
    }

    fun formatDate(raw: String?): String {
        return raw
            ?.takeIf { it.isNotBlank() }
            ?.take(10)
            ?: "—"
    }
}
