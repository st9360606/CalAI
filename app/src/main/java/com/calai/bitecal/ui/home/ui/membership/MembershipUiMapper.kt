package com.calai.bitecal.ui.home.ui.membership

import com.calai.bitecal.data.entitlement.model.PremiumStatus

data class MembershipDisplay(
    val title: String,
    val subtitle: String = ""
)

object MembershipUiMapper {

    fun map(
        status: PremiumStatus,
        paymentIssue: Boolean = false
    ): MembershipDisplay {
        if (paymentIssue && status == PremiumStatus.PREMIUM) {
            return MembershipDisplay(title = "Payment Issue")
        }

        return when (status) {
            PremiumStatus.FREE -> MembershipDisplay(title = "FREE")
            PremiumStatus.TRIAL -> MembershipDisplay(title = "TRIAL")
            PremiumStatus.PREMIUM -> MembershipDisplay(title = "PREMIUM")
        }
    }

    fun formatDate(raw: String?): String {
        return raw
            ?.takeIf { it.isNotBlank() }
            ?.take(10)
            ?: "—"
    }
}
