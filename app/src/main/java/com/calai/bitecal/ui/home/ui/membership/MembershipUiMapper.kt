package com.calai.bitecal.ui.home.ui.membership

import com.calai.bitecal.data.entitlement.model.PremiumStatus
import com.calai.bitecal.core.time.UtcTimeFormatter

enum class MembershipDisplayKind {
    FREE,
    TRIAL,
    PREMIUM,
    PAYMENT_ISSUE
}

data class MembershipDisplay(
    val kind: MembershipDisplayKind,
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
                kind = MembershipDisplayKind.PAYMENT_ISSUE,
                title = "Payment Issue",
                subtitle = "Update payment"
            )
        }

        return when (status) {
            PremiumStatus.FREE -> MembershipDisplay(
                kind = MembershipDisplayKind.FREE,
                title = "FREE",
                subtitle = "Upgrade"
            )

            PremiumStatus.TRIAL -> MembershipDisplay(
                kind = MembershipDisplayKind.TRIAL,
                title = "TRIAL",
                subtitle = formatTrialSubtitle(trialDaysLeft)
            )

            PremiumStatus.PREMIUM -> MembershipDisplay(
                kind = MembershipDisplayKind.PREMIUM,
                title = "PREMIUM",
                subtitle = formatPremiumSubtitle(currentPremiumUntil)
            )
        }
    }

    private fun formatPremiumSubtitle(currentPremiumUntil: String?): String {
        val date = formatDateOrNull(currentPremiumUntil)

        return if (date != null) {
            "Until $date"
        } else {
            "Active member"
        }
    }

    private fun formatTrialSubtitle(daysLeft: Int?): String {
        val safeDays = (daysLeft ?: 0).coerceAtLeast(0)

        return when (safeDays) {
            0 -> "Trial ends today"
            1 -> "1 day left"
            else -> "$safeDays days left"
        }
    }

    fun formatDateOrNull(raw: String?): String? =
        UtcTimeFormatter.formatUtcDateOrNull(raw)
}
