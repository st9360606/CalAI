package com.calai.app.data.profile.repo

import kotlin.math.floor
import kotlin.math.roundToInt

// === 標準換算常數 ===
// 1 lb = 0.45359237 kg
private const val KG_PER_LB = 0.45359237
private const val LBS_PER_KG = 1.0 / KG_PER_LB

// ★ 一律用「無條件捨去」到一位小數（增加 1e-8 防浮點誤差）
fun round1Floor(v: Double): Double = floor(v * 10.0 + 1e-8) / 10.0

// ★ kg ↔ lbs：一律到 1 位小數（無條件捨去）
fun kgToLbs1(v: Double): Double = round1Floor(v * LBS_PER_KG)
fun lbsToKg1(v: Double): Double = round1Floor(v * KG_PER_LB)

// ★ 新增：lbs 本身的 0.1 無條件捨去（給「存入 DataStore」用）
fun roundLbs1(v: Double): Float = round1Floor(v).toFloat()

/** ft/in -> cm（0.1cm 無條件捨去） */
fun feetInchesToCm1(feet: Int, inches: Int): Double {
    val totalInches = feet.coerceAtLeast(0) * 12 + inches.coerceIn(0, 11)
    val cm = totalInches * 2.54
    return round1Floor(cm) // 70in -> 177.8 ✅
}

/** cm -> ft/in（用「最近整吋」；避免 69.999999 -> 69 的地雷） */
fun cmToFeetInches1(cm: Double): Pair<Int, Int> {
    val totalInches = (cm / 2.54).roundToInt().coerceAtLeast(0) // ✅ 用 round，不要 toInt()
    var feet = totalInches / 12
    var inches = totalInches % 12
    if (inches == 12) { feet += 1; inches = 0 }
    return feet to inches
}

// ★ kg 存檔：一律到 1 位小數（無條件捨去）
fun roundKg1(v: Double): Float = round1Floor(v).toFloat()

// ★ cm 存檔：一律到 1 位小數（無條件捨去）
fun roundCm1(v: Double): Float = round1Floor(v).toFloat()
