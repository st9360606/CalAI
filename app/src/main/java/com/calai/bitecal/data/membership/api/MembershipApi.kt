package com.calai.bitecal.data.membership.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface MembershipApi {
    @GET("/api/v1/membership/me")
    suspend fun me(): MembershipSummaryDto

    @GET("/api/v1/membership/rewards")
    suspend fun rewards(): List<RewardHistoryItemDto>
}

@Serializable
data class MembershipSummaryDto(
    val premiumStatus: String,
    val currentPremiumUntil: String? = null,
    val trialEndsAt: String? = null,
    val trialDaysLeft: Int? = null,
    val paymentIssue: Boolean = false,
    val latestRewardSource: String? = null,
    val latestOldPremiumUntil: String? = null,
    val latestNewPremiumUntil: String? = null,
    val latestGrantedAtUtc: String? = null
)

@Serializable
data class RewardHistoryItemDto(
    val id: Long,
    val sourceType: String,
    val sourceRefId: Long,
    val daysAdded: Int,
    val oldPremiumUntil: String? = null,
    val newPremiumUntil: String? = null,
    val grantedAtUtc: String? = null
)
