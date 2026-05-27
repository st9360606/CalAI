package com.calai.bitecal.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Home cards style source of truth.
 *
 * Scope:
 * - CalendarStrip
 * - Calories / macro / micronutrient / health-score cards
 * - Steps / workout / weight / fasting / water cards
 *
 * Keep this object as the single place to tune shared Home card visual tokens.
 */
object HomeCardStyles {
    object Surface {
        // 卡片背景：微暖白，避免與背景漸層黏成一片
        val Bg = Color(0xFFFDFDFE)

        // 邊框：更深一點，讓卡片邊界更明顯
        val BorderColor = Color(0xFFE0E2E6)
        val Border = BorderStroke(1.dp, BorderColor)

        // Home 主要卡片圓角
        val Corner = RoundedCornerShape(20.dp)

        // 輕量立體感
        val Elevation: Dp = 0.7.dp
    }

    object Ring {
        val Size = 66.dp
        val Stroke = 5.dp
        val CenterDisk = 34.dp
        val Track = Color(0xFFEFF0F3)
        val CenterFill = RingColors.CenterFill
    }

    object Palette {
        val Calories = Color(0xFF1F1A17)
        val Protein = Color(0xFFE56C6C)
        val Carbs = Color(0xFFD89A62)
        val Fats = Color(0xFF6C93D8)
        val Fiber = Color(0xFFA78BFA)
        val Sugar = Color(0xFFF08AAF)
        val Sodium = Color(0xFF73B6E6)
        val HealthScore = Color(0xFF5ECB7A)
        val Workout = Color(0xFFA37FE0)
        val Steps = Color(0xFF6BB8DA)
        val Weight = Color(0xFF5ECB7A)
        val Water = Color(0xFF73B6E6)
        val Fasting = Color(0xFF111114)
    }

    object Text {
        val Primary = Color(0xFF0F172A)
        val Secondary = Color(0xFF111114)
        val Muted = Color(0xFF7C7C85)
        val Label = Color(0xFF6B7280)
    }

    object TopBar {
        val Height = 26.dp
        val HorizontalPadding = 16.dp
        val Container = Color.Black
        val Content = Color.White
        val Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    }

    object Calendar {
        val SelectedBackground = Color(0xFFFF8A33)
        val TodayBackground = Color.Gray
        val ActiveText = Color(0xFF111114)
        val DisabledText = Color(0xFF9CA3AF)
        val ActiveStroke = Color(0xFF111114)
        val DisabledStroke = Color(0xFFC1C7D0)

        // Ring color rules:
        // Green: within target / on target.
        // Brown: slightly over daily goal.
        // Red: far over daily goal.
        // Dotted gray: no meals logged / no kcal summary.
        val OnTargetStroke = Color(0xFF7DDF83)
        val SlightlyOverStroke = Color(0xFFB45309)
        val FarOverStroke = Color(0xFFD92D20)
        val NoMealStroke = Color(0xFF555A60)
        val TodayNoMealStroke = Color(0xFF2B3037)
    }
}

/**
 * Backward-compatible alias used by existing Home cards.
 *
 * New code should prefer HomeCardStyles.Surface / HomeCardStyles.Ring /
 * HomeCardStyles.Palette directly.
 */
object CardStyles {
    val Bg = HomeCardStyles.Surface.Bg
    val BorderColor = HomeCardStyles.Surface.BorderColor
    val Border = HomeCardStyles.Surface.Border
    val Corner = HomeCardStyles.Surface.Corner
    val Elevation: Dp = HomeCardStyles.Surface.Elevation
}
