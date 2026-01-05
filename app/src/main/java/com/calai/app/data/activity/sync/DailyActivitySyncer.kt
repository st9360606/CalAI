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
            var anyUpsertSucceeded = false

            for (d in days) {
                val byOrigin = runCatching { reader.readStepsByOrigin(d, nowZone) }
                    .getOrElse {
                        Log.e("HC_SYNC", "readStepsByOrigin failed date=$d err=${it.javaClass.simpleName}:${it.message}")
                        emptyMap()
                    }

                val chosen = choosePreferredOrigin(byOrigin, DataOriginPrefs.preferred)
                val steps = chosen?.let { byOrigin[it] }

                if (chosen == null || steps == null) continue

                val originName = reader.resolveOriginName(chosen)

                // ✅ 先把「本機讀到的結果」放進 out（不依賴後端成功）
                out += DailyActivityDayResult(
                    localDate = d,
                    timezone = nowZone.id,
                    steps = steps,
                    activeKcal = null,              // 先 null，後面再 merge server
                    dataOriginPackage = chosen,
                    dataOriginName = originName
                )

                // ✅ 後端 upsert：失敗不要讓整包炸掉（best-effort）
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
                } catch (ce: CancellationException) {
                    throw ce
                } catch (he: HttpException) {
                    Log.e("HC_SYNC", "upsert failed date=$d code=${he.code()} msg=${he.message()}")
                    // 不中斷：continue
                } catch (t: Throwable) {
                    Log.e("HC_SYNC", "upsert failed date=$d err=${t.javaClass.simpleName}:${t.message}")
                    // 不中斷：continue
                }
            }

            // ✅ 只有「至少有一次 upsert 成功」才去拉 server merge kcal（避免永遠 500）
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
