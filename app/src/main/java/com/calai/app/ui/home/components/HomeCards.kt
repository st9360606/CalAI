package com.calai.app.ui.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.calai.app.ui.home.ui.fasting.components.WeightCardNew

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
        modifier = modifier
            .height(cardHeight)
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardStyles.Border
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
                Surface(
                    color = RingColors.CenterFill, // ★ 使用統一更淺的顏色
                    shape = CircleShape,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)// ★ 更新：間距由 12.dp -> 8.dp，再更緊一點
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
        modifier = modifier
            .height(cardHeight)
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        border = CardStyles.Border,
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
                    color = RingColors.CenterFill, // ★ 更淺
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
    plusIconSize: Dp = 19.dp,     // 中間白色「＋」圖示大小
    // ★ 新增：點擊黑色 + 要做什麼
    onAddWorkoutClick: () -> Unit,
    onWorkoutCardClick: () -> Unit = {}
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
                // ✅ 新版：使用共用按鈕 + 灰色閃光
                WorkoutAddButton(
                    onClick = onAddWorkoutClick,
                    outerSizeDp = 36.dp,  // 觸控區 & 灰閃圈 (和 Water 卡一致)
                    innerSizeDp = 28.dp, // 黑底圓按鈕大小 (和 Water 卡一致)
                    iconSizeDp = 24.dp
                )
            },
            onCardClick = onWorkoutCardClick          // ★ 串進來
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

    // 小三角 prefix（可選）
    titlePrefix: (@Composable () -> Unit)? = null,
    titlePrefixGap: Dp = 4.dp,

    // 文字樣式/間距
    titleTextStyle: TextStyle? = null,
    primaryTextStyle: TextStyle? = null,
    secondaryTextStyle: TextStyle? = null,
    gapTitleToPrimary: Dp = 4.dp,
    gapPrimaryToSecondary: Dp = 2.dp,

    // ⭐ 左下角額外內容（Workout 的「+」按鈕）
    leftExtra: (@Composable () -> Unit)? = null,

    // ★★★ 新增：整張卡片點擊（可為 null 表示不啟用）
    onCardClick: (() -> Unit)? = null
) {
    val titleStyle = titleTextStyle ?: MaterialTheme.typography.bodySmall
    val primaryStyle = primaryTextStyle ?: MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val secondaryStyle = secondaryTextStyle ?: MaterialTheme.typography.bodySmall

    // ★ 為保持樣式不變：不使用 ripple
    val interaction = remember { MutableInteractionSource() }
    val clickableMod = if (onCardClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null
        ) { onCardClick() }
    } else Modifier

    Card(
        modifier = modifier
            .then(clickableMod)                 // ★ 套在 Card 上：整張卡片可點
            .height(cardHeight)
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        border = CardStyles.Border,
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
            // ========= 左半：用 Box 疊兩層 =========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 文字群組 (置頂)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
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
                        // 保留占位高度，避免版面大跳
                        Spacer(Modifier.height(18.dp))
                    }
                }

                // ⬇⬇⬇ Workout 的 + 按鈕固定在左下角，不再被 Column 擠壓
                leftExtra?.let { extraContent ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                    ) {
                        extraContent()
                    }
                }
            }

            // ========= 右半：圓環進度 =========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
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
                            color = RingColors.CenterFill,
                            shape = CircleShape,
                            modifier = Modifier.size(centerDisk)
                        ) {}
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
    onOpenFastingPlans: () -> Unit = {},
    fastingStartText: String? = null,
    fastingEndText: String? = null,
    planOverride: String? = null,
    fastingEnabled: Boolean = false,
    onToggle: (Boolean) -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val commonTopBarHeight = 30.dp
        val commonTopBarTextStyle = MaterialTheme.typography.labelMedium

        // === 左卡：Weight（新元件）
        val goal = -summary.weightDiffSigned
        val unit = summary.weightDiffUnit
        val primaryText =
            if (unit == "lbs") String.format(java.util.Locale.getDefault(), "%+d lbs", goal.roundToInt())
            else String.format(java.util.Locale.getDefault(), "%+.1f %s", goal, unit)

        WeightCardNew(
            primary = primaryText,
            secondary = "to goal",
            ringColor = Color(0xFF06B6D4),
            progress = 0f,
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight,
            ringSize = 74.dp,
            ringStroke = 6.dp,
            centerDisk = 32.dp,
            topBarTitle = "Weight",
            topBarHeight = commonTopBarHeight,
            topBarTextStyle = commonTopBarTextStyle,
            // ★ 放大到 19sp，並往上移 4dp，減少上方空隙到 4dp
            primaryFontSize = 19.sp,
            primaryYOffset = (-6).dp,
            primaryTopSpacing = 4.dp,
            secondaryYOffset = (-6).dp,        // ★ secondary 往上
            gapPrimaryToSecondary = 0.dp       // 可視覺需要把間距縮小
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
            topBarHeight = commonTopBarHeight,           // ★ 更薄
            topBarTextStyle = commonTopBarTextStyle, // 更低調一點
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
