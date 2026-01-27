package com.calai.bitecal.data.water.repo

import com.calai.bitecal.data.water.api.AdjustRequest
import com.calai.bitecal.data.water.api.WaterApi
import com.calai.bitecal.data.water.api.WaterSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val api: WaterApi
) {
    suspend fun loadToday(): WaterSummaryDto = api.today()

    suspend fun adjustCups(delta: Int): WaterSummaryDto {
        // 修正點：使用具名參數把 AdjustRequest 指定給 req，
        // 讓 tz 使用預設值 ZoneId.systemDefault().id
        return api.increment(
            req = AdjustRequest(cupsDelta = delta)
        )
    }
}
