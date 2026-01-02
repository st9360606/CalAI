package com.calai.app.data.activity.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

@Serializable
data class DailyActivityUpsertRequest(
    @SerialName("localDate")
    val localDate: String,              // YYYY-MM-DD

    @SerialName("timezone")
    val timezone: String,               // IANA

    @SerialName("steps")
    val steps: Long?,                   // nullable

    @SerialName("activeKcal")
    val activeKcal: Double?,            // nullable

    @SerialName("ingestSource")
    val ingestSource: String = "HEALTH_CONNECT",

    @SerialName("dataOriginPackage")
    val dataOriginPackage: String?,     // HC 建議必填

    @SerialName("dataOriginName")
    val dataOriginName: String? = null
)

@Serializable
data class DailyActivityItemDto(
    val localDate: String,
    val timezone: String,
    val steps: Long?,
    val activeKcal: Double?,
    val ingestSource: String,
    val dataOriginPackage: String?,
    val dataOriginName: String?
)

interface DailyActivityApi {
    @PUT("/api/v1/users/me/daily-activity")
    suspend fun upsert(@Body req: DailyActivityUpsertRequest)

    @GET("/api/v1/users/me/daily-activity")
    suspend fun getRange(
        @Query("from") from: String,
        @Query("to") to: String
    ): List<DailyActivityItemDto>
}
