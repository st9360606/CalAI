package com.calai.bitecal.ui.home.ui.weight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.data.weight.api.WeightItemDto
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.ui.weight.model.WeightViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import com.calai.bitecal.data.profile.repo.kgToLbs1
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.calai.bitecal.BuildConfig
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable

private const val WEIGHT_RANGE_30_DAYS = "30d"
private const val WEIGHT_RANGE_60_DAYS = "60d"
private const val WEIGHT_RANGE_90_DAYS = "90d"
private const val WEIGHT_RANGE_ALL_TIME = "all"
private const val WEIGHT_CHART_MAX_X_LABELS = 5
private const val WEIGHT_CHART_PADDING_RATIO = 0.04
private const val WEIGHT_CHART_MIN_SPAN_KG = 1.0
private val WeightChartHeight: Dp = 248.dp
private val WeightChartCardHorizontalPadding: Dp = 19.dp
private val WeightChartCardVerticalPadding: Dp = 20.dp
private val WeightChartHeaderToChartSpacing: Dp = 16.dp
private val WeightChartChartToBannerSpacing: Dp = 14.dp
private val WeightChartYAxisLabelWidth: Dp = 42.dp
private val WeightChartYAxisLabelGap: Dp = 10.dp
private val WeightChartXAxisLabelGap: Dp = 0.dp
private val WeightChartXAxisLabelHeight: Dp = 18.dp
private val WeightChartMotivationBannerMaxWidth: Dp = 320.dp
private const val WEIGHT_CHART_MOTIVATION_MAX_LINES = 2
private val WeightChartGroupHorizontalOffset: Dp = (-4).dp
private val NUM_LOCALE: Locale = Locale.US

// ----------------------------------------------------------
// Summary Cards
// ----------------------------------------------------------
val Green = Color(0xFF22C55E)
@Composable
fun WeightComponents(ui: WeightViewModel.UiState) {
    val label   = Color.Black.copy(alpha = 0.60f)
    val big     = Color(0xFF111114)
    val divider = Color(0xFFE2E5EA)
    val trackColor  = Color(0xFFDCE1E7)
    val stripeColor = Color.White.copy(alpha = 0.35f)
    val fillColor   = Color(0xFFFF8A33).copy(alpha = 0.81f)

    val unit = ui.unit

    val currentKg  = ui.current ?: ui.profileWeightKg
    val currentLbs = ui.currentLbs ?: ui.profileWeightLbs

    // ---------- 目標體重：一律用 DB user_profiles（SummaryDto.goal*） ----------
    val goalKg  = ui.goal      // ★ DB goal_weight_kg
    val goalLbs = ui.goalLbs   // ★ DB goal_weight_lbs

    // TO GOAL WEIGHT：還是用 kg 為基準算差值（顯示時再依單位轉）
    val gainedText = formatDeltaGoalMinusCurrentFromDb(
        goalKg = goalKg,
        goalLbs = goalLbs,
        currentKg = currentKg,
        currentLbs = currentLbs,
        unit = unit,
        lbsAsInt = false
    )

    // 進度：依目前 range 的 timeseries 算
    val pr = computeWeightProgress(
        timeSeries      = ui.series,
        currentKg       = currentKg,
        goalKg          = goalKg,
        profileWeightKg = ui.profileWeightKg
    )
    val progress = pr.fraction

    // ---------- edgeLeft：只拿 user_profiles table 的原始體重 ----------
    // 這裡完全不看 timeseries / summary，只看 Profile 的 weight_kg / weight_lbs
    val edgeLeft = formatWeightFromDb(
        kg  = ui.profileWeightKg,
        lbs = ui.profileWeightLbs,
        unit = unit
    )

    // ---------- edgeRight：只拿 DB 目標體重（Summary / user_profiles.goal*） ----------
    val edgeRight = formatWeightFromDb(
        kg  = goalKg,
        lbs = goalLbs,
        unit = unit
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(CardStyles.Border, CardStyles.Corner),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        elevation = CardDefaults.cardElevation(defaultElevation = CardStyles.Elevation)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左欄：TO GOAL WEIGHT
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    UpperLabel(
                        text = stringResource(R.string.weight_card_to_goal_weight),
                        color = label,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = gainedText,
                        color = big,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 右欄：CURRENT WEIGHT
                // 右欄：CURRENT WEIGHT
                Column(
                    modifier = Modifier
                        .weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    UpperLabel(
                        text = stringResource(R.string.weight_card_current_weight),
                        color = label,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    val currentText = formatWeightFromDb(
                        kg  = currentKg,
                        lbs = currentLbs,
                        unit = unit
                    )

                    Text(
                        text = currentText,
                        color = big,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = divider, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            val achievedFractionForLabel: Float =
                if (unit == UserProfileStore.WeightUnit.KG) {
                    progress
                } else {
                    // ✅ LBS 模式：用 DB lbs 算 ACHIEVED（資料不足就 fallback 回 kg）
                    computeWeightProgressFractionLbs(
                        timeSeries = ui.series,
                        currentLbs = currentLbs,
                        goalLbs = goalLbs,
                        profileWeightLbs = ui.profileWeightLbs
                    ) ?: progress
                }

            UpperLabel(
                text = stringResource(
                    R.string.weight_card_achieved_goal_format,
                    formatAchievedPercent1(achievedFractionForLabel)
                ),
                color = label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )


            Spacer(Modifier.height(10.dp))
            HatchedProgressBar(
                progress    = progress,
                trackColor  = trackColor,
                stripeColor = stripeColor,
                fillColor   = fillColor,
                height      = 36.dp,
                corner      = 12.dp,
                stripeWidth = 8.dp,
                stripeGap   = 6.dp,
                stripeAngle = -27f
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = edgeLeft,
                    color = label,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text("→", color = label, fontWeight = FontWeight.SemiBold)
                Text(
                    text = edgeRight,
                    color = label,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun UpperLabel(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
fun HatchedProgressBar(
    progress: Float,
    trackColor: Color = Color(0xFF2D2F32),
    stripeColor: Color = Color.White.copy(alpha = 0.18f),
    fillColor: Color = Color(0xFFFF8A33).copy(alpha = 0.85f),
    height: Dp = 40.dp,
    corner: Dp = 12.dp,
    stripeWidth: Dp = 8.dp,
    stripeGap: Dp = 6.dp,
    stripeAngle: Float = -27f,
    stripePhasePx: Float = 0f
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(Modifier
        .fillMaxWidth()
        .height(height)) {
        Canvas(Modifier.matchParentSize()) {
            val r = corner.toPx()
            val cr = CornerRadius(r, r)

            // 底軌
            drawRoundRect(color = trackColor, cornerRadius = cr, size = size)

            // 斜紋
            val rr = RoundRect(0f, 0f, size.width, size.height, cr)
            val clip = Path().apply { addRoundRect(rr) }
            val sw = stripeWidth.toPx()
            val gap = stripeGap.toPx()
            val period = sw + gap
            val diag = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
            withTransform({
                clipPath(clip)
                rotate(degrees = stripeAngle, pivot = center)
            }) {
                var x = -diag + (stripePhasePx % period)
                val maxX = size.width + diag
                val rectHeight = diag * 2f
                while (x < maxX) {
                    drawRect(
                        color = stripeColor,
                        topLeft = Offset(x, -diag),
                        size = Size(sw, rectHeight)
                    )
                    x += period
                }
            }

            // 內側高光/陰影
            withTransform({ clipPath(clip) }) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.06f),
                        0.6f to Color.Transparent
                    ),
                    cornerRadius = cr,
                    size = size
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.06f)
                    ),
                    cornerRadius = cr,
                    size = size
                )
            }

            // 進度填充
            if (clamped > 0f) {
                drawRoundRect(
                    color = fillColor,
                    cornerRadius = cr,
                    size = Size(size.width * clamped, size.height)
                )
            }

            // 1px 內框
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                cornerRadius = cr,
                style = Stroke(width = 1f)
            )
        }
    }
}

// ----------------------------------------------------------
// 共用格式化
// ----------------------------------------------------------

// ----------------------------------------------------------
// 共用格式化（所有 lbs 一律走 kgToLbs1，共用轉換邏輯）
// ----------------------------------------------------------

fun formatWeightCard(
    kg: Double?,
    unit: UserProfileStore.WeightUnit,
    lbsAsInt: Boolean
): String {
    if (kg == null) return "—"
    return if (unit == UserProfileStore.WeightUnit.KG) {
        // KG 模式：顯示到小數點一位
        String.format(NUM_LOCALE, "%.1f kg", kg)
    } else {
        // LBS 模式：一律走共用的 kgToLbs1，確保跟 Record / 後端一致
        val lbs = kgToLbs1(kg)
        if (lbsAsInt) {
            // 只要整數：TO GOAL WEIGHT / axis label 等場合你想顯示 176 lbs 這種
            String.format(NUM_LOCALE, "%d lbs", lbs.toInt())
        } else {
            // 顯示到小數點一位：如 CURRENT WEIGHT、TO GOAL WEIGHT 主數字
            String.format(NUM_LOCALE, "%.1f lbs", lbs)
        }
    }
}

// ----------------------------------------------------------
// Filter Tabs
// ----------------------------------------------------------

private val WeightFilterTabsContainerBg = Color(0xFFF1F1F3)
private val WeightFilterTabsActiveBg = Color(0xFFFFFFFF)
private val WeightFilterTabsActiveBorder = Color(0xFFE2E2E6)
private val WeightFilterTabsActiveText = Color(0xFF2D2F35)
private val WeightFilterTabsIdleText = Color(0xFF3A3D43)

private data class RangeTab(
    val key: String,
    val labelRes: Int
)

@Composable
fun FilterTabs(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        RangeTab(WEIGHT_RANGE_30_DAYS, R.string.weight_chart_tab_30_days),
        RangeTab(WEIGHT_RANGE_60_DAYS, R.string.weight_chart_tab_60_days),
        RangeTab(WEIGHT_RANGE_90_DAYS, R.string.weight_chart_tab_90_days),
        RangeTab(WEIGHT_RANGE_ALL_TIME, R.string.weight_chart_tab_all_time)
    )

    val selectedIndex = tabs.indexOfFirst { it.key == selected }.let {
        if (it >= 0) it else 0
    }

    val containerShape = RoundedCornerShape(18.dp)
    val activeTabShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WeightFilterTabsContainerBg, containerShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(
                                    elevation = 1.5.dp,
                                    shape = activeTabShape,
                                    clip = false
                                )
                                .background(WeightFilterTabsActiveBg, activeTabShape)
                                .border(1.dp, WeightFilterTabsActiveBorder, activeTabShape)
                        } else {
                            Modifier.background(Color.Transparent, activeTabShape)
                        }
                    )
                    .biteCalClickable { onSelect(tab.key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    color = if (isSelected) {
                        WeightFilterTabsActiveText
                    } else {
                        WeightFilterTabsIdleText
                    },
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ----------------------------------------------------------
// Weight Chart Card
// ----------------------------------------------------------

@Composable
fun WeightChartCard(
    ui: WeightViewModel.UiState,
    onEditGoalWeight: () -> Unit       // ★ 新增
) {
    val unit          = ui.unit
    val currentKg     = ui.current ?: ui.profileWeightKg
    val goalKg        = ui.goal              // ★ 只用 DB user_profiles.goal_weight_kg
    val profileWeight = ui.profileWeightKg   // 起點仍然用 DB 的 current weight

    val progressFraction = computeWeightProgress(
        timeSeries       = ui.series,
        currentKg        = currentKg,
        goalKg           = goalKg,
        profileWeightKg  = profileWeight
    ).fraction

    val progressPercent = (progressFraction * 100f)
        .toInt()
        .coerceIn(0, 100)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(CardStyles.Border, CardStyles.Corner),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        elevation = CardDefaults.cardElevation(defaultElevation = CardStyles.Elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WeightChartCardHorizontalPadding, vertical = WeightChartCardVerticalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.weight_chart_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF111114),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                GoalProgressBadge(
                    progressPercent = progressPercent,
                    onClick = onEditGoalWeight
                )
            }

            Spacer(Modifier.height(WeightChartHeaderToChartSpacing))

            GoalProgressChart(
                series = ui.series,
                unit = unit,
                currentKg = currentKg,
                goalKg = goalKg,
                modifier = Modifier.offset(x = WeightChartGroupHorizontalOffset)
            )

            Spacer(Modifier.height(WeightChartChartToBannerSpacing))

            MotivationBanner(
                text = stringResource(R.string.weight_chart_motivation),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun GoalProgressBadge(
    progressPercent: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit         // ★ 新增 callback
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFFFF7ED))
            .border(
                width = 1.dp,
                color = Color(0xFFF59E0B).copy(alpha = 0.36f),
                shape = RoundedCornerShape(999.dp)
            )
            .biteCalClickable { onClick() }    // ★ 讓整個膠囊可以點
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFD97706)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(
                R.string.weight_chart_goal_progress_badge,
                progressPercent.coerceIn(0, 100)
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFB45309)
        )
        // ★ 文字後面的鉛筆圖示
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.weight_chart_edit_goal_content_description),
            modifier = Modifier.size(13.dp),
            tint = Color(0xFFD97706)
        )
    }
}

// ----------------------------------------------------------
// Chart 內部資料結構
// ----------------------------------------------------------

private data class ChartPointNormalized(
    val x: Float, // 0f = 最舊日期, 1f = 最新日期
    val y: Float  // 0f = Y 軸頂端, 1f = Y 軸底端
)

private data class WeightChartData(
    val yLabels: List<String>,
    val xLabels: List<String>,
    val points: List<ChartPointNormalized>,
    val dates: List<LocalDate>,
    val weightsKg: List<Double>,
    val weightsLbs: List<Double?>,
    val axisDates: List<LocalDate>,
    val axisX: List<Float>,
    val topKg: Double,
    val bottomKg: Double,
    val goalPointY: Float?
)

// X 軸日期格式（軸上字）
private val axisDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMd", Locale.ENGLISH)

// Tooltip 用日期格式（底下黑色氣泡）
private val tooltipDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

private data class RawWeightPoint(
    val date: LocalDate,
    val kg: Double,
    val lbs: Double?
)

private fun buildEmptyWeightChartData(
    unit: UserProfileStore.WeightUnit,
    currentKg: Double?,
    goalKg: Double?
): WeightChartData {
    val scale = resolveWeightChartScale(
        weightsKg = emptyList(),
        currentKg = currentKg,
        goalKg = goalKg
    )
    val topKg = scale.first
    val bottomKg = scale.second
    val span = (topKg - bottomKg).coerceAtLeast(1e-6)

    val yLabels = buildYAxisLabels(
        topKg = topKg,
        bottomKg = bottomKg,
        unit = unit
    )

    return WeightChartData(
        yLabels = yLabels,
        xLabels = emptyList(),
        points = emptyList(),
        dates = emptyList(),
        weightsKg = emptyList(),
        weightsLbs = emptyList(),
        axisDates = emptyList(),
        axisX = emptyList(),
        topKg = topKg,
        bottomKg = bottomKg,
        goalPointY = goalKg?.let { goal ->
            (((topKg - goal.coerceIn(bottomKg, topKg)) / span).toFloat()).coerceIn(0f, 1f)
        }
    )
}
/**
 * 建立圖表資料
 */
private fun buildWeightChartData(
    series: List<WeightItemDto>,
    unit: UserProfileStore.WeightUnit,
    currentKg: Double?,
    goalKg: Double?
): WeightChartData {
    if (series.isEmpty()) {
        return buildEmptyWeightChartData(
            unit = unit,
            currentKg = currentKg,
            goalKg = goalKg
        )
    }

    // 1) 解析日期 + 排序（同時保留 kg / lbs）
    val sorted: List<RawWeightPoint> = series.mapNotNull { item ->
        runCatching { LocalDate.parse(item.logDate) }.getOrNull()
            ?.let { date ->
                RawWeightPoint(date = date, kg = item.weightKg, lbs = item.weightLbs)
            }
    }.sortedBy { it.date }

    if (sorted.isEmpty()) {
        return buildEmptyWeightChartData(
            unit = unit,
            currentKg = currentKg,
            goalKg = goalKg
        )
    }

    val datesSorted: List<LocalDate> = sorted.map { it.date }
    val weightsKg: List<Double> = sorted.map { it.kg }
    val weightsLbs: List<Double?> = sorted.map { it.lbs }

    val scale = resolveWeightChartScale(
        weightsKg = weightsKg,
        currentKg = currentKg,
        goalKg = goalKg
    )
    val topKg = scale.first
    val bottomKg = scale.second

    val span = (topKg - bottomKg).coerceAtLeast(1e-6)

    // 4) Y / X 標籤
    val yLabels = buildYAxisLabels(topKg, bottomKg, unit)

    val axisIndexes = buildXAxisIndexes(datesSorted.size)
    val axisDates = axisIndexes.map { datesSorted[it] }
    val xLabels = axisDates.map { axisDateFormatter.format(it) }

    val points: List<ChartPointNormalized> = weightsKg.mapIndexed { index, wKg ->
        val clamped = wKg.coerceIn(bottomKg, topKg)
        val y = (((topKg - clamped) / span).toFloat()).coerceIn(0f, 1f)
        ChartPointNormalized(
            x = xForDataIndex(index, datesSorted.size),
            y = y
        )
    }

    val axisX: List<Float> = axisIndexes.map { index ->
        xForDataIndex(index, datesSorted.size)
    }

    return WeightChartData(
        yLabels   = yLabels,
        xLabels   = xLabels,
        points    = points,
        dates     = datesSorted,
        weightsKg = weightsKg,
        weightsLbs = weightsLbs,
        axisDates = axisDates,
        axisX     = axisX,
        topKg = topKg,
        bottomKg = bottomKg,
        goalPointY = goalKg?.let { goal ->
            (((topKg - goal.coerceIn(bottomKg, topKg)) / span).toFloat()).coerceIn(0f, 1f)
        }
    )
}

private fun resolveWeightChartScale(
    weightsKg: List<Double>,
    currentKg: Double?,
    goalKg: Double?
): Pair<Double, Double> {
    val values = buildList {
        weightsKg.filterTo(this) { it.isFinite() }
        currentKg?.takeIf { it.isFinite() }?.let(::add)
        goalKg?.takeIf { it.isFinite() }?.let(::add)
    }

    if (values.isEmpty()) {
        val base = currentKg ?: goalKg ?: 70.0
        return (base + 1.0) to (base - 1.0).coerceAtLeast(1.0)
    }

    var minKg = values.minOrNull()!!
    var maxKg = values.maxOrNull()!!
    if (abs(maxKg - minKg) < WEIGHT_CHART_MIN_SPAN_KG) {
        val center = (maxKg + minKg) / 2.0
        minKg = center - WEIGHT_CHART_MIN_SPAN_KG / 2.0
        maxKg = center + WEIGHT_CHART_MIN_SPAN_KG / 2.0
    }

    val span = (maxKg - minKg).coerceAtLeast(WEIGHT_CHART_MIN_SPAN_KG)
    val paddedMin = minKg - span * WEIGHT_CHART_PADDING_RATIO
    val paddedMax = maxKg + span * WEIGHT_CHART_PADDING_RATIO
    val step = niceAxisStep((paddedMax - paddedMin) / 4.0)
    val bottom = floor(paddedMin / step) * step
    val top = ceil(paddedMax / step) * step
    return top to bottom.coerceAtLeast(0.0)
}

private fun niceAxisStep(rawStep: Double): Double {
    if (!rawStep.isFinite() || rawStep <= 0.0) return 1.0
    val exponent = floor(kotlin.math.log10(rawStep))
    val magnitude = Math.pow(10.0, exponent)
    val normalized = rawStep / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

/** Y 軸刻度：最多 5 個；必定包含頂端與底端。 */
private fun buildYAxisLabels(
    topKg: Double,
    bottomKg: Double,
    unit: UserProfileStore.WeightUnit
): List<String> {
    val labels = mutableListOf<Double>()
    val span = (topKg - bottomKg).coerceAtLeast(1e-6)

    labels += topKg
    val middleSlots = 3
    for (i in 1..middleSlots) {
        val t = i / (middleSlots + 1.0) // 1/4, 2/4, 3/4
        val v = topKg - t * span
        labels += v
    }
    labels += bottomKg

    val dedup = labels
        .sortedDescending()
        .distinctBy { "%.2f".format(it) }

    val final = if (dedup.size <= 5) dedup else dedup.take(5)

    return final.map { kg -> formatAxisWeightLabel(kg, unit) }
}

/** X 軸：從 minDate~maxDate 等分 maxLabels 個刻度（不依賴資料分佈） */
private fun buildXAxisIndexes(pointCount: Int): List<Int> {
    if (pointCount <= 0) return emptyList()
    val labelCount = minOf(pointCount, WEIGHT_CHART_MAX_X_LABELS)
    if (labelCount == 1) return listOf(0)
    val lastIndex = pointCount - 1
    return (0 until labelCount)
        .map { i -> ((i * lastIndex.toFloat()) / (labelCount - 1)).roundToInt() }
        .map { it.coerceIn(0, lastIndex) }
        .distinct()
}

private fun xForDataIndex(index: Int, pointCount: Int): Float {
    return if (pointCount <= 1) 0f else (index.toFloat() / (pointCount - 1)).coerceIn(0f, 1f)
}


    // ✅ 範圍太短：直接每天列出（<= maxLabels 個，不會重複）
    // 例：只有 11/24~11/26，硬切 5 份只會得到重複日期，反而難看。

    // ✅ 範圍夠長：平均取樣 maxLabels 個日期，首尾必定是 min/max

private fun formatAxisWeightLabel(
    kg: Double,
    unit: UserProfileStore.WeightUnit
): String {
    return formatWeightCard(
        kg = kg,
        unit = unit,
        lbsAsInt = (unit == UserProfileStore.WeightUnit.LBS)
    )
}

/**
 * Tooltip：顯示當日體重 + 日期（黑底氣泡）
 */
@Composable
private fun WeightTooltip(
    weightKg: Double,
    weightLbs: Double?,
    goalKg: Double?,
    unit: UserProfileStore.WeightUnit,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    key(unit, weightLbs, goalKg) {
        val weightText = formatTooltipWeight(
            weightKg = weightKg,
            weightLbs = weightLbs,
            unit = unit
        )
        val dateText = tooltipDateFormatter.format(date)
        val goalText = goalKg?.let { goal ->
            stringResource(
                R.string.weight_chart_to_goal,
                formatGoalDeltaWeight(
                    weightKg = weightKg,
                    weightLbs = weightLbs,
                    goalKg = goal,
                    unit = unit
                )
            )
        }

        Box(
            modifier = modifier
                .widthIn(min = 158.dp, max = 202.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF111114).copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = dateText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = weightText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (goalText != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = goalText,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 折線圖本體（drawWithCache 快取版）
 */
@Composable
private fun GoalProgressChart(
    series: List<WeightItemDto>,
    unit: UserProfileStore.WeightUnit,
    currentKg: Double?,
    goalKg: Double?,
    modifier: Modifier = Modifier
) {
    // ✅ 保留你原本預設尺寸，但允許 caller 覆蓋（caller 的 modifier 會在最後 then）
    val baseModifier = Modifier
        .fillMaxWidth()
        .height(WeightChartHeight)
        .then(modifier)

    val chartData = buildWeightChartData(
        series = series,
        unit = unit,
        currentKg = currentKg,
        goalKg = goalKg
    )

    var activeIndex by remember(chartData.points.size) { mutableStateOf<Int?>(null) }
    var pinnedIndex by remember(chartData.points.size) { mutableStateOf<Int?>(null) }

    LaunchedEffect(unit) {
        activeIndex = null
        pinnedIndex = null
    }

    val shownIndex: Int? = activeIndex ?: pinnedIndex

    var chartWidthPx by remember { mutableFloatStateOf(0f) }
    var chartHeightPx by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val startPaddingDp = WeightChartYAxisLabelWidth + WeightChartYAxisLabelGap
    val endPaddingDp = 8.dp
    val topPaddingDp = 10.dp
    val bottomPaddingDp = 10.dp

    val startPaddingPx = with(density) { startPaddingDp.toPx() }
    val endPaddingPx = with(density) { endPaddingDp.toPx() }
    val topPaddingPx = with(density) { topPaddingDp.toPx() }
    val bottomPaddingPx = with(density) { bottomPaddingDp.toPx() }

    val pointCentersPx = remember(chartData.points, chartWidthPx, startPaddingPx, endPaddingPx) {
        buildPointCentersPx(
            pointsXNorm = chartData.points.map { it.x },
            chartWidthPx = chartWidthPx,
            startPaddingPx = startPaddingPx,
            endPaddingPx = endPaddingPx
        )
    }

    fun pickIndex(rawX: Float): Int? = nearestIndexByX(pointCentersPx, rawX)

    fun setActive(idx: Int) {
        if (activeIndex != idx) activeIndex = idx
    }

    Column(modifier = baseModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged {
                    chartWidthPx = it.width.toFloat()
                    chartHeightPx = it.height.toFloat()
                }
                .pointerInput(chartData.points.size, chartWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downX = down.position.x
                        val downY = down.position.y

                        val downIdx = pickIndex(downX) ?: return@awaitEachGesture
                        setActive(downIdx)

                        var dragging = false
                        var cancelledByVertical = false
                        val slop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) break

                            val dx = change.position.x - downX
                            val dy = change.position.y - downY

                            if (!dragging) {
                                val dist2 = dx * dx + dy * dy
                                if (dist2 >= slop * slop) {
                                    if (abs(dx) >= abs(dy)) {
                                        dragging = true
                                    } else {
                                        cancelledByVertical = true
                                        break
                                    }
                                }
                            }

                            if (dragging) {
                                pickIndex(change.position.x)?.let { idx -> setActive(idx) }
                                change.consume()
                            }
                        }

                        if (!cancelledByVertical && !dragging) {
                            pinnedIndex = if (pinnedIndex == downIdx) null else downIdx
                        }

                        if (!cancelledByVertical && dragging && pinnedIndex != null) {
                            pinnedIndex = activeIndex ?: pinnedIndex
                        }

                        activeIndex = null
                    }
                }
        ) {
            // ✅ drawWithCache：Path/Brush/計算全部快取；拖曳只重畫 overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(
                        start = startPaddingDp,
                        end = endPaddingDp,
                        top = topPaddingDp,
                        bottom = bottomPaddingDp
                    )
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val r = 0f

                        val clipRound = CornerRadius(r, r)
                        val rrClip = RoundRect(0f, 0f, w, h, clipRound)
                        val clipPath = Path().apply { addRoundRect(rrClip) }

                        val gridCount = 4
                        val gridYs = (0..gridCount).map { i -> h * (i / gridCount.toFloat()) }

                        // Brushes（快取）
                        val bgBrush = Brush.verticalGradient(
                            0f to Color(0xFFECEFF3),
                            1f to Color(0xFFFAFAFA)
                        )
                        val baseAreaBrush = Brush.verticalGradient(
                            0f to Color(0xFF111114).copy(alpha = 0.18f),
                            1f to Color.Transparent
                        )
                        val rightAreaBrush = Brush.verticalGradient(
                            0f to Color(0xFF111114).copy(alpha = 0.16f),
                            0.55f to Color(0xFF111114).copy(alpha = 0.10f),
                            1f to Color.Transparent
                        )

                        val highlightGreen = Color(0xFF22C55E)
                        val baseStroke = 2.1.dp.toPx()
                        val halfStroke = baseStroke / 2f

                        val vLineStroke = 1.5.dp.toPx()
                        val gridStroke = 1.dp.toPx()
                        val goalDash = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                            0f
                        )

                        val circleOuter = 5.dp.toPx()
                        val circleInner = 3.dp.toPx()
                        val circleHalo = 8.dp.toPx()

                        // Points（快取）
                        val allPoints = chartData.points
                        val xsAll = allPoints.map { it.x * w }
                        val ysAll = allPoints.map { it.y * h }

                        // Paths（快取）
                        val linePathAll: Path? =
                            if (allPoints.size >= 2) buildClampedSmoothPath(xsAll, ysAll) else null

                        val areaPathAll: Path? =
                            if (linePathAll != null) {
                                Path().apply {
                                    addPath(linePathAll)
                                    lineTo(xsAll.last(), h)
                                    lineTo(xsAll.first(), h)
                                    close()
                                }
                            } else null

                        onDrawBehind {
                            withTransform({ clipPath(clipPath) }) {
                                // 背景
                                drawRoundRect(
                                    brush = bgBrush,
                                    cornerRadius = clipRound,
                                    size = size
                                )

                                // 網格
                                for (y in gridYs) {
                                    drawLine(
                                            color = Color(0xFFD8DEE6),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = gridStroke
                                    )
                                }

                                chartData.goalPointY?.let { goalYNorm ->
                                    val yGoal = goalYNorm * h
                                    drawLine(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.95f),
                                        start = Offset(0f, yGoal),
                                        end = Offset(w, yGoal),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = goalDash,
                                        cap = StrokeCap.Round
                                    )
                                }

                                if (allPoints.isEmpty()) return@withTransform

                                // ====== Base layer（不依賴 shownIndex，快取） ======
                                if (allPoints.size == 1) {
                                    val xSingle = xsAll[0]
                                    val ySingle = ysAll[0]

                                    // 灰底漸層
                                    // 基線：未按黑、按下綠（這段依賴 shownIndex，但只是 draw，不會重算 Path）
                                    val isActive = (shownIndex == 0)
                                    drawLine(
                                        color = if (isActive) highlightGreen else Color(0xFF111114),
                                        start = Offset(0f, ySingle),
                                        end = Offset(w, ySingle),
                                        strokeWidth = baseStroke,
                                        cap = StrokeCap.Round
                                    )

                                    drawCircle(
                                        color = if (isActive) highlightGreen else Color(0xFF111114),
                                        radius = circleOuter,
                                        center = Offset(xSingle, ySingle)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = circleInner,
                                        center = Offset(xSingle, ySingle)
                                    )

                                    if (isActive) {
                                        drawLine(
                                            color = Green.copy(alpha = 0.45f),
                                            start = Offset(xSingle, 0f),
                                            end = Offset(xSingle, h),
                                            strokeWidth = vLineStroke
                                        )
                                        drawCircle(
                                            color = Green.copy(alpha = 0.28f),
                                            radius = circleHalo,
                                            center = Offset(xSingle, ySingle)
                                        )
                                        drawCircle(
                                            color = highlightGreen,
                                            radius = circleOuter,
                                            center = Offset(xSingle, ySingle)
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = circleInner,
                                            center = Offset(xSingle, ySingle)
                                        )
                                    }
                                } else {
                                    // 灰底漸層 + 黑線
                                    areaPathAll?.let { ap ->
                                        drawPath(path = ap, brush = baseAreaBrush)
                                    }
                                    linePathAll?.let { lp ->
                                        drawPath(
                                            path = lp,
                                            color = Color(0xFF111114),
                                            style = Stroke(
                                                width = baseStroke,
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }

                                    // ====== Overlay（只依賴 shownIndex） ======
                                    val idx = shownIndex
                                    if (idx != null && idx in xsAll.indices && linePathAll != null && areaPathAll != null) {
                                        val xSel = xsAll[idx]
                                        val ySel = ysAll[idx]

                                        val leftClip = -halfStroke - 2f
                                        val rightClip = w + halfStroke + 2f

                                        // 左半段綠色覆蓋
                                        withTransform({
                                            clipRect(
                                                left = leftClip,
                                                top = 0f,
                                                right = xSel + halfStroke,
                                                bottom = h
                                            )
                                        }) {
                                            drawPath(
                                                path = areaPathAll,
                                                brush = Brush.verticalGradient(
                                                    0f to highlightGreen.copy(alpha = 0.24f),
                                                    0.55f to highlightGreen.copy(alpha = 0.16f),
                                                    1f to Color.Transparent
                                                )
                                            )
                                            drawPath(
                                                path = linePathAll,
                                                color = highlightGreen,
                                                style = Stroke(
                                                    width = baseStroke,
                                                    cap = StrokeCap.Round
                                                )
                                            )
                                        }

                                        // 右半段灰底（維持你原本的視覺）
                                        withTransform({
                                            clipRect(
                                                left = xSel - halfStroke,
                                                top = 0f,
                                                right = rightClip,
                                                bottom = h
                                            )
                                        }) {
                                            drawPath(
                                                path = areaPathAll,
                                                brush = rightAreaBrush
                                            )
                                        }

                                        // 垂直綠線 + 圓點
                                        drawLine(
                                            color = Green.copy(alpha = 0.45f),
                                            start = Offset(xSel, 0f),
                                            end = Offset(xSel, h),
                                            strokeWidth = vLineStroke
                                        )
                                        drawCircle(
                                            color = Green.copy(alpha = 0.28f),
                                            radius = circleHalo,
                                            center = Offset(xSel, ySel)
                                        )
                                        drawCircle(
                                            color = Green,
                                            radius = circleOuter,
                                            center = Offset(xSel, ySel)
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = circleInner,
                                            center = Offset(xSel, ySel)
                                        )
                                    }
                                }
                            }
                        }
                    }
            )

            if (chartData.points.isEmpty()) {
                Text(
                    text = stringResource(R.string.weight_goal_chart_empty_hint),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (chartData.goalPointY != null && chartWidthPx > 0f && chartHeightPx > 0f) {
                val innerHeightPx = chartHeightPx - topPaddingPx - bottomPaddingPx
                val labelWidthPx = with(density) { 54.dp.toPx() }
                val labelHeightPx = with(density) { 28.dp.toPx() }
                val labelX = (chartWidthPx - endPaddingPx - labelWidthPx - with(density) { 8.dp.toPx() })
                    .coerceAtLeast(startPaddingPx)
                val labelY = (topPaddingPx + chartData.goalPointY * innerHeightPx - labelHeightPx / 2f)
                    .coerceIn(topPaddingPx, chartHeightPx - bottomPaddingPx - labelHeightPx)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = with(density) { labelX.toDp() },
                            y = with(density) { labelY.toDp() }
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFF59E0B).copy(alpha = 0.42f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.weight_chart_goal_label),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309)
                    )
                }
            }

            // Y 軸標籤：右對齊，並保留固定間距避免文字貼到圖表區
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(WeightChartYAxisLabelWidth)
                    .fillMaxHeight()
                    .padding(top = topPaddingDp, bottom = bottomPaddingDp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                chartData.yLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.60f),
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tooltip
            val idx = shownIndex
            if (
                idx != null &&
                idx in chartData.dates.indices &&
                chartWidthPx > 0f &&
                chartHeightPx > 0f
            ) {
                val pointNorm = chartData.points[idx]

                val innerWidthPx = chartWidthPx - startPaddingPx - endPaddingPx
                val innerHeightPx = chartHeightPx - topPaddingPx - bottomPaddingPx

                val circleOuterPx = with(density) { 5.dp.toPx() }

                val baseXInner = pointNorm.x * innerWidthPx
                val xSafeInner = baseXInner.coerceIn(circleOuterPx, innerWidthPx - circleOuterPx)

                val centerX = startPaddingPx + xSafeInner
                val centerY = topPaddingPx + pointNorm.y * innerHeightPx

                val tooltipWidthPx = with(density) { 202.dp.toPx() }
                val tooltipHeightPx = with(density) { 90.dp.toPx() }
                var tx = centerX - tooltipWidthPx / 2f

                val paddingPx = with(density) { 8.dp.toPx() }
                val maxX = chartWidthPx - tooltipWidthPx - paddingPx
                tx = tx.coerceIn(paddingPx, maxX)

                val preferredTy = centerY - tooltipHeightPx - with(density) { 10.dp.toPx() }
                val fallbackTy = centerY + with(density) { 12.dp.toPx() }
                val rawTy = if (preferredTy >= paddingPx) preferredTy else fallbackTy
                val maxY = (chartHeightPx - tooltipHeightPx - paddingPx).coerceAtLeast(paddingPx)
                val ty = rawTy.coerceIn(paddingPx, maxY)

                WeightTooltip(
                    weightKg = chartData.weightsKg[idx],
                    weightLbs = chartData.weightsLbs.getOrNull(idx),
                    goalKg = goalKg,
                    unit = unit,
                    date = chartData.dates[idx],
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = with(density) { tx.toDp() },
                            y = with(density) { ty.toDp() }
                        )
                )
            }
        }

        Spacer(Modifier.height(WeightChartXAxisLabelGap))

        val selectedLabelIndex: Int? = shownIndex
            ?.takeIf { it in chartData.dates.indices }
            ?.let { pi ->
                val selectedX = chartData.points.getOrNull(pi)?.x ?: return@let null
                if (chartData.axisDates.isEmpty()) return@let null
                chartData.axisX.withIndex()
                    .minByOrNull { (_, x) -> abs(x - selectedX) }
                    ?.index
            }

        val textMeasurer = rememberTextMeasurer()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPaddingDp, end = endPaddingDp)
                .height(WeightChartXAxisLabelHeight)
        ) {
            if (chartWidthPx > 0f && chartData.axisDates.isNotEmpty()) {

                val innerWidthPx =
                    (chartWidthPx - startPaddingPx - endPaddingPx).coerceAtLeast(1f)

                val centersPx: List<Float> = chartData.axisX.map { it * innerWidthPx }

                val baseMinGapPx = with(density) { 8.dp.toPx() }
                val edgePaddingPx = with(density) { 2.dp.toPx() }
                val desiredMinCount = chartData.axisDates.size

                val measureStyle = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val formatters = listOf(axisDateFormatter)

                var bestLabels: List<String> = emptyList()
                var bestPlaced: List<XLabelPlaced> = emptyList()

                for (fmt in formatters) {
                    val labels = chartData.axisDates.map { fmt.format(it) }

                    val specs: List<XLabelSpec> = labels.indices.map { i ->
                        val wPx = textMeasurer
                            .measure(text = AnnotatedString(labels[i]), style = measureStyle)
                            .size.width.toFloat()

                        XLabelSpec(
                            index = i,
                            centerPx = centersPx.getOrElse(i) { 0f },
                            widthPx = wPx
                        )
                    }

                    val placed = placeXAxisLabelsAtLeast(
                        specs = specs,
                        innerWidthPx = innerWidthPx,
                        edgePaddingPx = edgePaddingPx,
                        baseMinGapPx = baseMinGapPx,
                        desiredMinCount = desiredMinCount,
                        keepEnds = true
                    )

                    if (placed.size > bestPlaced.size) {
                        bestPlaced = placed
                        bestLabels = labels
                    }
                    if (placed.size >= desiredMinCount) {
                        bestPlaced = placed
                        bestLabels = labels
                        break
                    }
                }

                bestPlaced.forEach { p ->
                    val label = bestLabels.getOrNull(p.index) ?: return@forEach
                    val selected = (selectedLabelIndex == p.index)

                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color(0xFF111114) else Color.Black.copy(alpha = 0.60f),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { translationX = p.leftPx }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------
// Motivation & History
// ----------------------------------------------------------

@Composable
fun MotivationBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val bg = Color(0xFFEFF9F4)
    val fg = Color(0xFF12823B)

    Box(
        modifier = modifier
            .widthIn(max = WeightChartMotivationBannerMaxWidth)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = fg,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            maxLines = WEIGHT_CHART_MOTIVATION_MAX_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val historyRowDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d · HH:mm", Locale.US)

private val historyRowDateOnlyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d", Locale.US)

private fun formatHistoryRowUpdatedTime(
    item: WeightItemDto,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    item.updatedAtUtc
        ?.let(::parseHistoryRowUtcInstantOrNull)
        ?.let { instant ->
            return instant
                .atZone(zoneId)
                .format(historyRowDateTimeFormatter)
        }

    return runCatching {
        LocalDate.parse(item.logDate).format(historyRowDateOnlyFormatter)
    }.getOrElse {
        item.logDate
    }
}

private fun parseHistoryRowUtcInstantOrNull(raw: String): Instant? {
    val text = raw.trim()
    if (text.isBlank()) return null

    runCatching {
        return Instant.parse(text)
    }

    runCatching {
        return OffsetDateTime.parse(text).toInstant()
    }

    runCatching {
        return ZonedDateTime.parse(text).toInstant()
    }

    runCatching {
        return LocalDateTime.parse(text)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
    }

    return null
}

private enum class TrendTag { LOSS, GAIN, STABLE }

@Composable
fun HistoryRow(
    item: WeightItemDto,
    unit: UserProfileStore.WeightUnit,
    previous: WeightItemDto?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val border = Color(0xFFE2E5EA)
    val label = Color.Black.copy(alpha = 0.55f)
    val mainText = Color(0xFF111114)
    val subText = Color.Black.copy(alpha = 0.45f)

    val dateText = remember(item.logDate, item.updatedAtUtc) {
        formatHistoryRowUpdatedTime(item)
    }

    val weightText = formatWeightFromDb(
        kg = item.weightKg,
        lbs = item.weightLbs,
        unit = unit
    )

    val delta = computeDelta(current = item, previous = previous, unit = unit)
    val (trend, deltaColor) = classifyTrendAndColor(delta)
    val deltaText = delta?.let { formatSigned1(it, unit) } ?: "—"

    val (chipText, chipBg, chipFg) = when (trend) {
        TrendTag.LOSS -> Triple("LOSS", Color(0xFFEFF9F4), Color(0xFF12823B))
        TrendTag.GAIN -> Triple("GAIN", Color(0xFFFEE2E2), Color(0xFFEF4444))
        TrendTag.STABLE -> Triple("STABLE", Color(0xFFDBEAFE), Color(0xFF3B82F6))
    }

    fun toAbsoluteUrl(maybePath: String?): String? {
        if (maybePath.isNullOrBlank()) return null
        // 已經是 http 開頭就直接回傳
        if (maybePath.startsWith("http://") || maybePath.startsWith("https://")) return maybePath
        // DB 是 /static/xxx 這種 → 補上 baseUrl
        return BuildConfig.API_BASE_URL.trimEnd('/') + maybePath
    }

    val imgUrl = toAbsoluteUrl(item.photoUrl)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 85.dp)
            .border(1.dp, border, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左側圖片
            val imageShape = RoundedCornerShape(14.dp)
            if (item.photoUrl != null) {
                AsyncImage(
                    model = imgUrl,
                    contentDescription = null,
                    modifier = Modifier.size(58.dp).clip(imageShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.weight_image),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp).clip(imageShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            // ✅ 一層就好：左欄(WEIGHT+DATE) + 右欄(CHANGE+CHIP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左：WEIGHT + 體重 + 更新時間
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 58.dp)
                        .padding(top = 1.dp, bottom = 1.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.weight_history_weight_label),
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        letterSpacing = 0.7.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = weightText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = (-2).dp),
                        fontSize = 19.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainText,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = dateText,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = subText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                // 右：CHANGE + Δ + chip（不再放 dateText）
                RightMetaColumn(
                    labelColor = label,
                    deltaColor = deltaColor,
                    deltaText = deltaText,
                    chipText = chipText,
                    chipBg = chipBg,
                    chipFg = chipFg
                )
            }
        }
    }
}

@Composable
private fun RightMetaColumn(
    labelColor: Color,
    deltaColor: Color,
    deltaText: String,
    chipText: String,
    chipBg: Color,
    chipFg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(min = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.weight_history_change_label),
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = deltaText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = deltaColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(y = 5.dp)
        )

        Spacer(Modifier.height(12.dp))

        MiniChip(text = chipText, bg = chipBg, fg = chipFg)
    }
}

@Composable
private fun MiniChip(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

/**
 * delta 的單位：
 * - KG：用 kg 差
 * - LBS：優先用 DB lbs 差（兩邊都有），否則用 kg 差轉 lbs
 */
private fun computeDelta(
    current: WeightItemDto,
    previous: WeightItemDto?,
    unit: UserProfileStore.WeightUnit
): Double? {
    if (previous == null) return null

    return when (unit) {
        UserProfileStore.WeightUnit.KG -> current.weightKg - previous.weightKg
        UserProfileStore.WeightUnit.LBS -> {
            val c = current.weightLbs
            val p = previous.weightLbs
            if (c != null && p != null) c - p else kgToLbs1(current.weightKg - previous.weightKg)
        }
    }
}

private fun formatSigned1(value: Double, unit: UserProfileStore.WeightUnit): String {
    val sign = when {
        value >  1e-6 -> "+"
        value < -1e-6 -> "−"
        else -> ""
    }
    val absV = abs(value)
    val unitText = if (unit == UserProfileStore.WeightUnit.KG) "kg" else "lbs"
    return String.format(NUM_LOCALE, "%s%.1f %s", sign, absV, unitText)
}

/**
 * 規則：
 * - 正：紅 + GAIN
 * - 負：綠 + LOSS
 * - 0：藍 + STABLE
 */
private fun classifyTrendAndColor(delta: Double?): Pair<TrendTag, Color> {
    if (delta == null) return TrendTag.STABLE to Color.Black.copy(alpha = 0.45f)

    return when {
        delta >  1e-6 -> TrendTag.GAIN to Color(0xFFEF4444)  // red-500
        delta < -1e-6 -> TrendTag.LOSS to Color(0xFF22C55E)  // green-500
        else -> TrendTag.STABLE to Color(0xFF3B82F6)         // blue-500
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterTabsPreview() {
    MaterialTheme {
        Column(Modifier.padding(50.dp)) {
            FilterTabs(selected = WEIGHT_RANGE_30_DAYS, onSelect = {})
        }
    }
}

// ★ 新增：Catmull-Rom cubic spline，讓曲線通過每一個點
private fun buildClampedSmoothPath(
    xs: List<Float>,
    ys: List<Float>
): Path {
    val path = Path()
    val n = xs.size
    if (n == 0) return path
    if (n == 1) {
        path.moveTo(xs[0], ys[0])
        return path
    }

    path.moveTo(xs[0], ys[0])

    for (i in 0 until n - 1) {
        val x0 = xs[i]
        val y0 = ys[i]
        val x1 = xs[i + 1]
        val y1 = ys[i + 1]
        val dx = (x1 - x0) * 0.35f
        val minY = minOf(y0, y1)
        val maxY = maxOf(y0, y1)

        path.cubicTo(
            x0 + dx,
            y0.coerceIn(minY, maxY),
            x1 - dx,
            y1.coerceIn(minY, maxY),
            x1,
            y1
        )
    }

    return path
}

/**
 * TO GOAL 專用（修正版）：
 * - KG 模式：用 goalKg - currentKg
 * - LBS 模式：優先用 DB 的 goalLbs - currentLbs（避免 kg→lbs 誤差）
 *   若 lbs 任何一個為 null，才 fallback 用 kg 差值轉 lbs
 */
fun formatDeltaGoalMinusCurrentFromDb(
    goalKg: Double?,
    goalLbs: Double?,
    currentKg: Double?,
    currentLbs: Double?,
    unit: UserProfileStore.WeightUnit,
    lbsAsInt: Boolean
): String {
    return when (unit) {
        UserProfileStore.WeightUnit.KG -> {
            if (goalKg == null || currentKg == null) return "—"
            val diffKg = goalKg - currentKg
            val sign = if (diffKg >= 0) "+" else "−"
            val absKg = abs(diffKg)
            String.format(Locale.US, "%s%.1f kg", sign, absKg)
        }

        UserProfileStore.WeightUnit.LBS -> {
            val diffLbs: Double? =
                if (goalLbs != null && currentLbs != null) {
                    goalLbs - currentLbs // ✅ 核心：直接用 DB lbs 算
                } else if (goalKg != null && currentKg != null) {
                    kgToLbs1(goalKg - currentKg) // fallback（避免崩）
                } else null

            if (diffLbs == null) return "—"

            val sign = if (diffLbs >= 0) "+" else "−"
            val absLbs = abs(diffLbs)

            val core = if (lbsAsInt) {
                absLbs.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", absLbs)
            }

            "$sign$core lbs"
        }
    }
}

fun formatWeightFromDb(
    kg: Double?,
    lbs: Double?,
    unit: UserProfileStore.WeightUnit
): String {
    return when (unit) {
        UserProfileStore.WeightUnit.KG -> {
            // KG 模式：只看 DB 的 kg，沒有就顯示破折號
            kg?.let { String.format(NUM_LOCALE,"%.1f kg", it) } ?: "—"
        }
        UserProfileStore.WeightUnit.LBS -> {
            // LBS 模式：只看 DB 的 lbs，沒有就顯示破折號
            lbs?.let { String.format(NUM_LOCALE,"%.1f lbs", it) } ?: "—"
        }
    }
}

/**
 * Tooltip 專用：一筆資料點的體重顯示
 */
fun formatTooltipWeight(
    weightKg: Double,
    weightLbs: Double?,
    unit: UserProfileStore.WeightUnit
): String {
    return when (unit) {
        UserProfileStore.WeightUnit.KG -> {
            String.format(NUM_LOCALE,"%.1f kg", weightKg)
        }
        UserProfileStore.WeightUnit.LBS -> {
            // 這裡也一樣：先用 DB 的 lbs
            val lbs = weightLbs ?: kgToLbs1(weightKg)
            String.format(NUM_LOCALE, "%.1f lbs", lbs)
        }
    }
}

private fun formatGoalDeltaWeight(
    weightKg: Double,
    weightLbs: Double?,
    goalKg: Double,
    unit: UserProfileStore.WeightUnit
): String {
    return when (unit) {
        UserProfileStore.WeightUnit.KG -> {
            String.format(NUM_LOCALE, "%.1f kg", abs(weightKg - goalKg))
        }
        UserProfileStore.WeightUnit.LBS -> {
            val displayWeightLbs = weightLbs ?: kgToLbs1(weightKg)
            val goalLbs = kgToLbs1(goalKg)
            String.format(NUM_LOCALE, "%.1f lbs", abs(displayWeightLbs - goalLbs))
        }
    }
}

data class XLabelSpec(
    val index: Int,
    val centerPx: Float,
    val widthPx: Float
)

data class XLabelPlaced(
    val index: Int,
    val leftPx: Float,
    val widthPx: Float
) {
    val rightPx: Float get() = leftPx + widthPx
}

/**
 * 依「不重疊」規則放置 X 軸 labels（px）
 * - 盡量多放
 * - 一定保留第一個、最後一個（keepEnds=true）
 * - 若最後一個會撞到前一個，會回頭移除前一個直到不撞（保第一個）
 */
fun placeXAxisLabels(
    specs: List<XLabelSpec>,
    innerWidthPx: Float,
    edgePaddingPx: Float,
    minGapPx: Float,
    keepEnds: Boolean = true
): List<XLabelPlaced> {
    if (specs.isEmpty() || innerWidthPx <= 0f) return emptyList()
    if (specs.size == 1) {
        val s = specs.first()
        val left = clampLeft(s.centerPx - s.widthPx / 2f, s.widthPx, innerWidthPx, edgePaddingPx)
        return listOf(XLabelPlaced(s.index, left, s.widthPx))
    }

    val sorted = specs.sortedBy { it.centerPx }
    val first = sorted.first()
    val last = sorted.last()

    fun placedOf(s: XLabelSpec): XLabelPlaced {
        val rawLeft = s.centerPx - s.widthPx / 2f
        val left = clampLeft(rawLeft, s.widthPx, innerWidthPx, edgePaddingPx)
        return XLabelPlaced(s.index, left, max(0f, s.widthPx))
    }

    val result = mutableListOf<XLabelPlaced>()

    // 先放第一個
    result += placedOf(first)

    // 中間：由左到右，能放就放
    for (i in 1 until sorted.lastIndex) {
        val p = placedOf(sorted[i])
        val prev = result.lastOrNull()
        if (prev == null || p.leftPx >= prev.rightPx + minGapPx) {
            result += p
        }
    }

    // 最後一個：強制放（若撞到前一個，就移除前一個直到不撞；但保留第一個）
    val lastPlaced = placedOf(last)

    if (keepEnds) {
        while (result.isNotEmpty()) {
            val prev = result.last()
            val overlaps = lastPlaced.leftPx < prev.rightPx + minGapPx
            if (!overlaps) break
            if (result.size == 1) break // 只剩第一個就不能刪了
            result.removeAt(result.lastIndex)
        }
        // 若最後還是跟第一個撞（極窄），這時至少保留最後一個（避免末端日期消失）
        val onlyFirst = result.size == 1
        if (onlyFirst) {
            val firstPlaced = result.first()
            val overlaps = lastPlaced.leftPx < firstPlaced.rightPx + minGapPx
            if (overlaps) {
                // 超窄：改成只留「最後」
                return listOf(lastPlaced)
            }
        }
        result += lastPlaced
        return dedupByIndexKeepOrder(result)
    } else {
        // 不強制首尾的版本（目前你用不到）
        val prev = result.lastOrNull()
        if (prev == null || lastPlaced.leftPx >= prev.rightPx + minGapPx) {
            result += lastPlaced
        }
        return dedupByIndexKeepOrder(result)
    }
}

private fun clampLeft(
    rawLeft: Float,
    widthPx: Float,
    innerWidthPx: Float,
    edgePaddingPx: Float
): Float {
    val maxLeft = (innerWidthPx - edgePaddingPx - widthPx).coerceAtLeast(edgePaddingPx)
    return rawLeft.coerceIn(edgePaddingPx, maxLeft)
}

private fun dedupByIndexKeepOrder(list: List<XLabelPlaced>): List<XLabelPlaced> {
    val seen = HashSet<Int>(list.size)
    val out = ArrayList<XLabelPlaced>(list.size)
    for (x in list) {
        if (seen.add(x.index)) out.add(x)
    }
    return out
}
private val GAP_MULTIPLIERS = floatArrayOf(
    1.00f, 0.85f, 0.70f, 0.55f, 0.40f, 0.25f
)

/**
 * 目標：盡量達到至少 desiredMinCount 個 label（同時 keepEnds 規則不變）
 * 做法：逐步降低 minGapPx，找到第一個能達標的；若都不行就回傳「最多的那次」。
 *
 * 注意：這裡的 minGapPx 是「文字框」之間的 gap，不是刻度中心距。
 */
internal fun placeXAxisLabelsAtLeast(
    specs: List<XLabelSpec>,
    innerWidthPx: Float,
    edgePaddingPx: Float,
    baseMinGapPx: Float,
    desiredMinCount: Int,
    keepEnds: Boolean = true
): List<XLabelPlaced> {
    if (specs.isEmpty() || innerWidthPx <= 0f) return emptyList()

    var best: List<XLabelPlaced> = emptyList()

    for (m in GAP_MULTIPLIERS) {
        val gap = max(0f, baseMinGapPx * m)
        val placed = placeXAxisLabels(
            specs = specs,
            innerWidthPx = innerWidthPx,
            edgePaddingPx = edgePaddingPx,
            minGapPx = gap,
            keepEnds = keepEnds
        )

        if (placed.size > best.size) best = placed
        if (placed.size >= desiredMinCount) return placed
    }

    return best
}

internal fun formatAchievedPercent1(progress: Float): String {
    return String.format(Locale.US, "%.1f", progress * 100f)
}
