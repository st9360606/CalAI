package com.calai.bitecal.data.activity.sync

import android.content.Context
import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.calai.bitecal.data.activity.model.DailyActivityStatus
import com.calai.bitecal.data.activity.test.DailyActivityDebug
import com.calai.bitecal.data.activity.test.DailyActivityDebugConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectDailyReader @Inject constructor(
    @ApplicationContext context: Context
) : DailyReader {

    private val ctx: Context = context.applicationContext
    private val client by lazy { HealthConnectClient.getOrCreate(ctx) }

    suspend fun debugDumpEnvDetailed() {
        if (!DailyActivityDebugConfig.enabled) return
        val sdk = HealthConnectClient.getSdkStatus(ctx)
        val granted = client.permissionController.getGrantedPermissions()
        DailyActivityDebug.logEnv(ctx, sdk, granted)
    }

    override suspend fun getStatus(): DailyActivityStatus {
        return when (HealthConnectClient.getSdkStatus(ctx)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val granted = client.permissionController.getGrantedPermissions()
                val needSteps = HealthPermission.getReadPermission(StepsRecord::class)
                if (granted.contains(needSteps)) DailyActivityStatus.AVAILABLE_GRANTED
                else DailyActivityStatus.PERMISSION_NOT_GRANTED
            }
            HealthConnectClient.SDK_UNAVAILABLE -> DailyActivityStatus.HC_UNAVAILABLE
            else -> DailyActivityStatus.HC_NOT_INSTALLED
        }
    }

    override suspend fun readStepsByOrigin(localDate: LocalDate, zoneId: ZoneId): Map<String, Long> {
        val tr = dayRange(localDate, zoneId)
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = tr
            )
        ).records

        if (records.isEmpty()) return emptyMap()

        return records
            .groupBy { it.metadata.dataOrigin.packageName }
            .mapValues { (_, list) -> list.sumOf { it.count } }
    }

    /**
     * ✅ Debug：印出當天「各來源的 steps / records / time range」
     * 你要看來源到底拿了什麼，這個最關鍵。
     */
    suspend fun debugDumpStepsOriginsDetailed(localDate: LocalDate, zoneId: ZoneId) {
        if (!DailyActivityDebugConfig.enabled) return
        val tr = dayRange(localDate, zoneId)
        val start = localDate.atStartOfDay(zoneId).toInstant()
        val end = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = tr
            )
        ).records

        DailyActivityDebug.logDayHeader(
            localDate = localDate,
            zoneId = zoneId,
            rangeStart = start,
            rangeEnd = end,
            totalRecords = records.size
        )

        if (records.isEmpty()) return

        val grouped = records.groupBy { it.metadata.dataOrigin.packageName }
        val rows = grouped.map { (pkg, list) ->
            val steps = list.sumOf { it.count }
            val firstStart = list.minOfOrNull { it.startTime }
            val lastEnd = list.maxOfOrNull { it.endTime }
            OriginRow(
                pkg = pkg,
                name = resolveOriginName(pkg),
                steps = steps,
                recordCount = list.size,
                firstStart = firstStart,
                lastEnd = lastEnd
            )
        }.sortedByDescending { it.steps }

        rows.forEach { r ->
            DailyActivityDebug.logOriginRow(
                pkg = r.pkg,
                originName = r.name,
                steps = r.steps,
                recordCount = r.recordCount,
                firstStart = r.firstStart,
                lastEnd = r.lastEnd
            )
        }
    }

    private data class OriginRow(
        val pkg: String,
        val name: String?,
        val steps: Long,
        val recordCount: Int,
        val firstStart: Instant?,
        val lastEnd: Instant?
    )

    override suspend fun hasAnyRecord(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Boolean {
        val map = readStepsByOrigin(localDate, zoneId)
        return map.containsKey(originPackage) // 0 也算
    }

    override suspend fun readSteps(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Long? {
        val map = readStepsByOrigin(localDate, zoneId)
        return map[originPackage]
    }

    override suspend fun resolveOriginName(packageName: String): String? {
        return try {
            val pm = ctx.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun dayRange(localDate: LocalDate, zoneId: ZoneId): TimeRangeFilter {
        val start = localDate.atStartOfDay(zoneId).toInstant()
        val end = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()
        return TimeRangeFilter.between(start, end)
    }
}
