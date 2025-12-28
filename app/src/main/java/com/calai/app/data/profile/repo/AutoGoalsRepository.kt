package com.calai.app.data.profile.repo

import com.calai.app.data.common.RepoInvalidationBus
import com.calai.app.data.profile.api.AutoGoalsApi
import com.calai.app.data.profile.api.AutoGoalsCommitRequest
import com.calai.app.data.profile.api.UserProfileDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoGoalsRepository @Inject constructor(
    private val api: AutoGoalsApi,
    private val store: UserProfileStore,
    private val bus: RepoInvalidationBus
) {
    suspend fun commitFromLocal(): UserProfileDto {
        val snap = store.snapshot()

        // ✅ commit 成功後，會影響：
        // - Profile（Height/Weight/Goal...）
        // - Weight timeseries current（你說 CurrentWeight 讀 timeseries）
        val resp = api.commit(
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

        // ✅ 寫入成功 -> 強制通知 UI 端 refresh（不會被節流擋住）
        bus.invalidateProfile()
        bus.invalidateWeight()

        return resp
    }
}
