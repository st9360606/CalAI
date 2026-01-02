package com.calai.app.data.activity.sync

import android.util.Log
import com.calai.app.data.activity.api.DailyActivityApi
import com.calai.app.data.activity.api.DailyActivityUpsertRequest
import com.calai.app.data.activity.model.DailyActivityStatus
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DailyActivityDayResult(
    val localDate: LocalDate,
    val timezone: String,
    val steps: Long?,
    val activeKcal: Int?,
    val dataOriginPackage: String?,
    val dataOriginName: String?
)

data class DailyActivitySyncResult(
    val status: DailyActivityStatus,
    val days: List<DailyActivityDayResult>
)

interface DailyReader {
    suspend fun getStatus(): DailyActivityStatus
    suspend fun hasAnyRecord(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Boolean
    suspend fun readSteps(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Long?
    suspend fun readActiveKcal(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Int?
    suspend fun resolveOriginName(packageName: String): String?
}

@ViewModelScoped
class DailyActivitySyncer @Inject constructor(
    private val api: DailyActivityApi,
    private val reader: DailyReader
) {
    private val df = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * ✅ 新版：回傳 status + days（避免空 list 被誤判 NO_DATA）
     */
    suspend fun syncLast7DaysWithStatus(nowZone: ZoneId): Result<DailyActivitySyncResult> {
        // ✅ 一進來就印（不會再「找不到 log」）
        Log.e("HC_SYNC", "syncLast7Days enter zone=${nowZone.id}")

        // ✅ 如果是 HealthConnectDailyReader，先 dump 環境（sdk status / granted perms）
        (reader as? HealthConnectDailyReader)?.debugDumpEnv()

        val status = reader.getStatus()
        Log.e("HC_SYNC", "reader.getStatus() = $status")

        // ✅ 不可用/未授權：直接回 status（但仍有 log）
        if (status != DailyActivityStatus.AVAILABLE_GRANTED) {
            return Result.success(DailyActivitySyncResult(status = status, days = emptyList()))
        }

        return try {
            val today = LocalDate.now(nowZone)
            val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

            val out = mutableListOf<DailyActivityDayResult>()

            for (d in days) {
                // ✅ 只針對今天：dump origins（一定會執行到這裡，因為 status 已是 GRANTED）
                if (d == today) {
                    (reader as? HealthConnectDailyReader)?.debugDumpStepsOrigins(d, nowZone)
                }

                val chosen = pickFirstExistingOrigin(DataOriginPrefs.preferred) { pkg ->
                    try {
                        reader.hasAnyRecord(d, nowZone, pkg)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (t: Throwable) {
                        Log.e("HC_SYNC", "hasAnyRecord failed pkg=$pkg date=$d err=${t.javaClass.simpleName}:${t.message}")
                        false
                    }
                }

                Log.e("HC_SYNC", "date=$d chosenOrigin=$chosen")

                if (chosen == null) continue

                val steps = reader.readSteps(d, nowZone, chosen)
                val kcal = reader.readActiveKcal(d, nowZone, chosen)
                val originName = reader.resolveOriginName(chosen)

                Log.e("HC_SYNC", "date=$d steps=$steps kcal=$kcal originName=$originName")

                api.upsert(
                    DailyActivityUpsertRequest(
                        localDate = d.format(df),
                        timezone = nowZone.id,
                        steps = steps,
                        activeKcal = kcal?.toDouble(),
                        ingestSource = "HEALTH_CONNECT",
                        dataOriginPackage = chosen,
                        dataOriginName = originName
                    )
                )

                out += DailyActivityDayResult(
                    localDate = d,
                    timezone = nowZone.id,
                    steps = steps,
                    activeKcal = kcal,
                    dataOriginPackage = chosen,
                    dataOriginName = originName
                )
            }

            Result.success(DailyActivitySyncResult(status = status, days = out))
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e("HC_SYNC", "sync failed err=${t.javaClass.simpleName}:${t.message}")
            Result.failure(t)
        }
    }

    /**
     * ✅ 舊版保留：如果你其他地方還在用，不會壞
     */
    suspend fun syncLast7Days(nowZone: ZoneId): Result<List<DailyActivityDayResult>> {
        return syncLast7DaysWithStatus(nowZone).map { it.days }
    }
}
