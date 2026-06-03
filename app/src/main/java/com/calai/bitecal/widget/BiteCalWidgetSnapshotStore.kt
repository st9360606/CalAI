package com.calai.bitecal.widget

import android.content.Context
import androidx.core.content.edit
import com.calai.bitecal.data.foodlog.repo.HomeTodayNutritionSummary
import com.calai.bitecal.data.home.repo.HomeSummary

/**
 * Desktop widgets cannot depend on Compose ViewModel state directly because the launcher owns their UI.
 * Keep the latest known Home nutrition snapshot in SharedPreferences so AppWidgetProvider can render it
 * even when the app process is recreated by the system.
 */
object BiteCalWidgetSnapshotStore {
    private const val PREFS_NAME = "bitecal_widget_snapshot"

    /**
     * Increase this when widget XML / RemoteViews rendering changes but visible nutrition data does not.
     * This forces one refresh so existing desktop widget instances pick up the new UI.
     */
    private const val WIDGET_RENDER_VERSION = 31

    private const val KEY_HAS_SNAPSHOT = "has_snapshot"
    private const val KEY_RENDER_VERSION = "render_version"
    private const val KEY_GOAL_KCAL = "goal_kcal"
    private const val KEY_EATEN_KCAL = "eaten_kcal"
    private const val KEY_PROTEIN_GOAL_G = "protein_goal_g"
    private const val KEY_EATEN_PROTEIN_G = "eaten_protein_g"
    private const val KEY_CARBS_GOAL_G = "carbs_goal_g"
    private const val KEY_EATEN_CARBS_G = "eaten_carbs_g"
    private const val KEY_FATS_GOAL_G = "fats_goal_g"
    private const val KEY_EATEN_FATS_G = "eaten_fats_g"
    private const val KEY_UPDATED_AT_MS = "updated_at_ms"

    /**
     * Saves the latest widget snapshot.
     *
     * @return true only when widget-visible data or widget render version changed.
     * When unchanged, avoid touching SharedPreferences and avoid re-applying RemoteViews
     * so the launcher does not redraw the widget unnecessarily.
     */
    fun saveFrom(
        context: Context,
        summary: HomeSummary?,
        todayNutrition: HomeTodayNutritionSummary
    ): Boolean {
        if (summary == null) return false

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val goalKcal = summary.tdee.coerceAtLeast(0)
        val eatenKcal = todayNutrition.eatenKcal.coerceAtLeast(0)
        val proteinGoalG = summary.proteinG.coerceAtLeast(0)
        val eatenProteinG = todayNutrition.eatenProteinG.coerceAtLeast(0)
        val carbsGoalG = summary.carbsG.coerceAtLeast(0)
        val eatenCarbsG = todayNutrition.eatenCarbsG.coerceAtLeast(0)
        val fatsGoalG = summary.fatG.coerceAtLeast(0)
        val eatenFatsG = todayNutrition.eatenFatsG.coerceAtLeast(0)

        val unchanged =
            prefs.getBoolean(KEY_HAS_SNAPSHOT, false) &&
                prefs.getInt(KEY_RENDER_VERSION, 0) == WIDGET_RENDER_VERSION &&
                prefs.getInt(KEY_GOAL_KCAL, DEFAULT_GOAL_KCAL) == goalKcal &&
                prefs.getInt(KEY_EATEN_KCAL, DEFAULT_EATEN_KCAL) == eatenKcal &&
                prefs.getInt(KEY_PROTEIN_GOAL_G, DEFAULT_PROTEIN_GOAL_G) == proteinGoalG &&
                prefs.getInt(KEY_EATEN_PROTEIN_G, DEFAULT_EATEN_PROTEIN_G) == eatenProteinG &&
                prefs.getInt(KEY_CARBS_GOAL_G, DEFAULT_CARBS_GOAL_G) == carbsGoalG &&
                prefs.getInt(KEY_EATEN_CARBS_G, DEFAULT_EATEN_CARBS_G) == eatenCarbsG &&
                prefs.getInt(KEY_FATS_GOAL_G, DEFAULT_FATS_GOAL_G) == fatsGoalG &&
                prefs.getInt(KEY_EATEN_FATS_G, DEFAULT_EATEN_FATS_G) == eatenFatsG

        if (unchanged) return false

        prefs.edit {
            putBoolean(KEY_HAS_SNAPSHOT, true)
            putInt(KEY_RENDER_VERSION, WIDGET_RENDER_VERSION)
            putInt(KEY_GOAL_KCAL, goalKcal)
            putInt(KEY_EATEN_KCAL, eatenKcal)
            putInt(KEY_PROTEIN_GOAL_G, proteinGoalG)
            putInt(KEY_EATEN_PROTEIN_G, eatenProteinG)
            putInt(KEY_CARBS_GOAL_G, carbsGoalG)
            putInt(KEY_EATEN_CARBS_G, eatenCarbsG)
            putInt(KEY_FATS_GOAL_G, fatsGoalG)
            putInt(KEY_EATEN_FATS_G, eatenFatsG)
            putLong(KEY_UPDATED_AT_MS, System.currentTimeMillis())
        }

        return true
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
    val caloriesLeft: Int = goalKcal.coerceAtLeast(0) - eatenKcal.coerceAtLeast(0)
    val proteinLeftG: Int = proteinGoalG.coerceAtLeast(0) - eatenProteinG.coerceAtLeast(0)
    val carbsLeftG: Int = carbsGoalG.coerceAtLeast(0) - eatenCarbsG.coerceAtLeast(0)
    val fatsLeftG: Int = fatsGoalG.coerceAtLeast(0) - eatenFatsG.coerceAtLeast(0)

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
