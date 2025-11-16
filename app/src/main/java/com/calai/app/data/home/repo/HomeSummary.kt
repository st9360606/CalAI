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
    /**
     * Δ = current - target
     * - 正數：目前比目標重 → 還需要「減重」的量
     * - 負數：目前比目標輕 → 已超過目標，理論上可以「增重」到目標
     */
    val weightDiffSigned: Double,
    /** weightDiffSigned 使用的單位，"kg" 或 "lbs" */
    val weightDiffUnit: String,
    val fastingPlan: String?,
    val todayActivity: TodayActivity,
    val recentMeals: List<MealItemDto>,
    val avatarUrl: Uri?
)
