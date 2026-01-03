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
import kotlin.math.roundToInt

data class DailyActivityDayResult(
    val localDate: LocalDate,
    val timezone: String,
    val steps: Long?,
    val activeKcal: Int?,              // ✅ 由 server 回填（後端用體重+steps計算）
    val dataOriginPackage: String?,
    val dataOriginName: String?
)

data class DailyActivitySyncResult(
    val status: DailyActivityStatus,
    val days: List<DailyActivityDayResult>
)

@ViewModelScoped
class DailyActivitySyncer @Inject constructor(
    private val api: DailyActivityApi,
    private val reader: DailyReader
) {
    private val df = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * ✅ 新版：
     * 1) 只同步 steps（activeKcal = null，後端會用 weight_timeseries 最新體重 + steps 計算）
     * 2) 同步完後，GET range 撈回 server 計算結果，回填 activeKcal（讓 UI 立即顯示）
     */
    suspend fun syncLast7DaysWithStatus(nowZone: ZoneId): Result<DailyActivitySyncResult> {
        Log.e("HC_SYNC", "syncLast7Days enter zone=${nowZone.id}")
        (reader as? HealthConnectDailyReader)?.debugDumpEnv()

        val status = reader.getStatus()
        Log.e("HC_SYNC", "reader.getStatus() = $status")

        if (status != DailyActivityStatus.AVAILABLE_GRANTED) {
            return Result.success(DailyActivitySyncResult(status = status, days = emptyList()))
        }

        return try {
            val today = LocalDate.now(nowZone)
            val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

            val out = mutableListOf<DailyActivityDayResult>()

            for (d in days) {
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
                val originName = reader.resolveOriginName(chosen)

                Log.e("HC_SYNC", "date=$d steps=$steps originName=$originName")

                // ✅ 只送 steps；activeKcal 一律送 null（避免 client 亂算/權限依賴）
                api.upsert(
                    DailyActivityUpsertRequest(
                        localDate = d.format(df),
                        timezone = nowZone.id,
                        steps = steps,
                        activeKcal = null,
                        ingestSource = "HEALTH_CONNECT",
                        dataOriginPackage = chosen,
                        dataOriginName = originName
                    )
                )

                out += DailyActivityDayResult(
                    localDate = d,
                    timezone = nowZone.id,
                    steps = steps,
                    activeKcal = null, // 先空，等一下從 server 回填
                    dataOriginPackage = chosen,
                    dataOriginName = originName
                )
            }

            // ✅ 一次性撈回 server 計算後的 activeKcal（避免每一天打一個 GET）
            val from = days.first().format(df)
            val to = days.last().format(df)

            val serverRows = runCatching { api.getRange(from = from, to = to) }
                .getOrElse { t ->
                    Log.e("HC_SYNC", "getRange for merge failed (ok): ${t.javaClass.simpleName}:${t.message}")
                    emptyList()
                }

            val kcalByDate: Map<LocalDate, Int?> = serverRows.associate { dto ->
                val date = LocalDate.parse(dto.localDate)
                val kcalInt = dto.activeKcal?.roundToInt()
                date to kcalInt
            }

            val merged = out.map { day ->
                day.copy(activeKcal = kcalByDate[day.localDate])
            }

            Result.success(DailyActivitySyncResult(status = status, days = merged))
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e("HC_SYNC", "sync failed err=${t.javaClass.simpleName}:${t.message}")
            Result.failure(t)
        }
    }

    /** ✅ 舊版保留：如果你其他地方還在用，不會壞 */
    suspend fun syncLast7Days(nowZone: ZoneId): Result<List<DailyActivityDayResult>> {
        return syncLast7DaysWithStatus(nowZone).map { it.days }
    }
}
