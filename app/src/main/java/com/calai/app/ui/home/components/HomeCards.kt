package com.calai.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.app.data.home.repo.HomeSummary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.unit.Dp


@Composable
fun CaloriesCardModern(
    caloriesLeft: Int,
    progress: Float,
    contentPaddingH: Dp = 20.dp,
    contentPaddingV: Dp = 14.dp, // ← 原本 18.dp，調小一點
    ringSize: Dp = 80.dp,        // ← 原本 84.dp
    ringStroke: Dp = 10.dp       // ← 原本 12.dp
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPaddingH, vertical = contentPaddingV),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$caloriesLeft",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(2.dp))
                Text("Calories left", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            }
            Box(contentAlignment = Alignment.Center) {
                GaugeRing(
                    progress = progress,
                    sizeDp = ringSize,
                    strokeDp = ringStroke,
                    trackColor = Color(0xFFE8EAEE),
                    progressColor = Color(0xFF111827),
                    drawTopTick = true
                )
                Icon(
                    imageVector = Icons.Filled.Whatshot,
                    contentDescription = null,
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun MacroRowModern(s: HomeSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Protein = 紅
        MacroStatCardModern(
            value = "${s.proteinG}g",
            label = "Protein left",
            ringColor = Color(0xFFEF4444),
            icon = {
                Icon(
                    imageVector = Icons.Filled.EggAlt,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        // Carbs = 橘（麵包）
        MacroStatCardModern(
            value = "${s.carbsG}g",
            label = "Carbs left",
            ringColor = Color(0xFFF59E0B),
            icon = {
                Icon(
                    imageVector = Icons.Filled.BakeryDining,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        // Fats = 綠（油滴；若要酪梨需自訂向量）
        MacroStatCardModern(
            value = "${s.fatG}g",
            label = "Fats left",
            ringColor = Color(0xFF22C55E),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Opacity,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroStatCardModern(
    value: String,
    label: String,
    ringColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    // ↓ 新增可調參數（都有預設值，Macro 三卡不必改）
    minHeight: Dp = 132.dp,
    ringSize: Dp = 60.dp,
    ringStroke: Dp = 6.dp,
    centerDisk: Dp = 26.dp,
    spacingTop: Dp = 12.dp
) {
    Card(
        modifier = modifier.heightIn(min = minHeight),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF0F172A)
            )
            Spacer(Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))

            Spacer(Modifier.height(spacingTop))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GaugeRing(
                    progress = progress,
                    sizeDp = ringSize,
                    strokeDp = ringStroke,
                    trackColor = Color(0xFFE8EAEE),
                    progressColor = ringColor,
                    drawTopTick = true,
                    tickColor = ringColor
                )
                Surface(color = Color(0xFFF3F4F6), shape = CircleShape, modifier = Modifier.size(centerDisk)) {}
                icon()
            }
        }
    }
}

@Composable
fun StepsWorkoutRowModern(summary: HomeSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Steps（藍）
        MacroStatCardModern(
            value = "${summary.todayActivity.steps}",
            label = "Steps",
            ringColor = Color(0xFF3B82F6),
            icon = {
                Icon(
                    imageVector = Icons.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f),
            progress = 0f,
            // ↓ 縮小一點
            minHeight = 112.dp,
            ringSize = 56.dp,
            ringStroke = 6.dp,
            centerDisk = 24.dp,
            spacingTop = 8.dp
        )

        // Workout（紫）
        MacroStatCardModern(
            value = "${summary.todayActivity.activeKcal.toInt()} kcal",
            label = "Workout",
            ringColor = Color(0xFFA855F7),
            icon = {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f),
            progress = 0f,
            // ↓ 縮小一點
            minHeight = 112.dp,
            ringSize = 56.dp,
            ringStroke = 6.dp,
            centerDisk = 24.dp,
            spacingTop = 8.dp
        )
    }
}


