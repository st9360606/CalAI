// app/src/main/java/com/calai/app/data/fasting/notifications/RescheduleWorker.kt
package com.calai.app.data.fasting.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calai.app.data.fasting.model.FastingPlan
import com.calai.app.data.fasting.repo.FastingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalTime

@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: FastingRepository,
    private val scheduler: FastingAlarmScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dto = repo.loadOrCreateDefault()
        if (!dto.enabled) return Result.success()

        val plan = FastingPlan.of(dto.planCode)
        val start = LocalTime.parse(dto.startTime)

        val tr = repo.nextTriggers(plan, start) // 後端回傳 UTC（DST 安全）
        scheduler.schedule(
            Instant.parse(tr.nextStartUtc),
            Instant.parse(tr.nextEndUtc)
        )
        return Result.success()
    }
}
