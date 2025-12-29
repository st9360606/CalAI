package com.calai.app.data.profile.repo

import com.calai.app.data.profile.api.NutritionGoalsManualRequest
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.api.UserProfileDto
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionGoalsRepository @Inject constructor(
    private val profileApi: ProfileApi
) {
    private companion object {
        const val TIMEOUT_MS = 10_000L
    }

    suspend fun fetchProfileOrNull(): UserProfileDto? = try {
        withTimeout(TIMEOUT_MS) { profileApi.getMyProfile() }
    } catch (e: TimeoutCancellationException) {
        // 讓 VM 顯示「Unable to load profile...」
        null
    } catch (e: HttpException) {
        when (e.code()) { 401, 404 -> null else -> throw e }
    } catch (e: IOException) {
        null
    }

    suspend fun setManualGoalsAndRefresh(req: NutritionGoalsManualRequest): UserProfileDto {
        withTimeout(TIMEOUT_MS) { profileApi.setManualNutritionGoals(req) }
        return withTimeout(TIMEOUT_MS) { profileApi.getMyProfile() }
    }
}
