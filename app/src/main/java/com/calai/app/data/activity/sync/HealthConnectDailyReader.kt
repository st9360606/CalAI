package com.calai.app.data.activity.sync

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.calai.app.data.activity.model.DailyActivityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Singleton
class HealthConnectDailyReader @Inject constructor(
    @ApplicationContext context: Context
) : DailyReader {

    private val ctx: Context = context.applicationContext
    private val client by lazy { HealthConnectClient.getOrCreate(ctx) }

    // ✅ 一進 sync 就會被呼叫：一定印得出來
    suspend fun debugDumpEnv() {
        val sdk = HealthConnectClient.getSdkStatus(ctx)
        val granted = client.permissionController.getGrantedPermissions()
        Log.e("HC_ENV", "sdkStatus=$sdk grantedPerms=${granted.size}")
        granted.forEach { Log.e("HC_ENV", "granted=$it") }
    }

    override suspend fun getStatus(): DailyActivityStatus {
        // ✅ 不依賴「不存在的常數」：只要不是 AVAILABLE，就分流
        return when (HealthConnectClient.getSdkStatus(ctx)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val granted = client.permissionController.getGrantedPermissions()
                val needSteps = HealthPermission.getReadPermission(StepsRecord::class)
                // ✅ 先只要求 Steps（ActiveKcal 允許缺，UI 顯示 —）
                if (granted.contains(needSteps)) DailyActivityStatus.AVAILABLE_GRANTED
                else DailyActivityStatus.PERMISSION_NOT_GRANTED
            }
            HealthConnectClient.SDK_UNAVAILABLE -> DailyActivityStatus.HC_UNAVAILABLE
            // UPDATE_REQUIRED / NOT_INSTALLED / 其它未知狀態 → 都當未安裝/需更新（你 UI 都導去安裝頁即可）
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

        // steps 有任一筆就算有
        val stepsAny = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = tr,
                dataOriginFilter = originFilter,
                pageSize = 1
            )
        ).records.isNotEmpty()

        if (stepsAny) return true

        // active kcal 有任一筆也算有（但可能沒權限，所以要 try/catch）
        val kcalAny = try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = tr,
                    dataOriginFilter = originFilter,
                    pageSize = 1
                )
            ).records.isNotEmpty()
        } catch (t: Throwable) {
            Log.e("HC_SYNC", "hasAnyRecord kcal check failed (ok): ${t.javaClass.simpleName}:${t.message}")
            false
        }
        return kcalAny
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

        // StepsRecord.count 是 Long
        return records.sumOf { it.count }
    }

    override suspend fun readActiveKcal(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Int? {
        return try {
            val tr = dayRange(localDate, zoneId)
            val originFilter = originFilter(originPackage)
            val records = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = tr,
                    dataOriginFilter = originFilter
                )
            ).records

            if (records.isEmpty()) return null
            records.sumOf { it.energy.inKilocalories }.roundToInt()
        } catch (t: Throwable) {
            Log.e("HC_SYNC", "readActiveKcal failed (ok -> return null): ${t.javaClass.simpleName}:${t.message}")
            null
        }
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

    /**
     * ✅ 你的 ON_DEVICE_ANDROID 是「fallback」，不要當成 packageName 去 filter
     * - 若是 ON_DEVICE_ANDROID：回 emptySet() 代表「不限制來源」→ 能讀到任何來源（含系統/廠商/其它 app）
     * - 否則：只讀指定 package 的來源
     */
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
