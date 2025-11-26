package com.calai.app.ui.home.ui.weight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.calai.app.R
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.weight.api.WeightItemDto
import com.calai.app.ui.home.components.CardStyles
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import com.calai.app.data.profile.repo.kgToLbs1
import java.time.format.FormatStyle
import kotlin.math.max
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer

private const val X_TICK_COUNT = 5

// ----------------------------------------------------------
// Summary Cards
// ----------------------------------------------------------
val Green = Color(0xFF22C55E)
@Composable
fun SummaryCards(ui: WeightViewModel.UiState) {
    val label   = Color.Black.copy(alpha = 0.60f)
    val big     = Color(0xFF111114)
    val divider = Color(0xFFE2E5EA)
    val trackColor  = Color(0xFFDCE1E7)
    val stripeColor = Color.White.copy(alpha = 0.35f)
    val fillColor   = Color(0xFFFF8A33).copy(alpha = 0.85f)

    val unit = ui.unit

    val currentKg  = ui.current ?: ui.profileWeightKg
    val currentLbs = ui.currentLbs ?: ui.profileWeightLbs

    // ---------- 目標體重：一律用 DB user_profiles（SummaryDto.goal*） ----------
    val goalKg  = ui.goal      // ★ DB target_weight_kg
    val goalLbs = ui.goalLbs   // ★ DB target_weight_lbs

    // TO TARGET：還是用 kg 為基準算差值（顯示時再依單位轉）
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
        timeseries      = ui.series,
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

    // ---------- edgeRight：只拿 DB 目標體重（Summary / user_profiles.target_*） ----------
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
                // 左欄：TO TARGET
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    UpperLabel(
                        text = "TO TARGET",
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
                        text = "CURRENT WEIGHT",
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

            UpperLabel(
                text = "ACHIEVED ${(progress * 100).toInt()}% OF GOAL",
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
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier
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
        String.format("%.1f kg", kg)
    } else {
        // LBS 模式：一律走共用的 kgToLbs1，確保跟 Record / 後端一致
        val lbs = kgToLbs1(kg)
        if (lbsAsInt) {
            // 只要整數：TO TARGET / axis label 等場合你想顯示 176 lbs 這種
            String.format("%d lbs", lbs.toInt())
        } else {
            // 顯示到小數點一位：如 CURRENT WEIGHT、TO TARGET 主數字
            String.format("%.1f lbs", lbs)
        }
    }
}

fun formatDeltaGoalMinusCurrent(
    goalKg: Double?,
    currentKg: Double?,
    unit: UserProfileStore.WeightUnit,
    lbsAsInt: Boolean
): String {
    if (goalKg == null || currentKg == null) return "—"
    val diffKg = goalKg - currentKg
    val sign = if (diffKg >= 0) "+" else "−"
    val absKg = kotlin.math.abs(diffKg)

    return if (unit == UserProfileStore.WeightUnit.KG) {
        // KG：差多少 kg，顯示到小數點一位
        String.format("%s%.1f kg", sign, absKg)
    } else {
        // LBS：差多少 lbs，一律透過 kgToLbs1
        val lbs = kgToLbs1(absKg)
        val core = if (lbsAsInt) {
            lbs.toInt().toString()
        } else {
            String.format("%.1f", lbs)
        }
        "$sign$core lbs"
    }
}

fun formatWeight(kg: Double?, unit: UserProfileStore.WeightUnit): String {
    if (kg == null) return "—"
    return if (unit == UserProfileStore.WeightUnit.KG) {
        String.format("%.1f kg", kg)
    } else {
        // 歷史列表也共用同一套 lbs 算法
        val lbs = kgToLbs1(kg)
        String.format("%.1f lbs", lbs)
    }
}

fun formatDelta(deltaKg: Double, unit: UserProfileStore.WeightUnit): String {
    val sign = if (deltaKg >= 0) "+" else "−"
    val absKg = kotlin.math.abs(deltaKg)
    return if (unit == UserProfileStore.WeightUnit.KG) {
        String.format("%s%.1f kg", sign, absKg)
    } else {
        val lbs = kgToLbs1(absKg)
        String.format("%s%.1f lbs", sign, lbs)
    }
}

// ----------------------------------------------------------
// Filter Tabs
// ----------------------------------------------------------

private data class RangeTab(
    val key: String,
    val label: String
)

@Composable
fun FilterTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    val tabs = listOf(
        RangeTab("season",   "90 Days"),
        RangeTab("half year","6 Months"),
        RangeTab("year",     "1 Year"),
        RangeTab("all",      "All time")
    )

    val selectedIndex = tabs.indexOfFirst { it.key == selected }.let {
        if (it >= 0) it else 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF1F3F7))
            .border(
                width = 1.dp,
                color = Color(0xFFE0E2E6),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onSelect(tab.key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) Color(0xFF111114) else Color(0xFF6B7280),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------
// Weight Chart Card
// ----------------------------------------------------------

/**
 * startWeightAllTimeKg：
 * - 之後 VM 若有「全時段第一筆體重」，可以從呼叫端傳進來；
 * - 現在預設為 null，會自動 fallback 成目前 slice 的第一筆紀錄。
 */
@Composable
fun WeightChartCard(
    ui: WeightViewModel.UiState,
    startWeightAllTimeKg: Double? = null,
    onEditTargetWeight: () -> Unit       // ★ 新增
) {
    val unit          = ui.unit
    val currentKg     = ui.current ?: ui.profileWeightKg
    val goalKg        = ui.goal              // ★ 只用 DB user_profiles.target_weight_kg
    val profileWeight = ui.profileWeightKg   // 起點仍然用 DB 的 current weight

    val progressFraction = computeWeightProgress(
        timeseries       = ui.series,
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111114)
                )
                GoalProgressBadge(
                    progressPercent = progressPercent,
                    onClick = onEditTargetWeight
                )
            }

            Spacer(Modifier.height(12.dp))

            GoalProgressChart(
                series               = ui.series,
                unit                 = unit,
                currentKg            = currentKg,
                goalKg               = goalKg,
                profileWeightKg      = profileWeight,
                startWeightAllTimeKg = startWeightAllTimeKg
            )

            Spacer(Modifier.height(12.dp))

            // ⭐ 關鍵：讓膠囊寬度只包住文字，位置靠左
            MotivationBanner(
                text = "Once you take the first step, the rest will follow !",
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
            .background(Color.White)
            .border(
                width = 1.dp,
                color = Color(0xFFE2E5EA),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }    // ★ 讓整個膠囊可以點
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFF6B7280)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${progressPercent.coerceIn(0, 100)}% of goal",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black.copy(alpha = 0.60f)
        )
        // ★ 文字後面的鉛筆圖示
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "Edit goal",
            modifier = Modifier.size(13.dp),
            tint = Color(0xFF6B7280)
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
    val yLabels: List<String>,              // 由上到下
    val xLabels: List<String>,              // 由左到右（X 軸顯示文字）
    val points: List<ChartPointNormalized>, // 折線所有點（0f..1f）
    val dates: List<LocalDate>,             // 每個資料點的日期（對應 points）
    val weightsKg: List<Double>,            // 每個資料點的體重（kg）
    val weightsLbs: List<Double?>,          // ★ 新增：每個點的 DB lbs（直接吃 DTO，可能為 null）
    val axisDates: List<LocalDate>,         // X 軸刻度實際日期（對應 xLabels）
    val axisX: List<Float>                  // 每個刻度在 X 軸上的位置（0f..1f）
)

// X 軸日期格式（軸上字）
private val axisDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH)

// Tooltip 用日期格式（底下黑色氣泡）
private val tooltipDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)

/**
 * 建立圖表資料
 */
private fun buildWeightChartData(
    series: List<WeightItemDto>,
    unit: UserProfileStore.WeightUnit,
    currentKg: Double?,
    goalKg: Double?,
    profileWeightKg: Double?,
    startWeightAllTimeKg: Double? = null
): WeightChartData {
    if (series.isEmpty()) {
        return WeightChartData(
            yLabels    = emptyList(),
            xLabels    = emptyList(),
            points     = emptyList(),
            dates      = emptyList(),
            weightsKg  = emptyList(),
            weightsLbs = emptyList(),   // ★ 一定要補這個
            axisDates  = emptyList(),
            axisX      = emptyList()
        )
    }

    // 1) 解析日期 + 排序（同時保留 kg / lbs）
    val sorted: List<Triple<LocalDate, Double, Double?>> = series.mapNotNull { item ->
        runCatching { LocalDate.parse(item.logDate) }.getOrNull()
            ?.let { date ->
                Triple(
                    date,
                    item.weightKg,
                    item.weightLbs?.toDouble() // DB 的 178.0 會變成 178.0
                )
            }
    }.sortedBy { it.first }

    if (sorted.isEmpty()) {
        return WeightChartData(
            yLabels    = emptyList(),
            xLabels    = emptyList(),
            points     = emptyList(),
            dates      = emptyList(),
            weightsKg  = emptyList(),
            weightsLbs = emptyList(),   // ★ 這邊也要
            axisDates  = emptyList(),
            axisX      = emptyList()
        )
    }

    val datesSorted: List<LocalDate> = sorted.map { it.first }
    val weightsKg:   List<Double>    = sorted.map { it.second }
    val weightsLbs:  List<Double?>   = sorted.map { it.third }

    // 全時段第一筆（後端算好的）> 沒有就用目前 slice 第一筆
    val startKg = startWeightAllTimeKg ?: weightsKg.first()

    // 區間內實際 min / max
    val dataMin = weightsKg.minOrNull()!!
    val dataMax = weightsKg.maxOrNull()!!

    val effGoal    = goalKg    ?: currentKg ?: startKg
    val effCurrent = currentKg ?: startKg

    // 2) 決定原始上下界（包住 start / goal / current / 區間 min/max）
    var topKg: Double
    var bottomKg: Double

    if (goalKg != null && currentKg != null) {
        if (goalKg > currentKg) {
            // 增重
            topKg = maxOf(goalKg, dataMax, startKg)
            bottomKg = minOf(startKg, dataMin)
        } else if (goalKg < currentKg) {
            // 減重
            topKg = maxOf(startKg, dataMax)
            bottomKg = minOf(goalKg, dataMin)
        } else {
            topKg = maxOf(startKg, effGoal, dataMax)
            bottomKg = minOf(startKg, effGoal, dataMin)
        }
    } else {
        topKg = maxOf(startKg, dataMax, effGoal, effCurrent)
        bottomKg = minOf(startKg, dataMin, effGoal, effCurrent)
    }

    // 3) 上下加一點 margin
    run {
        val rawTop = topKg
        val rawBottom = bottomKg

        if (rawTop == rawBottom) {
            topKg = rawTop + 0.5
            bottomKg = rawBottom - 0.5
        } else {
            val rawSpan = (rawTop - rawBottom).coerceAtLeast(1e-6)
            val marginRatio = 0.05
            val margin = rawSpan * marginRatio

            var topWithMargin = rawTop + margin
            var bottomWithMargin = rawBottom - margin

            if (topWithMargin <= bottomWithMargin) {
                topWithMargin = rawTop + 0.5
                bottomWithMargin = rawBottom - 0.5
            }

            topKg = topWithMargin
            bottomKg = bottomWithMargin
        }
    }

    val span = (topKg - bottomKg).coerceAtLeast(1e-6)

    // 4) Y / X 標籤
    val yLabels = buildYAxisLabels(topKg, bottomKg, unit)

    val allDates  = datesSorted
    val axisDates = buildXAxisDates(allDates, maxLabels = X_TICK_COUNT)
    val xLabels   = axisDates.map { axisDateFormatter.format(it) }

    // 共同時間座標基準
    val firstDay = allDates.first().toEpochDay()
    val lastDay  = allDates.last().toEpochDay()
    val daySpan  = (lastDay - firstDay).coerceAtLeast(1L)

    // 5) 折線資料點 → 正規化
    val points: List<ChartPointNormalized> =
        if (datesSorted.size == 1) {
            val onlyKg = weightsKg.first()
            val clamped = onlyKg.coerceIn(bottomKg, topKg)
            val y = (((topKg - clamped) / span).toFloat()).coerceIn(0f, 1f)
            listOf(
                ChartPointNormalized(
                    x = 0f,
                    y = y
                )
            )
        } else {
            datesSorted.zip(weightsKg).map { (date, wKg) ->
                val x = ((date.toEpochDay() - firstDay).toFloat() / daySpan.toFloat())
                    .coerceIn(0f, 1f)
                val clamped = wKg.coerceIn(bottomKg, topKg)
                val y = (((topKg - clamped) / span).toFloat()).coerceIn(0f, 1f)
                ChartPointNormalized(x, y)
            }
        }

    // 6) X 軸刻度位置
    val axisX: List<Float> =
        if (axisDates.size == 1) {
            listOf(0f)
        } else {
            axisDates.map { d ->
                ((d.toEpochDay() - firstDay).toFloat() / daySpan.toFloat())
                    .coerceIn(0f, 1f)
            }
        }

    return WeightChartData(
        yLabels   = yLabels,
        xLabels   = xLabels,
        points    = points,
        dates     = datesSorted,
        weightsKg = weightsKg,
        weightsLbs = weightsLbs,    // ★ 在這裡塞進去
        axisDates = axisDates,
        axisX     = axisX
    )
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
private fun buildXAxisDates(
    dates: List<LocalDate>,
    maxLabels: Int
): List<LocalDate> {
    if (dates.isEmpty() || maxLabels <= 0) return emptyList()

    val sortedDistinct = dates.distinct().sorted()
    val minDate = sortedDistinct.first()
    val maxDate = sortedDistinct.last()

    if (maxLabels == 1) return listOf(maxDate)

    val spanDays = (maxDate.toEpochDay() - minDate.toEpochDay()).coerceAtLeast(0L)

    // ✅ 範圍太短：直接每天列出（<= maxLabels 個，不會重複）
    // 例：只有 11/24~11/26，硬切 5 份只會得到重複日期，反而難看。
    if (spanDays <= (maxLabels - 1).toLong()) {
        return (0L..spanDays).map { d -> minDate.plusDays(d) }
    }

    // ✅ 範圍夠長：平均取樣 maxLabels 個日期，首尾必定是 min/max
    val lastIndex = maxLabels - 1
    return (0..lastIndex).map { i ->
        val offset = (i.toLong() * spanDays) / lastIndex.toLong()
        minDate.plusDays(offset)
    }.distinct()
}

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
    unit: UserProfileStore.WeightUnit,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    key(unit, weightLbs) {
        val weightText = formatTooltipWeight(
            weightKg = weightKg,
            weightLbs = weightLbs,
            unit = unit
        )
        val dateText = tooltipDateFormatter.format(date)

        Box(
            modifier = modifier
                .width(98.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111114).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = weightText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 折線圖本體
 */
@Composable
private fun GoalProgressChart(
    series: List<WeightItemDto>,
    unit: UserProfileStore.WeightUnit,
    currentKg: Double?,
    goalKg: Double?,
    profileWeightKg: Double?,
    startWeightAllTimeKg: Double? = null,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(190.dp)
) {
    val chartData = buildWeightChartData(
        series = series,
        unit = unit,
        currentKg = currentKg,
        goalKg = goalKg,
        profileWeightKg = profileWeightKg,
        startWeightAllTimeKg = startWeightAllTimeKg
    )

    // ★ 只用 points.size 當 key（避免 unit 切換造成 state 舊引用）
    var activeIndex by remember(chartData.points.size) { mutableStateOf<Int?>(null) }
    var pinnedIndex by remember(chartData.points.size) { mutableStateOf<Int?>(null) }

    // 單位切換：清掉（避免跳動）
    LaunchedEffect(unit) {
        activeIndex = null
        pinnedIndex = null
    }

    // ✅ 顯示優先序：手指/拖曳中的 active > pinned
    val shownIndex: Int? = activeIndex ?: pinnedIndex

    // ★ 圖表實際寬高（px）
    var chartWidthPx by remember { mutableStateOf(0f) }
    var chartHeightPx by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val startPaddingDp = 40.dp
    val endPaddingDp = 6.dp
    val topPaddingDp = 8.dp
    val bottomPaddingDp = 8.dp

    val startPaddingPx = with(density) { startPaddingDp.toPx() }
    val endPaddingPx = with(density) { endPaddingDp.toPx() }
    val topPaddingPx = with(density) { topPaddingDp.toPx() }
    val bottomPaddingPx = with(density) { bottomPaddingDp.toPx() }

    // ✅ 每個資料點在 Box 座標系的中心 X（px）
    val pointCentersPx = remember(chartData.points, chartWidthPx, startPaddingPx, endPaddingPx) {
        buildPointCentersPx(
            pointsXNorm = chartData.points.map { it.x },
            chartWidthPx = chartWidthPx,
            startPaddingPx = startPaddingPx,
            endPaddingPx = endPaddingPx
        )
    }

    fun pickIndex(rawX: Float): Int? = nearestIndexByX(pointCentersPx, rawX)

    // ✅ 小優化：只在 index 真的變化時才觸發 recomposition
    fun setActive(idx: Int) {
        if (activeIndex != idx) activeIndex = idx
    }

    Column(modifier = modifier) {
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

                        // ✅ 一碰就顯示
                        setActive(downIdx)

                        var dragging = false
                        var cancelledByVertical = false

                        val slop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            // UP / CANCEL
                            if (!change.pressed) break

                            val dx = change.position.x - downX
                            val dy = change.position.y - downY

                            // 決定方向：超過 slop 才判定
                            if (!dragging) {
                                val dist2 = dx * dx + dy * dy
                                if (dist2 >= slop * slop) {
                                    if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                                        dragging = true
                                    } else {
                                        cancelledByVertical = true
                                        break
                                    }
                                }
                            }

                            if (dragging) {
                                pickIndex(change.position.x)?.let { idx ->
                                    setActive(idx)
                                }
                                change.consume() // 只在水平拖曳 consume
                            }
                        }

                        // ✅ Tap：切換 pin / unpin
                        if (!cancelledByVertical && !dragging) {
                            pinnedIndex = if (pinnedIndex == downIdx) null else downIdx
                        }

                        // ✅ FIX：若已經有 pinned，且使用者水平拖曳過
                        // 放手時把 pinned 更新到最後一個 active（不會跳回舊 pinned）
                        if (!cancelledByVertical && dragging && pinnedIndex != null) {
                            pinnedIndex = activeIndex ?: pinnedIndex
                        }

                        // 手離開：active 永遠收掉；顯示與否交給 pinnedIndex
                        activeIndex = null
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(
                        start = startPaddingDp,
                        end = endPaddingDp,
                        top = topPaddingDp,
                        bottom = bottomPaddingDp
                    )
            ) {
                val w = size.width
                val h = size.height
                val r = 0f

                val clipRound = CornerRadius(r, r)
                val rrClip = androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, clipRound)
                val clipPath = Path().apply { addRoundRect(rrClip) }

                withTransform({ clipPath(clipPath) }) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0xFFF4F4F5),
                            1f to Color(0xFFFFFFFF)
                        ),
                        cornerRadius = clipRound,
                        size = size
                    )

                    val gridCount = 4
                    repeat(gridCount + 1) { i ->
                        val y = h * (i / gridCount.toFloat())
                        drawLine(
                            color = Color(0xFFE5E7EB),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val allPoints = chartData.points
                    if (allPoints.isEmpty()) {
                        // no-op
                    } else if (allPoints.size == 1) {
                        val highlightGreen = Color(0xFF22C55E)
                        val baseStroke = 2.1.dp.toPx()

                        val xSingle = allPoints[0].x * w
                        val ySingle = allPoints[0].y * h

                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color(0xFF111114).copy(alpha = 0.15f),
                                1f to Color.Transparent
                            ),
                            topLeft = Offset(0f, ySingle),
                            size = Size(w, h - ySingle)
                        )

                        val isActive = (shownIndex == 0)

                        drawLine(
                            color = if (isActive) highlightGreen else Color(0xFF111114),
                            start = Offset(0f, ySingle),
                            end = Offset(w, ySingle),
                            strokeWidth = baseStroke,
                            cap = StrokeCap.Round
                        )

                        if (isActive) {
                            val circleOuter = 5.dp.toPx()
                            val circleInner = 3.dp.toPx()
                            val circleHalo = 8.dp.toPx()

                            drawLine(
                                color = Green.copy(alpha = 0.45f),
                                start = Offset(xSingle, 0f),
                                end = Offset(xSingle, h),
                                strokeWidth = 1.5.dp.toPx()
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
                        val highlightGreen = Color(0xFF22C55E)
                        val baseStroke = 2.1.dp.toPx()
                        val halfStroke = baseStroke / 2f

                        val xsAll = allPoints.map { it.x * w }
                        val ysAll = allPoints.map { it.y * h }

                        val linePathAll = buildCatmullRomPath(xsAll, ysAll)
                        val areaPathAll = Path().apply {
                            addPath(linePathAll)
                            lineTo(xsAll.last(), h)
                            lineTo(xsAll.first(), h)
                            close()
                        }

                        drawPath(
                            path = areaPathAll,
                            brush = Brush.verticalGradient(
                                0f to Color(0xFF111114).copy(alpha = 0.15f),
                                1f to Color.Transparent
                            )
                        )

                        drawPath(
                            path = linePathAll,
                            color = Color(0xFF111114),
                            style = Stroke(width = baseStroke, cap = StrokeCap.Round)
                        )

                        val idx = shownIndex
                        if (idx != null && idx in xsAll.indices) {
                            val xSel = xsAll[idx]
                            val ySel = ysAll[idx]

                            val circleOuter = 5.dp.toPx()
                            val circleInner = 3.dp.toPx()
                            val circleHalo = 8.dp.toPx()

                            val leftClip = -halfStroke - 2f
                            val rightClip = w + halfStroke + 2f

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
                                    style = Stroke(width = baseStroke, cap = StrokeCap.Round)
                                )
                            }

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
                                    brush = Brush.verticalGradient(
                                        0f to Color(0xFF111114).copy(alpha = 0.16f),
                                        0.55f to Color(0xFF111114).copy(alpha = 0.10f),
                                        1f to Color.Transparent
                                    )
                                )
                            }

                            drawLine(
                                color = Green.copy(alpha = 0.45f),
                                start = Offset(xSel, 0f),
                                end = Offset(xSel, h),
                                strokeWidth = 1.5.dp.toPx()
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

            // Y 軸標籤
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .offset(x = (-4).dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                chartData.yLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.60f)
                    )
                }
            }

            // Tooltip（用 shownIndex）
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

                val tooltipWidthPx = with(density) { 98.dp.toPx() }
                var tx = centerX - tooltipWidthPx / 2f

                val paddingPx = with(density) { 8.dp.toPx() }
                val minX = paddingPx
                val maxX = chartWidthPx - tooltipWidthPx - paddingPx
                tx = tx.coerceIn(minX, maxX)

                val ty = centerY - with(density) { 52.dp.toPx() }

                WeightTooltip(
                    weightKg = chartData.weightsKg[idx],
                    weightLbs = chartData.weightsLbs.getOrNull(idx),
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

        Spacer(Modifier.height(6.dp))

        // X 軸粗體選中：用 shownIndex
        val selectedLabelIndex: Int? = shownIndex
            ?.takeIf { it in chartData.dates.indices }
            ?.let { pi ->
                val target = chartData.dates[pi]
                if (chartData.axisDates.isEmpty()) return@let null
                chartData.axisDates.withIndex()
                    .minByOrNull { (_, d) -> kotlin.math.abs(d.toEpochDay() - target.toEpochDay()) }
                    ?.index
            }

        val textMeasurer = rememberTextMeasurer()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPaddingDp, end = endPaddingDp)
                .height(24.dp)
        ) {
            if (chartWidthPx > 0f && chartData.axisDates.isNotEmpty()) {

                val innerWidthPx =
                    (chartWidthPx - startPaddingPx - endPaddingPx).coerceAtLeast(1f)

                val centersPx: List<Float> = chartData.axisX.map { it * innerWidthPx }

                val baseMinGapPx = with(density) { 8.dp.toPx() }
                val edgePaddingPx = with(density) { 4.dp.toPx() }
                val desiredMinCount = minOf(4, chartData.axisDates.size)

                val measureStyle = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val formatters = listOf(
                    DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("MM/dd", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("M/d", Locale.ENGLISH)
                )

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
                        fontSize = 12.sp,
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
    // 顏色你可以再換成你剛一起調的那組
    val bg = Color(0xFFEFF9F4)   // 超淡綠背景（比純白微微帶綠）
    val fg = Color(0xFF12823B)   // 草綠文字

    Box(
        modifier = modifier
            // ⭐ 不要 fillMaxWidth、不要固定 height
            // 只做圓角 + 背景，大小交給內容與 padding 決定
            .clip(RoundedCornerShape(14.dp)) // 大數字確保是膠囊形狀
            .background(bg)
            .padding(horizontal = 18.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
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

    // 日期（走系統 Locale）
    val dateText = runCatching {
        val d = LocalDate.parse(item.logDate)
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(d)
    }.getOrElse { item.logDate }

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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)              // ★ Row 高度變大
            .border(1.dp, border, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側圖片（變大）
            val imageShape = RoundedCornerShape(14.dp)
            if (item.photoUrl != null) {
                AsyncImage(
                    model = item.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(58.dp)           // ★ Image 變大
                        .clip(imageShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.weight_image),
                    contentDescription = null,
                    modifier = Modifier
                        .size(58.dp)           // ★ Image 變大
                        .clip(imageShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 上排：WEIGHT / CHANGE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "WEIGHT",
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // ★ 當前體重數字往下移一點點
                        Text(
                            text = weightText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.offset(y = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CHANGE",
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = label
                        )
                        Text(
                            text = deltaText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor     // ★ 正紅負綠持平藍
                        )
                    }
                }

                Spacer(Modifier.height(8.dp)) // ★ 修正：不要用 padding 當 Spacer

                // 下排：日期 + 趨勢 chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateText,
                        fontSize = 12.sp,
                        color = subText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    MiniChip(
                        text = chipText,
                        bg = chipBg,
                        fg = chipFg
                    )
                }
            }
        }
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
    return String.format("%s%.1f %s", sign, absV, unitText)
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
            FilterTabs(selected = "all", onSelect = {})
        }
    }
}

// ★ 新增：Catmull-Rom cubic spline，讓曲線通過每一個點
private fun buildCatmullRomPath(
    xs: List<Float>,
    ys: List<Float>,
    tension: Float = 0.5f   // 0.0 = 折線、1.0 = 很彎，0.5 較穩定
): Path {
    val path = Path()
    val n = xs.size
    if (n == 0) return path
    if (n == 1) {
        path.moveTo(xs[0], ys[0])
        return path
    }

    path.moveTo(xs[0], ys[0])

    val last = n - 1
    for (i in 0 until last) {
        // P0, P1, P2, P3
        val p0x = if (i == 0) xs[i] else xs[i - 1]
        val p0y = if (i == 0) ys[i] else ys[i - 1]

        val p1x = xs[i]
        val p1y = ys[i]

        val p2x = xs[i + 1]
        val p2y = ys[i + 1]

        val p3x = if (i + 1 == last) xs[i + 1] else xs[i + 2]
        val p3y = if (i + 1 == last) ys[i + 1] else ys[i + 2]

        val t = tension
        // Catmull-Rom 轉 cubic 的控制點公式
        val c1x = p1x + (p2x - p0x) / 6f * t
        val c1y = p1y + (p2y - p0y) / 6f * t
        val c2x = p2x - (p3x - p1x) / 6f * t
        val c2y = p2y - (p3y - p1y) / 6f * t

        path.cubicTo(c1x, c1y, c2x, c2y, p2x, p2y)
    }

    return path
}

/**
 * TO TARGET 專用（修正版）：
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
            kg?.let { String.format("%.1f kg", it) } ?: "—"
        }
        UserProfileStore.WeightUnit.LBS -> {
            // LBS 模式：只看 DB 的 lbs，沒有就顯示破折號
            lbs?.let { String.format("%.1f lbs", it) } ?: "—"
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
            String.format("%.1f kg", weightKg)
        }
        UserProfileStore.WeightUnit.LBS -> {
            // 這裡也一樣：先用 DB 的 lbs
            val lbs = weightLbs ?: kgToLbs1(weightKg)
            String.format("%.1f lbs", lbs)
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
