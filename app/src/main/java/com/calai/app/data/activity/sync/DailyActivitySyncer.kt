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

    private fun choosePreferredOrigin(
        byOrigin: Map<String, Long>,
        preferred: List<String>
    ): String? {
        //改成：先挑偏好中 >0 的；都沒有 >0 才退回 0 / max
        if (byOrigin.isEmpty()) return null

        fun stepsOf(pkg: String) = byOrigin[pkg]

        // 1) 先依偏好找：Google Fit > Samsung Health（但必須 >0）
        for (pkg in preferred) {
            if (pkg == DataOriginPrefs.ON_DEVICE_ANDROID) continue
            val v = stepsOf(pkg)
            if (v != null && v > 0L) return pkg
        }

        // 2) 允許任何來源：選 steps 最大（可能是 0）
        if (preferred.contains(DataOriginPrefs.ON_DEVICE_ANDROID)) {
            return byOrigin.maxByOrNull { it.value }?.key
        }

        // 3) 不允許 any-source：那就挑偏好存在的（即使 0），最後才 null
        for (pkg in preferred) {
            if (pkg == DataOriginPrefs.ON_DEVICE_ANDROID) continue
            if (byOrigin.containsKey(pkg)) return pkg
        }

        return null
    }


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
                // ✅ 直接拿當天所有來源 steps
                val byOrigin = try {
                    reader.readStepsByOrigin(d, nowZone)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    Log.e("HC_SYNC", "readStepsByOrigin failed date=$d err=${t.javaClass.simpleName}:${t.message}")
                    emptyMap()
                }

                // ✅ 依偏好挑來源：Fit > Samsung > 其他(steps 最大)
                val chosen = choosePreferredOrigin(byOrigin, DataOriginPrefs.preferred)
                Log.e("HC_SYNC", "date=$d origins=${byOrigin.size} chosen=$chosen")

                val steps = chosen?.let { byOrigin[it] }
                // ✅ 只擋 null；0 也要留下來
                if (chosen == null || steps == null) {
                    continue
                }

                if (d == today) {
                    // 需要就印出 debug
                    (reader as? HealthConnectDailyReader)?.debugDumpStepsOrigins(d, nowZone)
                }

                val originName = reader.resolveOriginName(chosen)

                // ✅ 只送 steps；activeKcal 一律送 null（server 回填）
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
                    activeKcal = null,
                    dataOriginPackage = chosen,
                    dataOriginName = originName
                )
            }

            // ✅ 撈回 server 的 activeKcal 回填
            val from = days.first().format(df)
            val to = days.last().format(df)

            val serverRows = runCatching { api.getRange(from = from, to = to) }
                .getOrElse {
                    Log.e("HC_SYNC", "getRange for merge failed (ok): ${it.javaClass.simpleName}:${it.message}")
                    emptyList()
                }

            val kcalByDate = serverRows.associate { dto ->
                val date = LocalDate.parse(dto.localDate)
                date to dto.activeKcal?.roundToInt()
            }

            val merged = out.map { day -> day.copy(activeKcal = kcalByDate[day.localDate]) }

            Result.success(DailyActivitySyncResult(status = status, days = merged))
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e("HC_SYNC", "sync failed err=${t.javaClass.simpleName}:${t.message}")
            Result.failure(t)
        }
    }
}
