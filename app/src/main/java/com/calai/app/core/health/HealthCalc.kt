package com.calai.app.core.health

import kotlin.math.max
import kotlin.math.roundToInt

enum class Gender { Male, Female }

data class HealthInputs(
    val gender: Gender,
    val age: Int,            // years
    val heightCm: Float,     // cm
    val weightKg: Float,     // kg
    val workoutsPerWeek: Int // 0,1..n 或代表值 0/2/4/6/7
)

data class MacroPlan(
    val kcal: Int,
    val carbsGrams: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val bmi: Double,
    val bmiClass: BmiClass
)

enum class BmiClass { Underweight, Normal, Overweight, Obesity }

/** 巨量營養素比例（0~1） */
data class MacroSplit(
    val proteinPct: Float,
    val fatPct: Float,
    val carbPct: Float
) {
    /** 確保比例總和為 1.0（避免小數誤差） */
    fun normalized(): MacroSplit {
        val sum = (proteinPct + fatPct + carbPct).coerceAtLeast(0.0001f)
        return MacroSplit(
            proteinPct = proteinPct / sum,
            fatPct = fatPct / sum,
            carbPct = carbPct / sum
        )
    }
}

object HealthCalc {

    /** UI 的運動次數 → 桶化（與 0/1–3/3–5/6–7/7+ 一致） */
    private fun bucketWorkouts(v: Int): Int = when {
        v <= 0      -> 0
        v in 1..3   -> 2
        v in 4..5   -> 4
        v in 6..7   -> 6
        else        -> 7
    }

    /** Mifflin–St Jeor BMR */
    fun bmr(inputs: HealthInputs): Double {
        val s = if (inputs.gender == Gender.Male) 5.0 else -161.0
        return 10.0 * inputs.weightKg +
                6.25 * inputs.heightCm -
                5.0 * inputs.age + s
    }

    /** 活動係數 PAL（與 UI 對齊） */
    fun activityFactor(workoutsPerWeek: Int): Double = when (bucketWorkouts(workoutsPerWeek)) {
        0   -> 1.20
        2   -> 1.375
        4   -> 1.55
        6   -> 1.725
        else-> 1.90
    }

    /** 維持熱量（TDEE） */
    fun tdee(inputs: HealthInputs): Double = bmr(inputs) * activityFactor(inputs.workoutsPerWeek)

    /**
     * 依「目標」回傳預設比例：
     * LOSE(減重)：  P30 / F30 / C40
     * MAINTAIN(維持)：P25 / F30 / C45
     * GAIN(增肌)：  P30 / F25 / C45
     * HEALTHY_EATING(健康)：P20 / F35 / C45
     * 其他 / null → 維持
     */
    fun splitForGoalKey(goalKey: String?): MacroSplit = when (goalKey) {
        "LOSE"            -> MacroSplit(0.30f, 0.30f, 0.40f)
        "MAINTAIN"        -> MacroSplit(0.25f, 0.30f, 0.45f)
        "GAIN"            -> MacroSplit(0.30f, 0.25f, 0.45f)
        "HEALTHY_EATING"  -> MacroSplit(0.20f, 0.30f, 0.50f)
        else              -> MacroSplit(0.20f, 0.30f, 0.50f) // default: 維持
    }

    /**
     * 依比例產生巨量營養素配置（公克）。
     * - g = kcal * pct / 每克熱量（P/C=4、F=9）
     * - kcal 預設取 TDEE，並設下限 1000
     */
    fun macroPlanBySplit(
        inputs: HealthInputs,
        split: MacroSplit,
        targetKcal: Int? = null
    ): MacroPlan {
        val kcal = max(1000, (targetKcal ?: tdee(inputs).roundToInt()))
        val norm = split.normalized()

        val proteinG = ((kcal * norm.proteinPct) / 4.0f).roundToInt()
        val fatG     = ((kcal * norm.fatPct) / 9.0f).roundToInt()
        val carbsG   = ((kcal * norm.carbPct) / 4.0f).roundToInt()

        val bmiVal = bmi(inputs.weightKg.toDouble(), inputs.heightCm.toDouble())
        val bmiClass = classifyBmi(bmiVal)

        return MacroPlan(
            kcal = kcal,
            carbsGrams = carbsG,
            proteinGrams = proteinG,
            fatGrams = fatG,
            bmi = round1(bmiVal),
            bmiClass = bmiClass
        )
    }

    /** 若你仍需要舊版（依 g/kg + 碳水比例）的計算，保留原 API */
    fun macroPlan(
        inputs: HealthInputs,
        targetKcal: Int? = null,
        proteinGPerKg: Float = 1.5f,
        carbPct: Float = 0.55f
    ): MacroPlan {
        val kcal = max(1000, (targetKcal ?: tdee(inputs).roundToInt()))
        val proteinG = (inputs.weightKg * proteinGPerKg)
            .coerceIn(1.2f * inputs.weightKg, 2.2f * inputs.weightKg)
            .roundToInt()

        val proteinKcal = proteinG * 4
        val carbsKcal = (kcal * carbPct).roundToInt()
        val fatKcal = max(0, kcal - proteinKcal - carbsKcal)

        val carbsG = (carbsKcal / 4.0).roundToInt()
        val fatG = (fatKcal / 9.0).roundToInt()

        val bmiVal = bmi(inputs.weightKg.toDouble(), inputs.heightCm.toDouble())
        val bmiClass = classifyBmi(bmiVal)

        return MacroPlan(
            kcal = kcal,
            carbsGrams = carbsG,
            proteinGrams = proteinG,
            fatGrams = fatG,
            bmi = round1(bmiVal),
            bmiClass = bmiClass
        )
    }

    /** BMI = kg / m^2（含最小值保護） */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        val safeCm = max(0.001, heightCm)
        val m = safeCm / 100.0
        return weightKg / (m * m)
    }

    fun classifyBmi(bmi: Double): BmiClass = when {
        bmi < 18.5 -> BmiClass.Underweight
        bmi < 25.0 -> BmiClass.Normal
        bmi < 30.0 -> BmiClass.Overweight
        else -> BmiClass.Obesity
    }

    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0
}
