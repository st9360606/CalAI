package com.calai.app.ui.home.ui.settings.details.common

import com.calai.app.data.profile.repo.UserProfileStore
import java.util.Locale
import kotlin.math.abs

object WeightDisplayFormatter {

    /**
     * 主行跟著 unit（符合你要的顯示 LBS 或 KG）
     * 次行顯示另一個單位（你若想只顯示單一單位，把 showSecondary=false）
     */
    fun formatMainSub(
        kg: Double?,
        lbs: Double?,
        unit: UserProfileStore.WeightUnit,
        showSecondary: Boolean = true
    ): Pair<String, String?> {

        val main = when (unit) {
            UserProfileStore.WeightUnit.KG -> when {
                kg != null -> "${smart(kg)} kg"
                lbs != null -> "${smart(lbs)} lbs" // fallback
                else -> "—"
            }
            UserProfileStore.WeightUnit.LBS -> when {
                lbs != null -> "${smart(lbs)} lbs"
                kg != null -> "${smart(kg)} kg"   // fallback
                else -> "—"
            }
        }

        val sub = if (!showSecondary) null else {
            when (unit) {
                UserProfileStore.WeightUnit.KG -> lbs?.let { "${smart(it)} lbs" }
                UserProfileStore.WeightUnit.LBS -> kg?.let { "${smart(it)} kg" }
            }
        }

        return main to sub
    }

    /** 整數不顯示 .0；小數最多 1 位 */
    fun smart(v: Double): String {
        val isInt = abs(v - v.toInt()) < 1e-9
        return if (isInt) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
    }
}
