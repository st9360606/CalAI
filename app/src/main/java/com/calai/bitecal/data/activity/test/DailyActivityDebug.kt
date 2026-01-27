package com.calai.bitecal.data.activity.test

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.calai.bitecal.data.activity.model.DailyActivityStatus
import com.calai.bitecal.data.activity.sync.DataOriginPrefs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DailyActivityDebug {

    private const val TAG_ENV = "HC_ENV"
    private const val TAG_SYNC = "HC_SYNC"
    private const val TAG_ORIGIN = "HC_ORIGIN"
    private const val TAG_PICK = "HC_PICK"
    private const val TAG_SERVER = "HC_SERVER"

    private inline fun ifEnabled(block: () -> Unit) {
        if (DailyActivityDebugConfig.enabled) block()
    }

    fun logSyncEnter(zoneId: ZoneId) = ifEnabled {
        Log.i(TAG_SYNC, "syncLast7Days enter zone=${zoneId.id}")
    }

    fun logStatus(status: DailyActivityStatus) = ifEnabled {
        Log.i(TAG_SYNC, "reader.getStatus() = $status")
    }

    fun logEnv(context: Context, sdkStatus: Int, grantedPerms: Set<String>) = ifEnabled {
        val needSteps = HealthPermission.getReadPermission(StepsRecord::class)
        Log.i(TAG_ENV, "sdkStatus=$sdkStatus (SDK_AVAILABLE=${HealthConnectClient.SDK_AVAILABLE})")
        Log.i(TAG_ENV, "needPermission=$needSteps grantedCount=${grantedPerms.size}")
        grantedPerms.forEach { Log.i(TAG_ENV, "granted=$it") }

        val pm = context.packageManager
        logInstalled(pm, DataOriginPrefs.GOOGLE_FIT, "Google Fit")
        logInstalled(pm, DataOriginPrefs.SAMSUNG_HEALTH, "Samsung Health")
    }

    private fun logInstalled(pm: PackageManager, pkg: String, label: String) {
        val installed = try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (_: Throwable) {
            false
        }
        Log.i(TAG_ENV, "sourceAppInstalled $label pkg=$pkg installed=$installed")
    }

    fun logDayHeader(
        localDate: LocalDate,
        zoneId: ZoneId,
        rangeStart: Instant,
        rangeEnd: Instant,
        totalRecords: Int
    ) = ifEnabled {
        Log.i(TAG_ORIGIN, "date=$localDate zone=${zoneId.id} range=[$rangeStart .. $rangeEnd) records=$totalRecords")
    }

    fun logOriginRow(
        pkg: String,
        originName: String?,
        steps: Long,
        recordCount: Int,
        firstStart: Instant?,
        lastEnd: Instant?
    ) = ifEnabled {
        Log.i(
            TAG_ORIGIN,
            "origin pkg=$pkg name=${originName ?: "(unknown)"} steps=$steps records=$recordCount " +
                    "firstStart=${firstStart ?: "-"} lastEnd=${lastEnd ?: "-"}"
        )
    }

    fun logPickDecision(
        date: LocalDate,
        preferred: List<String>,
        byOrigin: Map<String, Long>,
        chosen: String?,
        chosenSteps: Long?
    ) = ifEnabled {
        Log.i(TAG_PICK, "date=$date preferred=$preferred byOrigin=$byOrigin")
        Log.i(TAG_PICK, "date=$date chosen=${chosen ?: "null"} steps=${chosenSteps ?: "null"}")
    }

    fun logUpsertOk(date: LocalDate, originPkg: String, steps: Long) = ifEnabled {
        Log.i(TAG_SYNC, "upsert OK date=$date origin=$originPkg steps=$steps")
    }

    fun logUpsertFail(date: LocalDate, msg: String) = ifEnabled {
        Log.w(TAG_SYNC, "upsert FAIL date=$date $msg")
    }

    fun logServerMerge(from: String, to: String, rowCount: Int) = ifEnabled {
        Log.i(TAG_SERVER, "getRange from=$from to=$to rows=$rowCount")
    }

    fun logServerRow(date: String, activeKcal: Int?) = ifEnabled {
        Log.i(TAG_SERVER, "server date=$date activeKcal=${activeKcal ?: "null"}")
    }
}
