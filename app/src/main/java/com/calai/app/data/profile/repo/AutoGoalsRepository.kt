package com.calai.app.data.profile.repo

import com.calai.app.data.profile.api.AutoGoalsApi
import com.calai.app.data.profile.api.AutoGoalsCommitRequest
import com.calai.app.data.profile.api.UserProfileDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoGoalsRepository @Inject constructor(
    private val api: AutoGoalsApi,
    private val store: UserProfileStore
) {
    suspend fun commitFromLocal(): UserProfileDto {
        val snap = store.snapshot()
        return api.commit(
            AutoGoalsCommitRequest(
                workoutsPerWeek = snap.exerciseFreqPerWeek,
                heightCm = snap.heightCm?.toDouble(),
                heightFeet = snap.heightFeet,
                heightInches = snap.heightInches,
                weightKg = snap.weightKg?.toDouble(),
                weightLbs = snap.weightLbs?.toDouble(),
                goalKey = snap.goal
            )
        )
    }
}
