package com.calai.app.ui.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.data.home.repo.HomeSummary
import kotlin.math.roundToInt
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.sp

// 統一圓環尺寸（與「蛋白質」卡相同）
private object RingDefaults {
    val Size = 64.dp      // 圓直徑
    val Stroke = 5.dp     // 圓環粗細
    val CenterDisk = 32.dp// 圓心淺灰底大小
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
fun MacroRowModern(
    s: HomeSummary,
    cardHeight: Dp = PanelHeights.Metric
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight
        )
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
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight
        )
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
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight
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
    cardHeight: Dp = PanelHeights.Metric, // ← 改成固定高度參數
    ringSize: Dp = RingDefaults.Size,
    ringStroke: Dp = RingDefaults.Stroke,
    centerDisk: Dp = RingDefaults.CenterDisk,
    spacingTop: Dp = 12.dp
) {
    Card(
        modifier = modifier.height(cardHeight),          // ← 固定高度
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
fun StepsWorkoutRowModern(
    summary: HomeSummary,
    // ★ 新增：卡片高度 & 圓環大小，可依需求調整
    cardHeight: Dp = 120.dp,   // 原 132.dp → 小一點
    ringSize: Dp = 68.dp,      // 原 76.dp → 略小，避免卡片太擠
    centerDisk: Dp = 28.dp,    // 原 30.dp → 跟著縮小一點
    ringStroke: Dp = 8.dp,    // 保持視覺厚度不變（要更輕可改 7.dp）
    // ★ 新增：Workout 黑圓＋大小可調
    plusButtonSize: Dp = 24.dp,  // 黑色圓的直徑（預設放大）
    plusIconSize: Dp = 19.dp     // 中間白色「＋」圖示大小
) {
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
            cardHeight = cardHeight,
            ringSize = ringSize,
            ringStroke = ringStroke,
            centerDisk = centerDisk
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
            cardHeight = cardHeight,
            ringSize = ringSize,
            ringStroke = ringStroke,
            centerDisk = centerDisk,
            leftExtra = {
                // ★ 用 requiredSize 強制正方形，避免被父層拉伸
                Surface(
                    modifier = Modifier.requiredSize(plusButtonSize),
                    shape = CircleShape,
                    color = Color.Black
                ) {
                    // ★ 填滿 Surface（已是正方形），就不會變橢圓
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(plusIconSize)
                        )
                    }
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
    cardHeight: Dp = PanelHeights.Metric,
    ringSize: Dp = RingDefaults.Size,
    ringStroke: Dp = RingDefaults.Stroke,
    centerDisk: Dp = RingDefaults.CenterDisk,
    drawRing: Boolean = true,
    // ↓↓↓ 新增：標題前綴（例如小三角）與間距
    titlePrefix: (@Composable () -> Unit)? = null,
    titlePrefixGap: Dp = 4.dp,
    // 文字樣式/間距（先前就有）
    titleTextStyle: TextStyle? = null,
    primaryTextStyle: TextStyle? = null,
    secondaryTextStyle: TextStyle? = null,
    gapTitleToPrimary: Dp = 4.dp,
    gapPrimaryToSecondary: Dp = 2.dp,
    leftExtra: (@Composable () -> Unit)? = null
) {
    val titleStyle = titleTextStyle ?: MaterialTheme.typography.bodySmall
    val primaryStyle = primaryTextStyle ?: MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val secondaryStyle = secondaryTextStyle ?: MaterialTheme.typography.bodySmall

    Card(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                // 標題列：可插入前綴小圖示（例如小三角）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (titlePrefix != null) {
                        titlePrefix()
                        Spacer(Modifier.width(titlePrefixGap))
                    }
                    Text(
                        text = title,
                        style = titleStyle,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(gapTitleToPrimary))

                Text(
                    text = primary,
                    style = primaryStyle,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(gapPrimaryToSecondary))
                if (!secondary.isNullOrBlank()) {
                    Text(
                        text = secondary,
                        style = secondaryStyle,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                }
                leftExtra?.let {
                    Spacer(Modifier.height(8.dp))
                    it()
                }
            }

            // 右側圓環（略，與你現有相同）
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
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
                        Surface(color = Color(0xFFF3F4F6), shape = CircleShape, modifier = Modifier.size(centerDisk)) {}
                    } else {
                        Spacer(Modifier.size(ringSize))
                    }
                }
            }
        }
    }
}

@Composable
fun WeightFastingRowModern(
    summary: HomeSummary,
    cardHeight: Dp = PanelHeights.Metric,
    plusButtonSize: Dp = 24.dp,
    plusIconSize: Dp = 19.dp,
    onOpenFastingPlans: () -> Unit = {},
    fastingStartText: String? = null,
    fastingEndText: String? = null,
    planOverride: String? = null,
    fastingEnabled: Boolean = false,
    onToggle: (Boolean) -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

        // 左卡：Weight（維持原樣）
        val goal = -summary.weightDiffSigned
        val unit = summary.weightDiffUnit
        val primaryText =
            if (unit == "lbs") String.format(java.util.Locale.getDefault(), "%+d lbs", goal.roundToInt())
            else String.format(java.util.Locale.getDefault(), "%+.1f %s", goal, unit)

        // 左卡 Weight（原樣，重點是有 weight(1f)）
        ActivityStatCardSplit(
            title = "Weight",
            primary = primaryText,
            secondary = "to goal",
            ringColor = Color(0xFF06B6D4),
            progress = 0f,
            modifier = Modifier.weight(1f), // ★ 保持 50%
            cardHeight = cardHeight,
            ringSize = 74.dp,
            ringStroke = 6.dp,
            centerDisk = 32.dp,
            drawRing = true,
            titlePrefix = { TitlePrefixTriangle(side = 6.dp, color = Color.Black) },
            titlePrefixGap = 6.dp,
            titleTextStyle = MaterialTheme.typography.bodySmall,
            primaryTextStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            secondaryTextStyle = MaterialTheme.typography.bodySmall,
            gapTitleToPrimary = 10.dp,
            gapPrimaryToSecondary = 2.dp,
            leftExtra = {
                Surface(
                    modifier = Modifier.requiredSize(plusButtonSize),
                    shape = CircleShape,
                    color = Color.Black
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.requiredSize(plusIconSize)
                        )
                    }
                }
            }
        )
        // 右卡 Fasting Plan（改用 modifier.weight(1f).height(cardHeight)）
        val plan = planOverride ?: (summary.fastingPlan ?: "—")

        FastingPlanCard(
            planTitle = "Fasting Plan",
            planName = plan,
            startLabel = "start time",
            startText = fastingStartText,
            endLabel = "end time",
            endText = fastingEndText,
            enabled = fastingEnabled,
            onToggle = onToggle,
            onClick = onOpenFastingPlans,
            cardHeight = cardHeight,
            modifier = Modifier.weight(1f).height(cardHeight),
            topBarHeight = 30.dp,            // ★ 更薄
            topBarTextStyle = MaterialTheme.typography.labelMedium, // 更低調一點
            planNameYOffset = (2).dp,        // ★ 再往上
            planNameFontSize = 32.sp
        )
    }
}

/** 自訂綠色開關（#34C759），接近你上傳圖檔風格 */
@Composable
fun GreenSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 52.dp,
    height: Dp = 32.dp,
) {
    val radius = height / 2
    val thumbSize = height - 4.dp
    val trackOn = Color(0xFF34C759)
    val trackOff = Color(0xFFE5E7EB)
    val thumb = Color.White

    val offset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - 2.dp else 2.dp,
        label = "thumbOffset"
    )

    // 取消 ripple/press 陰影，避免顏色變暗
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(radius))
            .background(if (checked) trackOn else trackOff)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(thumbSize)
                .shadow(3.dp, CircleShape, clip = false)
                .background(thumb, CircleShape)
        )
    }
}

/**
 * 小三角（等邊，頂點朝上），尺寸獨立於文字大小
 */
@Composable
fun TitlePrefixTriangle(
    side: Dp = 8.dp,                 // ← 想更小/更大改這裡
    color: Color = Color(0xFF06B6D4) // ← 品牌色/想要的顏色
) {
    Canvas(modifier = Modifier.size(side)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w / 2f, 0f)   // 上頂點
            lineTo(0f, h)        // 左下
            lineTo(w, h)         // 右下
            close()
        }
        drawPath(path = path, color = color)
    }
}
