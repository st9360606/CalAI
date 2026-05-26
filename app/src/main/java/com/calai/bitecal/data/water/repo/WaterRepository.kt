package com.calai.bitecal.data.water.repo

import com.calai.bitecal.data.water.api.AdjustRequest
import com.calai.bitecal.data.water.api.WaterApi
import com.calai.bitecal.data.water.api.WaterSummaryDto
import com.calai.bitecal.data.water.api.WaterWeeklyChartDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val api: WaterApi
) {
    suspend fun loadToday(): WaterSummaryDto = api.today()

    // X-Client-Timezone 由 BaseHeadersInterceptor 統一帶出
    suspend fun adjustCups(delta: Int): WaterSummaryDto {
        return api.increment(
            req = AdjustRequest(cupsDelta = delta)
        )
    }

    suspend fun loadWeeklyChart(weekOffset: Int = 0): WaterWeeklyChartDto {
        return api.weekly(weekOffset = weekOffset.coerceIn(0, MAX_PROGRESS_WEEK_OFFSET))
    }

    private companion object {
        const val MAX_PROGRESS_WEEK_OFFSET = 5
    }
}
