package com.calai.bitecal.data.membership.repo

import com.calai.bitecal.data.membership.api.MembershipApi
import com.calai.bitecal.data.membership.api.MembershipSummaryDto
import com.calai.bitecal.data.membership.api.RewardHistoryItemDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipRepository @Inject constructor(
    private val api: MembershipApi
) {
    suspend fun getSummary(): MembershipSummaryDto = api.me()
    suspend fun getRewardHistory(): List<RewardHistoryItemDto> = api.rewards()
}
