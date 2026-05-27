package com.calai.bitecal.ui.home.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.DayOfWeek

private object CalendarStripColors {
    val SelectedBackground = HomeCardStyles.Calendar.SelectedBackground
    val TodayBackground = HomeCardStyles.Calendar.TodayBackground
    val ActiveText = HomeCardStyles.Calendar.ActiveText
    val DisabledText = HomeCardStyles.Calendar.DisabledText
    val ActiveStroke = HomeCardStyles.Calendar.ActiveStroke
    val DisabledStroke = HomeCardStyles.Calendar.DisabledStroke
    val OnTargetStroke = HomeCardStyles.Calendar.OnTargetStroke
    val SlightlyOverStroke = HomeCardStyles.Calendar.SlightlyOverStroke
    val FarOverStroke = HomeCardStyles.Calendar.FarOverStroke
    val NoMealStroke = HomeCardStyles.Calendar.NoMealStroke
    val TodayNoMealStroke = HomeCardStyles.Calendar.TodayNoMealStroke
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CalendarStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    disableFuture: Boolean = true,
    caloriesByDate: Map<LocalDate, Int> = emptyMap(),
    dailyGoalKcal: Int = 0,
    selectedBgWidthFraction: Float = 0.83f,
    selectedBgCorner: Dp = 16.dp,
    itemHeight: Dp = 74.dp,
) {
    val today = LocalDate.now()

    // 顯示範圍由呼叫端控制，避免元件內部 hard-code 造成 Home 想顯示 30 天時仍被裁切。
    val visibleDays = remember(days) {
        days.distinct().sorted()
    }

    // Home 需要第一屏固定露出「前 4 天 + 今天 + 未來 1 天」，所以這裡以實際可用寬度切 6 欄。
    // 不可用 LocalConfiguration.screenWidthDp，因為 CalendarStrip 外層有 horizontal padding，
    // 用整個螢幕寬度會把 item 算太寬，導致明天雖然在資料中，卻被擠到右側需要滑動才看得到。
    val visibleCount = 6
    val spacing = 7.dp
    val minItemWidth = 40.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val itemWidth: Dp = remember(maxWidth) {
            ((maxWidth - spacing * (visibleCount - 1)) / visibleCount)
                .coerceAtLeast(minItemWidth)
        }

        val initialFirstVisibleIndex = remember(visibleDays, today) {
            val todayIndex = visibleDays.indexOf(today).coerceAtLeast(0)
            val maxFirstVisibleIndex = (visibleDays.size - visibleCount).coerceAtLeast(0)

            // 只在 CalendarStrip 第一次進入畫面時定位：
            // 讓今天落在第 5 格，第一屏顯示「前 4 天 + 今天 + 明天」。
            // 注意：不要把 selected 放進 remember key，也不要在 selected 改變時 scrollToItem，
            // 否則使用者點其他日期時，LazyRow 會自動跳動，手感會很差。
            (todayIndex - 4).coerceIn(0, maxFirstVisibleIndex)
        }

        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialFirstVisibleIndex
        )

        val dashedPath = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                count = visibleDays.size,
                key = { i -> visibleDays[i].toEpochDay() },
                contentType = { _ -> "day" }
            ) { i ->
                val d = visibleDays[i]
                val isSelected = d == selected
                val isToday = d == today
                val isFuture = d.isAfter(today)
                val enabled = !(disableFuture && isFuture)
                val ringStyle = calendarRingStyleForDate(
                    date = d,
                    today = today,
                    enabled = enabled,
                    caloriesByDate = caloriesByDate,
                    dailyGoalKcal = dailyGoalKcal
                )

                val baseContainer = Modifier
                    .width(itemWidth)
                    .height(itemHeight)

                when {
                    isSelected -> {
                        // 選中的那天：使用 HomeCardStyles.Calendar 管理的金棕色底
                        Box(
                            modifier = baseContainer
                                .clickable { onSelect(d) }
                                .drawBehind {
                                    val fraction = selectedBgWidthFraction.coerceIn(0.6f, 1f)
                                    val chipW = size.width * fraction
                                    val chipH = size.height
                                    val left = (size.width - chipW) / 2f
                                    drawRoundRect(
                                        color = CalendarStripColors.SelectedBackground.copy(alpha = 0.81f),
                                        topLeft = Offset(left, 0f),
                                        size = Size(chipW, chipH),
                                        cornerRadius = CornerRadius(
                                            selectedBgCorner.toPx(),
                                            selectedBgCorner.toPx()
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DayDot(
                                date = d,
                                width = itemWidth,
                                enabled = true,
                                style = ringStyle.dotStyle,
                                ringColor = ringStyle.strokeColor,
                                dashedPath = dashedPath,
                                onClick = {},
                                useClickable = false,
                                isSelected = true
                            )
                        }
                    }

                    isToday && (selected != today) -> {
                        // 今天但未被選：柔和灰底
                        Box(
                            modifier = baseContainer
                                .clickable(enabled = enabled) { if (enabled) onSelect(d) }
                                .drawBehind {
                                    val fraction = selectedBgWidthFraction.coerceIn(0.6f, 1f)
                                    val chipW = size.width * fraction
                                    val chipH = size.height
                                    val left = (size.width - chipW) / 2f
                                    drawRoundRect(
                                        color = CalendarStripColors.TodayBackground.copy(alpha = 0.25f),
                                        topLeft = Offset(left, 0f),
                                        size = Size(chipW, chipH),
                                        cornerRadius = CornerRadius(
                                            selectedBgCorner.toPx(),
                                            selectedBgCorner.toPx()
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DayDot(
                                date = d,
                                width = itemWidth,
                                enabled = enabled,
                                style = ringStyle.dotStyle,
                                ringColor = ringStyle.strokeColor,
                                dashedPath = dashedPath,
                                onClick = { if (enabled) onSelect(d) }
                            )
                        }
                    }

                    else -> {
                        // 其他（一般日或未來日）
                        Box(
                            modifier = baseContainer
                                .clickable(enabled = enabled) { if (enabled) onSelect(d) },
                            contentAlignment = Alignment.Center
                        ) {
                            DayDot(
                                date = d,
                                width = itemWidth,
                                enabled = enabled,
                                style = ringStyle.dotStyle,
                                ringColor = ringStyle.strokeColor,
                                dashedPath = dashedPath,
                                onClick = { if (enabled) onSelect(d) },
                                useClickable = false
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class DotStyle { Dashed, SolidStroke }

private data class CalendarDayRingStyle(
    val dotStyle: DotStyle,
    val strokeColor: Color
)

private fun calendarRingStyleForDate(
    date: LocalDate,
    today: LocalDate,
    enabled: Boolean,
    caloriesByDate: Map<LocalDate, Int>,
    dailyGoalKcal: Int
): CalendarDayRingStyle {
    if (!enabled || date.isAfter(today)) {
        return CalendarDayRingStyle(
            dotStyle = DotStyle.SolidStroke,
            strokeColor = CalendarStripColors.DisabledStroke
        )
    }

    val eatenKcal = caloriesByDate[date]?.coerceAtLeast(0) ?: 0
    if (eatenKcal <= 0 || dailyGoalKcal <= 0) {
        return CalendarDayRingStyle(
            dotStyle = DotStyle.Dashed,
            strokeColor = if (date == today) {
                CalendarStripColors.TodayNoMealStroke
            } else {
                CalendarStripColors.NoMealStroke
            }
        )
    }

    val overGoalKcal = eatenKcal - dailyGoalKcal
    val color = when {
        overGoalKcal >= 200 -> CalendarStripColors.FarOverStroke
        overGoalKcal >= 100 -> CalendarStripColors.SlightlyOverStroke
        else -> CalendarStripColors.OnTargetStroke
    }

    return CalendarDayRingStyle(
        dotStyle = DotStyle.SolidStroke,
        strokeColor = color
    )
}

/** 星期縮寫 → 間距 → 圓圈 + 日期數字 */
@Composable
private fun DayDot(
    date: LocalDate,
    width: Dp,
    enabled: Boolean,
    style: DotStyle,
    ringColor: Color,
    dashedPath: PathEffect,
    onClick: () -> Unit,
    useClickable: Boolean = true,
    isSelected: Boolean = false
) {
    val weekdayColor = CalendarStripColors.ActiveText
    val disabledStrokeColor = CalendarStripColors.DisabledStroke

    val textColor = if (enabled) Color.Black else CalendarStripColors.DisabledText
    val alpha = if (enabled) 1f else 0.85f

    // 固定 Canvas 外徑，並用 strokeWidth 反推 radius，避免 selected / future 圓圈看起來大小不同。
    // 實線代表已有餐點紀錄，視覺上放大一點；虛線代表無餐點紀錄，維持較輕量。
    val dottedDotSize = 34.dp //所有虛線的圓
    val solidDotSize = 38.dp // 實線的圓
    val futureDotSize = 36.dp // 只縮小未來一天的圓，其他日期不受影響
    val dotSize = when {
        !enabled && style == DotStyle.SolidStroke -> futureDotSize
        style == DotStyle.SolidStroke -> solidDotSize
        else -> dottedDotSize
    }
    val normalStrokeWidth = 1.9.dp
    val todayDashedStrokeWidth = 1.9.dp
    val emphasizedStrokeWidth = 2.5.dp
    val futureStrokeWidth = 1.75.dp
    val isToday = date == LocalDate.now()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .padding(vertical = 2.dp)
            .then(if (useClickable) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
    ) {
        Text(
            text = dayOfWeekAbbrev(date.dayOfWeek),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = weekdayColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        Spacer(Modifier.height(6.dp))

        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(dotSize)) {
                val strokeWidthPx = when {
                    !enabled && style == DotStyle.SolidStroke -> futureStrokeWidth.toPx()
                    isToday && style == DotStyle.Dashed -> todayDashedStrokeWidth.toPx()
                    isSelected -> emphasizedStrokeWidth.toPx()
                    style == DotStyle.SolidStroke -> emphasizedStrokeWidth.toPx()
                    else -> normalStrokeWidth.toPx()
                }
                val radius = (size.minDimension - strokeWidthPx) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val strokeColor = if (enabled) ringColor else disabledStrokeColor

                when (style) {
                    DotStyle.Dashed -> {
                        drawCircle(
                            color = strokeColor.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style = Stroke(
                                width = strokeWidthPx,
                                pathEffect = dashedPath
                            )
                        )
                    }

                    DotStyle.SolidStroke -> {
                        drawCircle(
                            color = strokeColor.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                }
            }

            Text(
                text = date.dayOfMonth.toString(),
                color = textColor.copy(alpha = alpha),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}


private fun dayOfWeekAbbrev(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}