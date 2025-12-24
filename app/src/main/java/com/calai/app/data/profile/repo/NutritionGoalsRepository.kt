package com.calai.app.data.profile.repo

import com.calai.app.data.profile.api.NutritionGoalsManualRequest
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.api.UserProfileDto
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionGoalsRepository @Inject constructor(
    private val profileApi: ProfileApi
) {
    suspend fun fetchProfileOrNull(): UserProfileDto? = try {
        profileApi.getMyProfile()
    } catch (e: HttpException) {
        when (e.code()) { 401, 404 -> null else -> throw e }
    } catch (e: IOException) {
        null
    }

    suspend fun setManualGoalsAndRefresh(req: NutritionGoalsManualRequest): UserProfileDto {
        profileApi.setManualNutritionGoals(req)
        return profileApi.getMyProfile()
    }
}
