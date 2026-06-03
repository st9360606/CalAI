package com.calai.bitecal.widget

import android.content.Context
import com.calai.bitecal.data.foodlog.repo.HomeTodayNutritionSummary
import com.calai.bitecal.data.home.repo.HomeSummary

/**
 * Desktop widgets cannot depend on Compose ViewModel state directly because the launcher owns their UI.
 * Keep the latest known Home nutrition snapshot in SharedPreferences so AppWidgetProvider can render it
 * even when the app process is recreated by the system.
 */
object BiteCalWidgetSnapshotStore {
    private const val PREFS_NAME = "bitecal_widget_snapshot"

    private const val KEY_GOAL_KCAL = "goal_kcal"
    private const val KEY_EATEN_KCAL = "eaten_kcal"
    private const val KEY_PROTEIN_GOAL_G = "protein_goal_g"
    private const val KEY_EATEN_PROTEIN_G = "eaten_protein_g"
    private const val KEY_CARBS_GOAL_G = "carbs_goal_g"
    private const val KEY_EATEN_CARBS_G = "eaten_carbs_g"
    private const val KEY_FATS_GOAL_G = "fats_goal_g"
    private const val KEY_EATEN_FATS_G = "eaten_fats_g"
    private const val KEY_UPDATED_AT_MS = "updated_at_ms"

    fun saveFrom(
        context: Context,
        summary: HomeSummary?,
        todayNutrition: HomeTodayNutritionSummary
    ) {
        if (summary == null) return

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_GOAL_KCAL, summary.tdee.coerceAtLeast(0))
            .putInt(KEY_EATEN_KCAL, todayNutrition.eatenKcal.coerceAtLeast(0))
            .putInt(KEY_PROTEIN_GOAL_G, summary.proteinG.coerceAtLeast(0))
            .putInt(KEY_EATEN_PROTEIN_G, todayNutrition.eatenProteinG.coerceAtLeast(0))
            .putInt(KEY_CARBS_GOAL_G, summary.carbsG.coerceAtLeast(0))
            .putInt(KEY_EATEN_CARBS_G, todayNutrition.eatenCarbsG.coerceAtLeast(0))
            .putInt(KEY_FATS_GOAL_G, summary.fatG.coerceAtLeast(0))
            .putInt(KEY_EATEN_FATS_G, todayNutrition.eatenFatsG.coerceAtLeast(0))
            .putLong(KEY_UPDATED_AT_MS, System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context): BiteCalWidgetSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return BiteCalWidgetSnapshot(
            goalKcal = prefs.getInt(KEY_GOAL_KCAL, DEFAULT_GOAL_KCAL),
            eatenKcal = prefs.getInt(KEY_EATEN_KCAL, DEFAULT_EATEN_KCAL),
            proteinGoalG = prefs.getInt(KEY_PROTEIN_GOAL_G, DEFAULT_PROTEIN_GOAL_G),
            eatenProteinG = prefs.getInt(KEY_EATEN_PROTEIN_G, DEFAULT_EATEN_PROTEIN_G),
            carbsGoalG = prefs.getInt(KEY_CARBS_GOAL_G, DEFAULT_CARBS_GOAL_G),
            eatenCarbsG = prefs.getInt(KEY_EATEN_CARBS_G, DEFAULT_EATEN_CARBS_G),
            fatsGoalG = prefs.getInt(KEY_FATS_GOAL_G, DEFAULT_FATS_GOAL_G),
            eatenFatsG = prefs.getInt(KEY_EATEN_FATS_G, DEFAULT_EATEN_FATS_G),
            updatedAtMs = prefs.getLong(KEY_UPDATED_AT_MS, 0L)
        )
    }

    private const val DEFAULT_GOAL_KCAL = 2430
    private const val DEFAULT_EATEN_KCAL = 0
    private const val DEFAULT_PROTEIN_GOAL_G = 152
    private const val DEFAULT_EATEN_PROTEIN_G = 0
    private const val DEFAULT_CARBS_GOAL_G = 273
    private const val DEFAULT_EATEN_CARBS_G = 0
    private const val DEFAULT_FATS_GOAL_G = 81
    private const val DEFAULT_EATEN_FATS_G = 0
}

data class BiteCalWidgetSnapshot(
    val goalKcal: Int,
    val eatenKcal: Int,
    val proteinGoalG: Int,
    val eatenProteinG: Int,
    val carbsGoalG: Int,
    val eatenCarbsG: Int,
    val fatsGoalG: Int,
    val eatenFatsG: Int,
    val updatedAtMs: Long
) {
    val caloriesLeft: Int = (goalKcal - eatenKcal).coerceAtLeast(0)
    val proteinLeftG: Int = (proteinGoalG - eatenProteinG).coerceAtLeast(0)
    val carbsLeftG: Int = (carbsGoalG - eatenCarbsG).coerceAtLeast(0)
    val fatsLeftG: Int = (fatsGoalG - eatenFatsG).coerceAtLeast(0)

    val calorieProgress: Int = progressPercent(eatenKcal, goalKcal)
    val proteinProgress: Int = progressPercent(eatenProteinG, proteinGoalG)
    val carbsProgress: Int = progressPercent(eatenCarbsG, carbsGoalG)
    val fatsProgress: Int = progressPercent(eatenFatsG, fatsGoalG)

    private companion object {
        fun progressPercent(current: Int, goal: Int): Int {
            if (goal <= 0) return 0
            return ((current.coerceAtLeast(0).toFloat() / goal.toFloat()) * 100f)
                .toInt()
                .coerceIn(0, 100)
        }
    }
}
