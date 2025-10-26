package com.calai.app.data.water.repo

import com.calai.app.data.water.api.AdjustRequest
import com.calai.app.data.water.api.WaterApi
import com.calai.app.data.water.api.WaterSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val api: WaterApi
) {
    suspend fun loadToday(): WaterSummaryDto = api.today()

    suspend fun adjustCups(delta: Int): WaterSummaryDto {
        return api.increment(AdjustRequest(cupsDelta = delta))
    }
}
