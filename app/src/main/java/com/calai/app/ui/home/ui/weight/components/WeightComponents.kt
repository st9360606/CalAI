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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment

// ----------------------------------------------------------
// Summary Cards
// ----------------------------------------------------------
val Green = Color(0xFF22C55E)
@Composable
fun SummaryCards(ui: WeightViewModel.UiState) {
    val cardBg  = Color(0xFFF5F5F5)
    val label   = Color.Black.copy(alpha = 0.60f)
    val big     = Color(0xFF111114)
    val divider = Color(0xFFE2E5EA)
    val edgeTx  = Color(0xFF111114).copy(alpha = 0.88f)
    val arrow   = Color(0xFF6B7280)

    val trackColor  = Color(0xFFDCE1E7)
    val stripeColor = Color.White.copy(alpha = 0.35f)
    val fillColor   = Color(0xFFFF8A33).copy(alpha = 0.85f)

    val unit = ui.unit

    val currentKg = ui.current ?: ui.profileWeightKg
    val goalKg    = ui.profileTargetWeightKg ?: ui.goal

    val gainedText = formatDeltaGoalMinusCurrent(
        goalKg = goalKg,
        currentKg = currentKg,
        unit = unit,
        lbsAsInt = (unit == UserProfileStore.WeightUnit.LBS)
    )

    val pr = computeWeightProgress(
        timeseries      = ui.series,
        currentKg       = currentKg,
        goalKg          = goalKg,
        profileWeightKg = ui.profileWeightKg
    )
    val progress = pr.fraction

    val edgeLeft  = formatWeightCard(currentKg, unit, lbsAsInt = (unit == UserProfileStore.WeightUnit.LBS))
    val edgeRight = formatWeightCard(goalKg, unit, lbsAsInt = (unit == UserProfileStore.WeightUnit.LBS))

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
                    Text(
                        text = formatWeightCard(
                            currentKg,
                            unit,
                            lbsAsInt = (unit == UserProfileStore.WeightUnit.LBS)
                        ),
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
    Box(Modifier.fillMaxWidth().height(height)) {
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

private fun kgToLbs(kg: Double): Double = kg * 2.20462262

fun formatWeightCard(
    kg: Double?,
    unit: UserProfileStore.WeightUnit,
    lbsAsInt: Boolean
): String {
    if (kg == null) return "—"
    return if (unit == UserProfileStore.WeightUnit.KG) {
        String.format("%.1f kg", kg)
    } else {
        val lbs = kgToLbs(kg)
        if (lbsAsInt) {
            val rounded = kotlin.math.round(lbs).toInt()
            String.format("%d lb", rounded)
        } else {
            String.format("%.1f lb", lbs)
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
    return if (unit == UserProfileStore.WeightUnit.KG) {
        String.format("%s%.1f kg", sign, abs(diffKg))
    } else {
        val lbs = kgToLbs(abs(diffKg))
        val v = if (lbsAsInt) kotlin.math.round(lbs).toInt().toString()
        else String.format("%.1f", lbs)
        "$sign$v lb"
    }
}

fun formatWeight(kg: Double?, unit: UserProfileStore.WeightUnit): String {
    if (kg == null) return "—"
    return if (unit == UserProfileStore.WeightUnit.KG) {
        String.format("%.1f kg", kg)
    } else {
        String.format("%.1f lb", kg * 2.20462262)
    }
}

fun formatDelta(deltaKg: Double, unit: UserProfileStore.WeightUnit): String {
    val sign = if (deltaKg >= 0) "+" else "−"
    return if (unit == UserProfileStore.WeightUnit.KG) {
        String.format("%s%.1f kg", sign, abs(deltaKg))
    } else {
        String.format("%s%.1f lb", sign, abs(deltaKg) * 2.20462262)
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
    startWeightAllTimeKg: Double? = null
) {
    val unit          = ui.unit
    val currentKg     = ui.current ?: ui.profileWeightKg
    val goalKg        = ui.profileTargetWeightKg ?: ui.goal
    val profileWeight = ui.profileWeightKg

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
                GoalProgressBadge(progressPercent = progressPercent)
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
    modifier: Modifier = Modifier
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
    val xLabels: List<String>,              // 由左到右（軸上刻度）
    val points: List<ChartPointNormalized>, // 折線所有點（正規化）
    val dates: List<LocalDate>,             // 每個資料點的日期（對應 points）
    val weightsKg: List<Double>,            // 每個資料點的體重（對應 points）
    val axisDates: List<LocalDate>          // X 軸刻度實際日期（對應 xLabels）
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
            yLabels = emptyList(),
            xLabels = emptyList(),
            points = emptyList(),
            dates = emptyList(),
            weightsKg = emptyList(),
            axisDates = emptyList()
        )
    }

    // 1) 解析日期 + 排序（這裡就是折線的所有點）
    val sorted = series.mapNotNull { item ->
        runCatching { LocalDate.parse(item.logDate) }.getOrNull()
            ?.let { d -> d to item.weightKg }
    }.sortedBy { it.first }

    if (sorted.isEmpty()) {
        return WeightChartData(
            yLabels = emptyList(),
            xLabels = emptyList(),
            points = emptyList(),
            dates = emptyList(),
            weightsKg = emptyList(),
            axisDates = emptyList()
        )
    }

    val datesSorted   = sorted.map { it.first }
    val weightsSorted = sorted.map { it.second }

    // 全時段第一筆（後端算好的）> 沒有就用目前 slice 第一筆
    val startKg = startWeightAllTimeKg ?: weightsSorted.first()

    // 區間內實際 min / max
    val dataMin = weightsSorted.minOrNull()!!
    val dataMax = weightsSorted.maxOrNull()!!

    val effGoal    = goalKg    ?: currentKg ?: startKg
    val effCurrent = currentKg ?: startKg

    // 2) 先決定「原始」上下界（包住 start / goal / current / 區間 min/max）
    var topKg: Double
    var bottomKg: Double

    if (goalKg != null && currentKg != null) {
        if (goalKg > currentKg) {
            // 增重：目標 > 當前
            topKg = maxOf(goalKg, dataMax, startKg)
            bottomKg = minOf(startKg, dataMin)
        } else if (goalKg < currentKg) {
            // 減重：目標 < 當前
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

    // 3) 加上上下 5% margin，避免線貼死邊界
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
    val axisDates = buildXAxisDates(allDates, maxLabels = 5)
    val xLabels   = axisDates.map { axisDateFormatter.format(it) }

    val firstDay = allDates.first().toEpochDay()
    val lastDay  = allDates.last().toEpochDay()
    val daySpan  = (lastDay - firstDay).coerceAtLeast(1L)

    // 5) 正規化點
    val points = datesSorted.zip(weightsSorted).map { (date, wKg) ->
        val x = ((date.toEpochDay() - firstDay).toFloat() / daySpan.toFloat())
            .coerceIn(0f, 1f)
        val clamped = wKg.coerceIn(bottomKg, topKg)
        val y = (((topKg - clamped) / span).toFloat()).coerceIn(0f, 1f)
        ChartPointNormalized(x, y)
    }

    return WeightChartData(
        yLabels   = yLabels,
        xLabels   = xLabels,
        points    = points,
        dates     = datesSorted,
        weightsKg = weightsSorted,
        axisDates = axisDates
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

/** X 軸：最多 maxLabels 個日期，最左最右一定存在 */
private fun buildXAxisDates(
    dates: List<LocalDate>,
    maxLabels: Int
): List<LocalDate> {
    if (dates.isEmpty()) return emptyList()
    val sorted = dates.distinct().sorted()
    val count  = sorted.size
    if (count <= maxLabels) return sorted

    val result    = mutableListOf<LocalDate>()
    val lastIndex = count - 1
    for (i in 0 until maxLabels) {
        val index = (lastIndex * i) / (maxLabels - 1)
        val d = sorted[index]
        if (result.isEmpty() || result.last() != d) {
            result.add(d)
        }
    }
    return result
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
// ❶ 確保切換單位時重組
@Composable
private fun WeightTooltip(
    weightKg: Double,
    unit: UserProfileStore.WeightUnit,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    key(unit) {
        val weightText = formatWeightCard(
            kg = weightKg,
            unit = unit,
            lbsAsInt = unit == UserProfileStore.WeightUnit.LBS
        )
        val dateText = tooltipDateFormatter.format(date)

        Box(
            modifier = modifier
                // ★ 寬度縮小一點：160 → 140
                .width(98.dp)
                // ★ 透明度增加：0.92 → 0.85（更輕）
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
        series               = series,
        unit                 = unit,
        currentKg            = currentKg,
        goalKg               = goalKg,
        profileWeightKg      = profileWeightKg,
        startWeightAllTimeKg = startWeightAllTimeKg
    )

    // ★ 互動狀態：指向「第幾個資料點」（不是 xLabels）
    //   把 unit 加進 key，切換 kg / lb 時會重置狀態，避免 tooltip 卡住舊值
    var activeIndex by remember(chartData.points.size, unit) {
        mutableStateOf<Int?>(null)
    }

    // ★ 圖表實際寬高（px）
    var chartWidthPx by remember { mutableStateOf(0f) }
    var chartHeightPx by remember { mutableStateOf(0f) }

    // ★ 跟左右 / 上下 padding 對齊（Canvas 內部）
    val density = LocalDensity.current
    val startPaddingDp = 40.dp
    val endPaddingDp   = 6.dp
    val topPaddingDp   = 8.dp
    val bottomPaddingDp = 8.dp

    val startPaddingPx = with(density) { startPaddingDp.toPx() }
    val endPaddingPx   = with(density) { endPaddingDp.toPx() }
    val topPaddingPx   = with(density) { topPaddingDp.toPx() }
    val bottomPaddingPx = with(density) { bottomPaddingDp.toPx() }

    // ★ 將手指 x 座標轉成「第幾個資料點」
    fun updateActiveIndex(rawX: Float) {
        val pointCount = chartData.points.size
        if (pointCount == 0 || chartWidthPx <= 0f) return

        if (pointCount == 1) {
            activeIndex = 0
            return
        }

        val minX = startPaddingPx
        val maxX = chartWidthPx - endPaddingPx
        val effectiveWidth = (maxX - minX).coerceAtLeast(1f)

        val clamped = rawX.coerceIn(minX, maxX)
        val t = (clamped - minX) / effectiveWidth   // 0f..1f
        val segments = pointCount - 1
        val idx = (t * segments.toFloat()).roundToInt().coerceIn(0, segments)

        activeIndex = idx
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // 取得 Box 實際寬高，用來換算 touch / tooltip 位置
                .onSizeChanged {
                    chartWidthPx = it.width.toFloat()
                    chartHeightPx = it.height.toFloat()
                }
                // ★ 低階手勢：一碰就生效，然後可以拖曳
                .pointerInput(chartData.points.size) {
                    awaitEachGesture {
                        // ❶ TAP：第一次觸碰立即生效
                        val down = awaitFirstDown(requireUnconsumed = false)
                        updateActiveIndex(down.position.x)

                        // ❷ DRAG：持續更新
                        drag(down.id) { change ->
                            updateActiveIndex(change.position.x)
                            change.consume()
                        }

                        // ❸ UP：放開就清除 tooltip
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

                // ❷ 定義整個繪圖區圓角裁切（避免任何筆畫超出邊界）
                val clipRound = CornerRadius(r, r)
                val rrClip = androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, clipRound)
                val clipPath = Path().apply { addRoundRect(rrClip) }

                withTransform({
                    clipPath(clipPath)   // **所有繪製都在圓角區域內**
                }) {
                    // 背景
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0xFFF4F4F5),
                            1f to Color(0xFFFFFFFF)
                        ),
                        cornerRadius = clipRound,
                        size = size
                    )

                    // 水平網格
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
                    if (allPoints.size >= 2) {
                        val highlightGreen = Color(0xFF22C55E)
                        // 基礎線條粗細
                        val baseStroke = 2.1.dp.toPx()
                        val halfStroke = baseStroke / 2f

                        val xsAll = allPoints.map { it.x * w }
                        val ysAll = allPoints.map { it.y * h }

                        fun buildSmoothPath(xs: List<Float>, ys: List<Float>): Path {
                            val p = Path()
                            if (xs.isEmpty()) return p
                            p.moveTo(xs.first(), ys.first())
                            for (i in 1 until xs.size) {
                                val prevX = xs[i - 1]
                                val prevY = ys[i - 1]
                                val currX = xs[i]
                                val currY = ys[i]
                                val midX = (prevX + currX) / 2f
                                val midY = (prevY + currY) / 2f
                                p.quadraticBezierTo(prevX, prevY, midX, midY)
                            }
                            if (xs.size >= 2) {
                                val prevX = xs[xs.size - 2]
                                val prevY = ys[xs.size - 2]
                                p.quadraticBezierTo(prevX, prevY, xs.last(), ys.last())
                            }
                            return p
                        }

                        val linePathAll = buildSmoothPath(xsAll, ysAll)
                        val areaPathAll = Path().apply {
                            addPath(linePathAll)
                            lineTo(xsAll.last(), h)
                            lineTo(xsAll.first(), h)
                            close()
                        }

                        // ❸ 先畫「全域底部灰色漸層」（無論是否有 activeIndex 都存在）
                        drawPath(
                            path = areaPathAll,
                            brush = Brush.verticalGradient(
                                0f to Color(0xFF111114).copy(alpha = 0.15f), // 上方淡灰
                                1f to Color.Transparent                       // 下方透明
                            )
                        )

                        // ❹ 再畫「完整黑線」作為基準
                        drawPath(
                            path = linePathAll,
                            color = Color(0xFF111114),
                            style = Stroke(width = baseStroke, cap = StrokeCap.Round)
                        )

                        val idx = activeIndex
                        if (idx != null && idx in xsAll.indices) {
                            val xSel = xsAll[idx]
                            val ySel = ysAll[idx]

                            // ★ 綠圓尺寸（縮小一點）
                            val circleOuter = 5.dp.toPx()   // 主綠圓半徑
                            val circleInner = 3.dp.toPx()   // 白心半徑
                            val circleHalo  = 8.dp.toPx()   // 淡綠光暈半徑

                            // **補償值**：用於左 / 右半段 clip，不動線段分割邏輯
                            val leftClip = -halfStroke - 2f
                            val rightClip = w + halfStroke + 2f

                            // ❹ 左半段 (0..xSel) 綠色覆蓋
                            withTransform({
                                clipRect(left = leftClip, top = 0f, right = xSel + halfStroke, bottom = h)
                            }) {
                                // 綠色底
                                drawPath(
                                    path = areaPathAll,
                                    brush = Brush.verticalGradient(
                                        0f    to highlightGreen.copy(alpha = 0.24f),
                                        0.55f to highlightGreen.copy(alpha = 0.16f),
                                        1f    to Color.Transparent
                                    )
                                )
                                // 綠線覆蓋黑線（無光暈）
                                drawPath(
                                    path = linePathAll,
                                    color = highlightGreen,
                                    style = Stroke(width = baseStroke, cap = StrokeCap.Round)
                                )
                            }

                            // ❺ 右半段 (xSel..end) 灰底
                            withTransform({
                                clipRect(left = xSel - halfStroke, top = 0f, right = rightClip, bottom = h)
                            }) {
                                drawPath(
                                    path = areaPathAll,
                                    brush = Brush.verticalGradient(
                                        0f    to Color(0xFF111114).copy(alpha = 0.16f),
                                        0.55f to Color(0xFF111114).copy(alpha = 0.10f),
                                        1f    to Color.Transparent
                                    )
                                )
                            }

                            // -------------------------------
                            // ★ 關鍵：垂直線＋圓點全部用 xSel
                            //     這樣就會跟最左邊那條 Y 軸直線「同一條」，直接蓋住
                            // -------------------------------

                            // 垂直輔助線（綠色蓋住原本的直線）
                            drawLine(
                                color = Green.copy(alpha = 0.45f),
                                start = Offset(xSel, 0f),
                                end   = Offset(xSel, h),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // 淡綠光暈
                            drawCircle(
                                color = Green.copy(alpha = 0.28f),
                                radius = circleHalo,
                                center = Offset(xSel, ySel)
                            )

                            // 主綠圓（小一點）
                            drawCircle(
                                color = Green,
                                radius = circleOuter,
                                center = Offset(xSel, ySel)
                            )

                            // 內圈白色
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

            // Tooltip 疊在上面（放在綠圓的上方）
            val idx = activeIndex
            if (
                idx != null &&
                idx in chartData.dates.indices &&
                chartWidthPx > 0f &&
                chartHeightPx > 0f
            ) {
                val pointNorm = chartData.points[idx]

                val innerWidthPx  = chartWidthPx  - startPaddingPx - endPaddingPx
                val innerHeightPx = chartHeightPx - topPaddingPx  - bottomPaddingPx

                // 與綠圓同一組半徑
                val circleOuterPx = with(density) { 5.dp.toPx() }

                // 先算出未保護的 X（在 Canvas 內部座標）
                val baseXInner = pointNorm.x * innerWidthPx

                // 安全 X（避免被圓角或 tooltip 邊界吃掉）
                val xSafeInner = baseXInner.coerceIn(circleOuterPx, innerWidthPx - circleOuterPx)

                // 真正的中心 X / Y（含 padding）
                val centerX = startPaddingPx + xSafeInner
                val centerY = topPaddingPx    + pointNorm.y * innerHeightPx

                // Tooltip 寬度
                val tooltipWidthPx = with(density) { 98.dp.toPx() }  // 跟 WeightTooltip 的 width 對齊

                // ❶ 以圓點為中心
                var tx = centerX - tooltipWidthPx / 2f

                // ❷ 邊界保護：左右各保留 8dp padding
                val paddingPx = with(density) { 8.dp.toPx() }
                val minX = paddingPx
                val maxX = chartWidthPx - tooltipWidthPx - paddingPx
                tx = tx.coerceIn(minX, maxX)

                // ❸ 垂直位置：圓點上方 52dp（比原本 56dp 稍微貼近）
                val ty = centerY - with(density) { 52.dp.toPx() }

                WeightTooltip(
                    weightKg = chartData.weightsKg[idx],
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

        // X 軸標籤：選到的日期附近那個加粗
        val selectedLabelIndex: Int? = activeIndex?.let { pi ->
            if (pi !in chartData.dates.indices) {
                null
            } else {
                val targetEpoch = chartData.dates[pi].toEpochDay()
                var bestIndex = 0
                var bestDiff = Long.MAX_VALUE
                chartData.axisDates.forEachIndexed { idx, d ->
                    val diff = kotlin.math.abs(d.toEpochDay() - targetEpoch)
                    if (diff < bestDiff) {
                        bestDiff = diff
                        bestIndex = idx
                    }
                }
                bestIndex
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPaddingDp, end = endPaddingDp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            chartData.xLabels.forEachIndexed { index, label ->
                val selected = selectedLabelIndex != null && index == selectedLabelIndex
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected)
                        Color(0xFF111114)
                    else
                        Color.Black.copy(alpha = 0.60f)
                )
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

@Composable
fun HistoryRow(item: WeightItemDto, unit: UserProfileStore.WeightUnit) {
    Card {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.photoUrl != null) {
                AsyncImage(
                    model = item.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.weight_image),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(formatWeight(item.weightKg, unit))
                Text(item.logDate, color = Color.Gray)
            }
        }
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
