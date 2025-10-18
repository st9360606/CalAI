package com.calai.app.ui.home.components

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
                Text(
                    "Calories left",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
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
            minHeight = 120.dp,           // 兩張卡一致
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
            minHeight = 120.dp,           // 兩張卡一致
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
    minHeight: Dp = 116.dp,               // 與兩張卡一致
    ringSize: Dp = RingDefaults.Size,
    ringStroke: Dp = RingDefaults.Stroke,
    centerDisk: Dp = RingDefaults.CenterDisk,
    leftExtra: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 用固定內容高度做對齊基準
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight)
        ) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左欄
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = primary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF0F172A),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (!secondary.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = secondary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (leftExtra != null) {
                        Spacer(Modifier.height(8.dp))
                        leftExtra()
                    }
                }

                // 右欄：填滿卡片高度，再在右半區塊內置中
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),          // ★ 關鍵：確保右欄高度 = 卡片高度
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(ringSize),
                        contentAlignment = Alignment.Center
                    ) {
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
                    }
                }
            }
        }
    }
}
