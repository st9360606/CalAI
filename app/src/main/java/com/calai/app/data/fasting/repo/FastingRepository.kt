package com.calai.app.data.fasting.repo


import com.calai.app.data.fasting.api.FastingApi
import com.calai.app.data.fasting.api.FastingPlanDto
import com.calai.app.data.fasting.api.NextTriggersResp
import com.calai.app.data.fasting.api.UpsertFastingPlanReq
import com.calai.app.data.fasting.model.FastingPlan
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import retrofit2.HttpException

class FastingRepository(
    private val api: FastingApi,
    private val zoneIdProvider: () -> ZoneId
) {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun loadOrCreateDefault(): FastingPlanDto {
        val resp = api.getMine()
        if (resp.isSuccessful) return requireNotNull(resp.body())

        // ★ 404 與 5xx 都 fallback 到 upsert 預設（避免首次 500 造成崩潰）
        if (resp.code() == 404 || resp.code() in 500..599) {
            val tz = zoneIdProvider().id
            return api.upsert(
                UpsertFastingPlanReq(
                    planCode = "16:8",
                    startTime = "09:00",
                    enabled = false,
                    timeZone = tz
                )
            )
        }
        throw HttpException(resp)
    }

    suspend fun save(planCode: String, start: LocalTime, enabled: Boolean): FastingPlanDto {
        val tz = zoneIdProvider().id
        return api.upsert(
            UpsertFastingPlanReq(
                planCode = planCode,
                startTime = start.format(fmt),
                enabled = enabled,
                timeZone = tz
            )
        )
    }

    suspend fun nextTriggers(plan: FastingPlan, start: LocalTime): NextTriggersResp {
        val tz = zoneIdProvider().id
        return api.nextTriggers(
            planCode = plan.code,
            startTime = start.format(fmt),
            timeZone = tz
        )
    }


}

