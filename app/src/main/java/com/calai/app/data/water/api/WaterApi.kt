package com.calai.app.data.water.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 後端回傳今天喝水的摘要
 * - date: 依使用者所在時區算出的 yyyy-MM-dd
 * - cups: 幾杯
 * - ml: 總毫升
 * - flOz: 總美制液量盎司 (fluid ounce)
 */
@Serializable
data class WaterSummaryDto(
    val date: String,
    val cups: Int,
    val ml: Int,
    val flOz: Int
)

/**
 * 用來告訴後端「+1杯」或「-1杯」
 * cupsDelta = 1 代表 +1 杯
 * cupsDelta = -1 代表 -1 杯（後端自己會 clamp 到 >=0）
 */
@Serializable
data class AdjustRequest(
    val cupsDelta: Int
)

interface WaterApi {

    @GET("/water/today")
    suspend fun today(): WaterSummaryDto

    @POST("/water/increment")
    suspend fun increment(@Body req: AdjustRequest): WaterSummaryDto
}
