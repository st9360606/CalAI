package com.calai.app.data.activity.sync

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.calai.app.data.activity.model.DailyActivityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
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

    suspend fun debugDumpEnv() {
        val sdk = HealthConnectClient.getSdkStatus(ctx)
        val granted = client.permissionController.getGrantedPermissions()
        Log.e("HC_ENV", "sdkStatus=$sdk grantedPerms=${granted.size}")
        granted.forEach { Log.e("HC_ENV", "granted=$it") }
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

    override suspend fun hasAnyRecord(
        localDate: LocalDate,
        zoneId: ZoneId,
        originPackage: String
    ): Boolean {
        val tr = dayRange(localDate, zoneId)
        val originFilter = originFilter(originPackage)

        // ✅ 只看 steps：有任一筆就算有
        val stepsAny = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = tr,
                dataOriginFilter = originFilter,
                pageSize = 1
            )
        ).records.isNotEmpty()

        return stepsAny
    }

    override suspend fun readSteps(
        localDate: LocalDate,
        zoneId: ZoneId,
        originPackage: String
    ): Long? {
        val tr = dayRange(localDate, zoneId)
        val originFilter = originFilter(originPackage)

        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = tr,
                dataOriginFilter = originFilter
            )
        ).records

        if (records.isEmpty()) return null
        return records.sumOf { it.count }
    }

    override suspend fun resolveOriginName(packageName: String): String? {
        if (packageName == DataOriginPrefs.ON_DEVICE_ANDROID) return "On device"
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

    private fun originFilter(originPackage: String): Set<DataOrigin> {
        return if (originPackage == DataOriginPrefs.ON_DEVICE_ANDROID) {
            emptySet()
        } else {
            setOf(DataOrigin(packageName = originPackage))
        }
    }

    suspend fun debugDumpStepsOrigins(localDate: LocalDate, zoneId: ZoneId) {
        val tr = dayRange(localDate, zoneId)

        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = tr
            )
        ).records

        val byOrigin = records
            .groupBy { it.metadata.dataOrigin.packageName }
            .mapValues { (_, list) -> list.sumOf { it.count } }
            .toList()
            .sortedByDescending { it.second }

        Log.e("HC_ORIGIN", "date=$localDate totalRecords=${records.size} originCount=${byOrigin.size}")
        byOrigin.forEach { (pkg, steps) ->
            Log.e("HC_ORIGIN", "origin=$pkg steps=$steps")
        }
    }
}
