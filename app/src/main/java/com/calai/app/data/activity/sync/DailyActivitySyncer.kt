package com.calai.app.data.activity.sync

import android.util.Log
import com.calai.app.data.activity.api.DailyActivityApi
import com.calai.app.data.activity.api.DailyActivityUpsertRequest
import com.calai.app.data.activity.model.DailyActivityStatus
import com.calai.app.data.activity.test.DailyActivityDebug
import com.calai.app.data.activity.test.DailyActivityDebugConfig
import com.calai.app.data.activity.test.OriginPicker
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt
import retrofit2.HttpException

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
        DailyActivityDebug.logSyncEnter(nowZone)

        // ✅ 環境/權限：一次印清楚
        (reader as? HealthConnectDailyReader)?.debugDumpEnvDetailed()

        val status = reader.getStatus()
        DailyActivityDebug.logStatus(status)

        if (status != DailyActivityStatus.AVAILABLE_GRANTED) {
            return Result.success(DailyActivitySyncResult(status = status, days = emptyList()))
        }

        return try {
            val today = LocalDate.now(nowZone)
            val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

            val out = mutableListOf<DailyActivityDayResult>()
            var anyUpsertSucceeded = false

            for (d in days) {
                val byOrigin = runCatching { reader.readStepsByOrigin(d, nowZone) }
                    .getOrElse {
                        Log.e("HC_SYNC", "readStepsByOrigin failed date=$d err=${it.javaClass.simpleName}:${it.message}")
                        emptyMap()
                    }

                // ✅ 直接印：當天各來源的詳細（records/time-range）
                if (DailyActivityDebugConfig.enabled) {
                    (reader as? HealthConnectDailyReader)?.debugDumpStepsOriginsDetailed(d, nowZone)
                }

                val chosen = OriginPicker.choosePreferredOrigin(byOrigin, DataOriginPrefs.preferred)
                val steps = chosen?.let { byOrigin[it] }

                DailyActivityDebug.logPickDecision(
                    date = d,
                    preferred = DataOriginPrefs.preferred,
                    byOrigin = byOrigin,
                    chosen = chosen,
                    chosenSteps = steps
                )

                if (chosen == null || steps == null) continue

                val originName = reader.resolveOriginName(chosen)

                // 先把本機讀到的結果放進 out（不依賴後端成功）
                out += DailyActivityDayResult(
                    localDate = d,
                    timezone = nowZone.id,
                    steps = steps,
                    activeKcal = null,
                    dataOriginPackage = chosen,
                    dataOriginName = originName
                )

                // 後端 upsert：best-effort
                try {
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
                    anyUpsertSucceeded = true
                    DailyActivityDebug.logUpsertOk(d, chosen, steps)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (he: HttpException) {
                    DailyActivityDebug.logUpsertFail(d, "code=${he.code()} msg=${he.message()}")
                } catch (t: Throwable) {
                    DailyActivityDebug.logUpsertFail(d, "err=${t.javaClass.simpleName}:${t.message}")
                }
            }

            // 只有至少一次 upsert 成功才拉 server merge
            if (!anyUpsertSucceeded) {
                return Result.success(DailyActivitySyncResult(status = status, days = out))
            }

            val from = days.first().format(df)
            val to = days.last().format(df)

            val serverRows = runCatching { api.getRange(from = from, to = to) }
                .getOrElse {
                    Log.e("HC_SYNC", "getRange failed (ok): ${it.javaClass.simpleName}:${it.message}")
                    emptyList()
                }

            DailyActivityDebug.logServerMerge(from, to, serverRows.size)
            serverRows.forEach { dto ->
                DailyActivityDebug.logServerRow(dto.localDate, dto.activeKcal?.roundToInt())
            }

            val kcalByDate = serverRows.associate { dto ->
                LocalDate.parse(dto.localDate) to dto.activeKcal?.roundToInt()
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
