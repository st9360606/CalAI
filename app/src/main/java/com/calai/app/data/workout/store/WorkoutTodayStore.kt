package com.calai.app.data.workout.store

import com.calai.app.data.workout.api.TodayWorkoutResponse
import com.calai.app.data.workout.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 全局的「今天運動狀態」，讓 HomeViewModel 跟 WorkoutTracker 共用。
 *
 * - WorkoutViewModel 在存檔後呼叫 refresh() 或 setFromServer()
 * - HomeViewModel 讀這個 flow 來更新首頁卡片 (activeKcal)
 *
 * 「首頁卡片」目前顯示 ${s.todayActivity.activeKcal} kcal / ${s.todayActivity.exerciseMinutes} min:contentReference[oaicite:8]{index=8}
 * 我們將用 totalKcalToday 取代 activeKcal，把 minutes 之後也一起從後端回傳。
 */
@Singleton
class WorkoutTodayStore @Inject constructor(
    private val repo: WorkoutRepository
) {
    private val _today = MutableStateFlow<TodayWorkoutResponse?>(null)
    val today: StateFlow<TodayWorkoutResponse?> = _today.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _today.value = repo.loadToday()
    }

    suspend fun setFromServer(resp: TodayWorkoutResponse) = withContext(Dispatchers.IO) {
        _today.value = resp
    }
}
