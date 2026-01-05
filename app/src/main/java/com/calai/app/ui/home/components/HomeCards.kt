package com.calai.app.ui.home.components

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.data.activity.model.DailyActivityStatus
import com.calai.app.data.activity.util.ActivityKcalEstimator
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.ui.home.ui.fasting.components.FastingPlanCard
import com.calai.app.ui.home.ui.fasting.components.WeightCardNew
import kotlin.math.max
import kotlin.math.roundToInt

// 統一圓環尺寸（與「蛋白質」卡相同）
private object RingDefaults {
    val Size = 66.dp      // 圓直徑
    val Stroke = 5.dp     // 圓環粗細
    val CenterDisk = 34.dp// 圓心淺灰底大小
}

// ✅ Steps / Workout 圓環色票（依你需求：Steps 淺藍、Workout 深藍）
private object ActivityRingColors {
    val StepsLightBlue = Color(0xFF60A5FA)  // 淺藍
    val WorkoutDeepBlue = Color(0xFF2563EB) // 深藍
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
                Text("Calories left", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
            }
            Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                GaugeRing(
                    progress = progress,
                    sizeDp = ringSize,
                    strokeDp = ringStroke,
                    trackColor = Color(0xFFEFF0F3),
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
                // 🔥 圖片：火焰 icon 疊在灰圓上
                Image(
                    painter = painterResource(R.drawable.fire),
                    contentDescription = "Fire",
                    modifier = Modifier.size(24.dp)
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
                    modifier = Modifier.size(21.dp)
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
                    modifier = Modifier.size(30.dp)
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
                    modifier = Modifier.size(24.dp)
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
                color = Color.Black
            )
            Spacer(Modifier.height(spacingTop))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GaugeRing(
                    progress = progress,
                    sizeDp = ringSize,
                    strokeDp = ringStroke,
                    trackColor = Color(0xFFEFF0F3),
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

private const val WORKOUT_RING_GOAL_KCAL: Int = 400

private fun progressOfLong(current: Long?, goal: Long?): Float {
    val c = current ?: return 0f
    val g = (goal ?: 0L)
    if (g <= 0L) return 0f
    return (c.toFloat() / g.toFloat()).coerceIn(0f, 1f)
}

private fun progressOfInt(current: Int?, goal: Int): Float {
    val c = current ?: return 0f
    val g = max(goal, 1)
    return (c.toFloat() / g.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun StepsWorkoutRowModern(
    summary: HomeSummary,
    workoutTotalKcalOverride: Int? = null,
    stepsOverride: Long? = null,
    activeKcalOverride: Int? = null,
    weightKgLatest: Double? = null,
    dailyStatus: DailyActivityStatus = DailyActivityStatus.AVAILABLE_GRANTED,
    onDailyCtaClick: (() -> Unit)? = null,
    stepsGoalOverride: Long? = null,
    cardHeight: Dp = 120.dp,
    ringSize: Dp = 74.dp,
    centerDisk: Dp = 38.dp,
    ringStroke: Dp = 6.dp,
    onAddWorkoutClick: () -> Unit,
    onWorkoutCardClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val activityPrimaryStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

        val canShowLive = dailyStatus == DailyActivityStatus.AVAILABLE_GRANTED
        val steps: Long? = if (canShowLive) stepsOverride else null

        // 建議：把 — 也資源化（可選，但我建議做）
        val dash = stringResource(R.string.common_dash)

        // primary
        val stepsPrimary = when {
            canShowLive -> (steps?.toString() ?: dash)
            dailyStatus == DailyActivityStatus.NO_DATA -> dash
            dailyStatus == DailyActivityStatus.PERMISSION_NOT_GRANTED ->
                stringResource(R.string.steps_status_permission_not_granted)
            dailyStatus == DailyActivityStatus.HC_NOT_INSTALLED ->
                stringResource(R.string.steps_status_hc_not_installed)
            dailyStatus == DailyActivityStatus.HC_UNAVAILABLE ->
                stringResource(R.string.steps_status_hc_unavailable)
            else -> stringResource(R.string.common_error)
        }

        // secondary
        val stepsSecondary = when {
            canShowLive && activeKcalOverride != null ->
                stringResource(R.string.steps_secondary_est_kcal, activeKcalOverride)

            canShowLive && steps != null && weightKgLatest != null -> {
                val kcal = ActivityKcalEstimator.estimateActiveKcal(weightKgLatest, steps)
                stringResource(R.string.steps_secondary_est_kcal, kcal)
            }
            canShowLive -> dash
            dailyStatus == DailyActivityStatus.NO_DATA ->
                stringResource(R.string.steps_secondary_no_data_yet)
            else -> stringResource(R.string.steps_secondary_connect)
        }

        // ✅ 只在「未授權」與「未安裝」時顯示提示小卡
        val hintText: String? = when (dailyStatus) {
            DailyActivityStatus.PERMISSION_NOT_GRANTED ->
                stringResource(R.string.steps_hint_connect_google_health)

            DailyActivityStatus.HC_NOT_INSTALLED ->
                stringResource(R.string.steps_hint_install_health_connect)

            DailyActivityStatus.HC_UNAVAILABLE ->
                stringResource(R.string.steps_hint_hc_unavailable)

            DailyActivityStatus.ERROR_RETRYABLE ->
                stringResource(R.string.steps_hint_retry)

            else -> null
        }

        val hintIconRes = when (dailyStatus) {
            DailyActivityStatus.PERMISSION_NOT_GRANTED -> R.drawable.google_health
            DailyActivityStatus.HC_NOT_INSTALLED -> R.drawable.health_connect_logo
            DailyActivityStatus.HC_UNAVAILABLE -> R.drawable.health_connect_logo
            DailyActivityStatus.ERROR_RETRYABLE -> R.drawable.google_health
            else -> R.drawable.google_health
        }

        // ✅ Steps 圓環進度：100% = daily_step_goal（只有可用時才算）
        val stepsProgress = if (canShowLive) progressOfLong(steps, stepsGoalOverride) else 0f

        ActivityStatCardSplit(
            title = "Steps",
            primary = stepsPrimary,
            secondary = stepsSecondary,
            ringColor = ActivityRingColors.StepsLightBlue,
            progress = stepsProgress,
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight,
            ringSize = ringSize,
            ringStroke = ringStroke,
            centerDisk = centerDisk,
            gapPrimaryToSecondary = 4.dp,
            ringCenterContent = {
                Image(
                    painter = painterResource(R.drawable.footstep),
                    contentDescription = "Footstep",
                    modifier = Modifier.size(22.dp)
                )
            },
            onCardClick = onDailyCtaClick, // ✅ 降級時可導去授權/安裝

            blurBackground = (hintText != null),
            overlay = hintText?.let { text ->
                {
                    StepsConnectHintCard(
                        text = text,
                        modifier = Modifier.fillMaxWidth(0.79f),
                        minHeight = 78.dp,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,                  // ✅ 字大小
                            fontWeight = FontWeight.Medium,    // ✅ 粗度
                            lineHeight = 15.sp                 // ✅ 行高（可選）
                            // letterSpacing = 0.1.sp          // ✅ 字距（可選）
                        ),
                        icon = {
                            Image(
                                painter = painterResource(hintIconRes),
                                contentDescription = "Google Health",
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(26.dp)
                            )
                        },
                        onClick = onDailyCtaClick
                    )
                }
            }
        )

        // ===== Workout =====
        val workoutKcal: Int? = workoutTotalKcalOverride
            ?: summary.todayActivity.activeKcal?.roundToInt()

        val workoutPrimary = workoutKcal?.toString() ?: "—"
        val workoutProgress = progressOfInt(workoutKcal, WORKOUT_RING_GOAL_KCAL)// ✅ Workout 圓環進度：100% 固定 400 kcal

        ActivityStatCardSplit(
            title = "Workout",
            primary = workoutPrimary,
            secondary = null,
            ringColor = ActivityRingColors.WorkoutDeepBlue,
            progress = workoutProgress,
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight,
            ringSize = ringSize,
            ringStroke = ringStroke,
            centerDisk = centerDisk,
            primaryTextStyle = activityPrimaryStyle,
            ringCenterContent = {
                Image(
                    painter = painterResource(R.drawable.fitness),
                    contentDescription = "Dumbbell",
                    modifier = Modifier.size(26.dp)
                )
            },
            primaryContent = workoutKcal?.let {
                { WorkoutPrimaryText(kcal = it, numberStyle = activityPrimaryStyle) }
            },
            leftExtra = {
                Box(modifier = Modifier.offset(x = (-4).dp, y = (2).dp)) {
                    WorkoutAddButton(
                        onClick = onAddWorkoutClick,
                        outerSizeDp = 34.dp,
                        innerSizeDp = 26.dp,
                        iconSizeDp = 21.dp
                    )
                }
            },
            onCardClick = onWorkoutCardClick
        )
    }
}

/**
 * ✅ Workout 專用 primary：數字大/粗，kcal 小/細
 * - 不會影響 Steps，因為 Steps 不會用 primaryContent
 */
@Composable
private fun WorkoutPrimaryText(
    kcal: Int,
    numberStyle: TextStyle
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = kcal.toString(),
            style = numberStyle,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(5.dp))

        Text(
            text = "kcal",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                baselineShift = BaselineShift(0.28f)
            ),
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    modifier: Modifier = Modifier,
    title: String,
    primary: String,
    secondary: String? = null,
    ringColor: Color,
    progress: Float = 0f,
    cardHeight: Dp = 120.dp,
    ringSize: Dp = 74.dp,
    ringStroke: Dp = 6.dp,
    centerDisk: Dp = 38.dp,
    drawRing: Boolean = true,
    ringCenterContent: (@Composable () -> Unit)? = null,
    titlePrefix: (@Composable () -> Unit)? = null,
    titlePrefixGap: Dp = 4.dp,
    titleTextStyle: TextStyle? = null,
    primaryTextStyle: TextStyle? = null,
    secondaryTextStyle: TextStyle? = null,
    gapTitleToPrimary: Dp = 4.dp,
    gapPrimaryToSecondary: Dp = 2.dp,
    leftExtra: (@Composable () -> Unit)? = null,
    primaryContent: (@Composable () -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,

    // ✅ 模糊/提示狀態
    blurBackground: Boolean = false,

    // ✅ 建議值：比你原本更接近圖片（「輕微」）
    blurRadiusWhenOn: Dp = 1.dp,
    dimAlphaWhenOn: Float = 0.88f,

    // ✅ 霧面感（白色薄紗），更像你圖
    scrimAlphaWhenOn: Float = 0.1f,

    overlay: (@Composable () -> Unit)? = null
) {
    val titleStyle = titleTextStyle ?: MaterialTheme.typography.bodySmall
    val primaryStyle =
        primaryTextStyle ?: MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val secondaryStyle = secondaryTextStyle ?: MaterialTheme.typography.bodySmall

    val interaction = remember { MutableInteractionSource() }
    val clickableMod = if (onCardClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null
        ) { onCardClick() }
    } else Modifier

    // ✅ 動畫化（切換更順）
    val animBlur by animateDpAsState(
        targetValue = if (blurBackground) blurRadiusWhenOn else 0.dp,
        label = "stepsBlur"
    )
    val animAlpha by animateFloatAsState(
        targetValue = if (blurBackground) dimAlphaWhenOn else 1f,
        label = "stepsDimAlpha"
    )
    val animScrim by animateFloatAsState(
        targetValue = if (blurBackground) scrimAlphaWhenOn else 0f,
        label = "stepsScrimAlpha"
    )

    fun Modifier.smartBlurAndDim(): Modifier {
        // 低版本不 blur，只 dim；31+ 才 blur
        val dimmed = this.graphicsLayer { alpha = animAlpha }
        return if (Build.VERSION.SDK_INT >= 31 && animBlur > 0.dp) dimmed.blur(animBlur) else dimmed
    }

    Card(
        modifier = modifier
            .then(clickableMod)
            .height(cardHeight)
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        border = CardStyles.Border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ===== 底層內容（必要時 blur + dim）=====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .then(if (blurBackground) Modifier.smartBlurAndDim() else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (titlePrefix != null) {
                                titlePrefix()
                                Spacer(Modifier.width(titlePrefixGap))
                            }
                            Text(
                                text = title,
                                style = titleStyle,
                                color = Color(0xFF111114),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(gapTitleToPrimary))

                        if (primaryContent != null) {
                            primaryContent()
                        } else {
                            Text(
                                text = primary,
                                style = primaryStyle,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(gapPrimaryToSecondary))

                        if (!secondary.isNullOrBlank()) {
                            Text(
                                text = secondary,
                                style = secondaryStyle,
                                color = Color(0xFF111114),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Spacer(Modifier.height(18.dp))
                        }
                    }

                    leftExtra?.let { extra ->
                        Box(modifier = Modifier.align(Alignment.BottomStart)) { extra() }
                    }
                }

                // 右側圓環
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
                                trackColor = Color(0xFFEFF0F3),
                                progressColor = ringColor,
                                drawTopTick = true,
                                tickColor = ringColor
                            )
                            Surface(
                                color = RingColors.CenterFill,
                                shape = CircleShape,
                                modifier = Modifier.size(centerDisk)
                            ) {}
                            ringCenterContent?.invoke()
                        } else {
                            Spacer(Modifier.size(ringSize))
                        }
                    }
                }
            }

            // ✅ scrim：放在「底層」上方、overlay 下方（更像你圖）
            if (animScrim > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = animScrim))
                )
            }

            // ===== 上層提示卡：不模糊 =====
            if (overlay != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    overlay()
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
    onToggle: (Boolean) -> Unit = {},
    weightPrimary: String,
    weightProgress: Float,
    onOpenWeight: () -> Unit,
    onQuickLogWeight: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val commonTopBarHeight = 30.dp
        val commonTopBarTextStyle = MaterialTheme.typography.labelMedium

        // === 左卡：Weight（新元件）
        WeightCardNew(
            primary = weightPrimary,
            secondary = "to goal",  // "p=${(weightProgress * 100).toInt()}%",
            ringColor = Color(0xFF06B6D4),
            progress = weightProgress,
            modifier = Modifier
                .weight(1f)
                .height(cardHeight)
                .clickable { onOpenWeight() }, // ★ 整張卡片可點
            cardHeight = cardHeight,
            ringSize = 74.dp,
            ringStroke = 6.dp,
            centerDisk = 40.dp,
            topBarTitle = "Weight",
            topBarHeight = commonTopBarHeight,
            topBarTextStyle = commonTopBarTextStyle,
            primaryFontSize = 19.sp,
            primaryYOffset = (-6).dp,
            primaryTopSpacing = 4.dp,
            secondaryYOffset = (-5).dp,
            gapPrimaryToSecondary = 0.dp,
            onAddWeightClick = onQuickLogWeight        // ★ 按「＋」直接開記錄頁
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
    height: Dp = 38.dp,
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
