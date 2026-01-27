package com.calai.bitecal.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class TodayActivity(
    val steps: Long,
    val activeKcal: Double,   // kcal
    val exerciseMinutes: Long // 分鐘
)

@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    // ✅ 正確的權限 API
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    // 讀取今天的步數/主動熱量/運動時長（用 readRecords 自行加總）
    suspend fun readToday(): TodayActivity = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val range = TimeRangeFilter.between(start, end)

        val stepsResp = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = range
            )
        )
        val steps = stepsResp.records.sumOf { it.count }

        val calResp = client.readRecords(
            ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = range
            )
        )
        val kcal = calResp.records.sumOf { it.energy.inKilocalories }

        val sesResp = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = range
            )
        )
        val minutes = sesResp.records.sumOf { r ->
            (r.endTime.toEpochMilli() - r.startTime.toEpochMilli()) / 60000L
        }

        TodayActivity(steps = steps, activeKcal = kcal, exerciseMinutes = minutes)
    }
}

