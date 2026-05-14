package com.calai.bitecal.data.onboarding.repo

import com.calai.bitecal.data.onboarding.api.OnboardingApi
import com.calai.bitecal.data.onboarding.api.OnboardingBootstrapDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    private val api: OnboardingApi,
) {
    suspend fun bootstrap(): OnboardingBootstrapDto {
        return api.bootstrap()
    }
}