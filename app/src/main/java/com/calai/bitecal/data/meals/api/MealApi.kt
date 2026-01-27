package com.calai.bitecal.data.meals.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApi {
    @GET("/api/v1/diary/recent")
    suspend fun recentMeals(@Query("limit") limit: Int = 10): List<MealItemDto>
}

@Serializable
data class MealItemDto(
    val id: Long,
    val imageUrl: String?,
    val title: String,
    val time: String, // "HH:mm"
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)
