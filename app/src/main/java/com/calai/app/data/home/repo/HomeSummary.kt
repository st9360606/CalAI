package com.calai.app.data.home.repo

import android.net.Uri
import com.calai.app.data.health.TodayActivity
import com.calai.app.data.meals.api.MealItemDto

/**
 * Home 畫面所需的彙總資料。
 * 只包含 UI 會用到的欄位，避免不必要重組。
 */
data class HomeSummary(
    val tdee: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val bmi: Double,
    val bmiLabel: String,
    val waterGoalMl: Int,
    val waterTodayMl: Int,
    // current - target（正=還需減；負=已低於目標）
    val weightDiffSigned: Double,
    val weightDiffUnit: String, // "kg" or "lbs"
    val fastingPlan: String?,
    val todayActivity: TodayActivity,
    val recentMeals: List<MealItemDto>,
    val avatarUrl: Uri?
)
