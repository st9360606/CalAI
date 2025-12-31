package com.calai.app.data.fasting.repo

import com.calai.app.data.fasting.api.FastingApi
import com.calai.app.data.fasting.api.FastingPlanDto
import com.calai.app.data.fasting.api.NextTriggersResp
import com.calai.app.data.fasting.api.UpsertFastingPlanReq
import com.calai.app.data.fasting.model.FastingPlan
import retrofit2.HttpException
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FastingRepository(
    private val api: FastingApi,
    private val zoneIdProvider: () -> ZoneId
) {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * 只讀：不存在才回 null（404），其他 HTTP 錯誤直接丟出
     */
    suspend fun getMineOrNull(): FastingPlanDto? {
        val resp = api.getMine()
        if (resp.isSuccessful) return resp.body()
        if (resp.code() == 404) return null
        throw HttpException(resp)
    }

    /**
     * 只在「真的不存在(404)」才建立預設
     * ✅ 不要在 5xx 時寫回預設，避免覆蓋用戶設定
     */
    suspend fun ensureDefaultIfMissing(): FastingPlanDto {
        val exist = getMineOrNull()
        if (exist != null) return exist

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
