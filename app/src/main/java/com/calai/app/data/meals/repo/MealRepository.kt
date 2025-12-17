package com.calai.app.data.meals.repo

import com.calai.app.data.meals.api.MealApi
import com.calai.app.data.meals.api.MealItemDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val api: MealApi
) {
    suspend fun loadRecent(limit: Int = 10): List<MealItemDto> = runCatching {
        api.recentMeals(limit)
    }.getOrDefault(emptyList())
}
