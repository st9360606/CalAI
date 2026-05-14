package com.calai.bitecal.data.referral.repo

import com.calai.bitecal.data.referral.api.ClaimReferralRequest
import com.calai.bitecal.data.referral.api.ClaimReferralResponse
import com.calai.bitecal.data.referral.api.ReferralApi
import com.calai.bitecal.data.referral.api.ReferralSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferralRepository @Inject constructor(
    private val api: ReferralApi
) {
    suspend fun getSummary(): ReferralSummaryDto = api.me()
    suspend fun claim(promoCode: String): ClaimReferralResponse = api.claim(ClaimReferralRequest(promoCode.trim()))
}
