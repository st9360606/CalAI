package com.calai.app.ui.home.components

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.data.home.repo.HomeSummary
import kotlin.math.roundToInt

// 統一圓環尺寸（與「蛋白質」卡相同）
private object RingDefaults {
    val Size = 60.dp      // 圓直徑
    val Stroke = 6.dp     // 圓環粗細
    val CenterDisk = 26.dp// 圓心淺灰底大小
}

@Composable
fun CaloriesCardModern(
    caloriesLeft: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    cardHeight: Dp = PanelHeights.Metric,   // ★ 新增：固定高度
    ringSize: Dp = RingDefaults.Size,
    ringStroke: Dp = RingDefaults.Stroke,
    centerDisk: Dp = RingDefaults.CenterDisk,
    contentPaddingH: Dp = 16.dp,
    contentPaddingV: Dp = 12.dp,
) {
    Card(
        modifier = modifier.height(cardHeight), // ★ 固定高度，避免分頁高度不一
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .padding(horizontal = contentPaddingH, vertical = contentPaddingV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "$caloriesLeft",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text("Calories left", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            }
            Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                GaugeRing(
                    progress = progress,
                    sizeDp = ringSize,
                    strokeDp = ringStroke,
                    trackColor = Color(0xFFE8EAEE),
                    progressColor = Color(0xFF111827),
                    drawTopTick = true,
                    tickColor = Color(0xFF111827)
                )
                androidx.compose.material3.Surface(
                    color = Color(0xFFF3F4F6),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(centerDisk),
                    content = {}
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
    // ↓ 改成跟蛋白質一致的預設（之後 Steps/Workout 也會引用同一組）
    minHeight: Dp = 132.dp,
    ringSize: Dp = RingDefaults.Size,
    ringStroke: Dp = RingDefaults.Stroke,
    centerDisk: Dp = RingDefaults.CenterDisk,
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )

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
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = CircleShape,
                    modifier = Modifier.size(centerDisk)
                ) {}
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
        val steps = summary.todayActivity.steps
        val stepsKcalApprox = (steps * 0.04f).roundToInt()

        // Steps
        ActivityStatCardSplit(
            title = "Steps",
            primary = "$steps",
            secondary = "≈ $stepsKcalApprox kcal",
            ringColor = Color(0xFF3B82F6),
            progress = 0f,
            modifier = Modifier.weight(1f),
            cardHeight = PanelHeights.Metric,
            ringSize = 76.dp,             // 兩張卡一致
            ringStroke = 8.dp,
            centerDisk = 30.dp
        )

        // Workout
        val workoutKcal = summary.todayActivity.activeKcal.toInt()
        ActivityStatCardSplit(
            title = "Workout",
            primary = "$workoutKcal kcal",
            secondary = null,
            ringColor = Color(0xFFA855F7),
            progress = 0f,
            modifier = Modifier.weight(1f),
            cardHeight = PanelHeights.Metric,
            ringSize = 76.dp,             // 兩張卡一致
            ringStroke = 8.dp,
            centerDisk = 30.dp,
            leftExtra = {
                Surface(color = Color.Black, shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        )
    }
}

/**
 * 活動類卡片（左右分欄）：
 * - 左：主/副文字 + 可選額外小圖示
 * - 右：圓形進度條（含中心淺灰圓）
 */
@Composable
fun ActivityStatCardSplit(
    title: String,
    primary: String,
    secondary: String? = null,
    ringColor: Color,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    cardHeight: Dp = PanelHeights.Metric, // ← 改用固定高度（預設 = Macro 高度）
    ringSize: Dp = RingDefaults.Size,
    ringStroke: Dp = RingDefaults.Stroke,
    centerDisk: Dp = RingDefaults.CenterDisk,
    drawRing: Boolean = true,
    leftExtra: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.height(cardHeight), // ★ 直接鎖定卡片高度
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 內容用「填滿卡片高度」當置中基準
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左欄：靠上；沒副標就用 Spacer 占位，避免高度漂移
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = primary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF0F172A),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                if (!secondary.isNullOrBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(Modifier.height(18.dp)) // ≈ 一行 bodySmall 的高度
                }
                if (leftExtra != null) {
                    Spacer(Modifier.height(8.dp))
                    leftExtra()
                }
            }

            // 右欄：幾何置中；關閉圓環時保留同等空間
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                    if (drawRing) {
                        GaugeRing(
                            progress = progress,
                            sizeDp = ringSize,
                            strokeDp = ringStroke,
                            trackColor = Color(0xFFE8EAEE),
                            progressColor = ringColor,
                            drawTopTick = true,
                            tickColor = ringColor
                        )
                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = CircleShape,
                            modifier = Modifier.size(centerDisk),
                            content = {}
                        )
                    } else {
                        Spacer(Modifier.size(ringSize))
                    }
                }
            }
        }
    }
}

@Composable
fun WeightFastingRowModern(summary: HomeSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== Weight（顯示「距離目標」）=====
        val unit = summary.weightDiffUnit
        val deltaToGoal = -summary.weightDiffSigned
        val absDelta = abs(deltaToGoal)
        val primaryText =
            if (unit == "lbs") "${absDelta.roundToInt()} $unit"
            else String.format(Locale.getDefault(), "%.1f %s", absDelta, unit)

        ActivityStatCardSplit(
            title = "Weight",
            primary = primaryText,
            secondary = "from goal",
            ringColor = Color(0xFF06B6D4),
            progress = 0f,
            modifier = Modifier.weight(1f),
            cardHeight = PanelHeights.Metric,   // ★ 固定高度
            ringSize = RingDefaults.Size,
            ringStroke = RingDefaults.Stroke,
            centerDisk = RingDefaults.CenterDisk
        )

        // ===== Fasting plan（無圓環）=====
        val plan = summary.fastingPlan ?: "—"
        ActivityStatCardSplit(
            title = "Fasting plan",
            primary = plan,
            secondary = null,
            ringColor = Color.Transparent,
            progress = 0f,
            modifier = Modifier.weight(1f),
            cardHeight = PanelHeights.Metric,   // ★ 固定高度
            ringSize = RingDefaults.Size,
            ringStroke = RingDefaults.Stroke,
            centerDisk = RingDefaults.CenterDisk,
            drawRing = false
        )
    }
}
