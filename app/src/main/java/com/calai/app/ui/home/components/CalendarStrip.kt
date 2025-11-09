package com.calai.app.ui.home.components

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.DayOfWeek

@Composable
fun CalendarStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    disableFuture: Boolean = true,
    selectedBgWidthFraction: Float = 0.83f,
    selectedBgCorner: Dp = 16.dp,
    itemHeight: Dp = 74.dp,
) {
    val today = LocalDate.now()

    // 視窗：今天往回 19 天（含今天） + 未來 1 天（明天）
    val startDate = today.minusDays(19)
    val endDate = today.plusDays(1)
    val visibleDays = remember(days, today) {
        days.filter { !it.isBefore(startDate) && !it.isAfter(endDate) }
    }

    // 每屏至少 7 天
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val visibleCount = 7

    // ★ ④ 間距加大一點
    val spacing = 7.dp

    val MIN_ITEM_WIDTH = 56.dp
    val itemWidth: Dp = remember(screenWidth) {
        ((screenWidth - spacing * (visibleCount - 1)) / visibleCount)
            .coerceAtLeast(MIN_ITEM_WIDTH)
    }

    // 初始定位
    val initialIndex = remember(visibleDays, selected, today) {
        val selIdx = visibleDays.indexOf(selected)
        val idx = if (selIdx >= 0) selIdx else visibleDays.indexOf(today).coerceAtLeast(0)
        (idx - 3).coerceIn(0, (visibleDays.lastIndex).coerceAtLeast(0))
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    val dashedPath = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
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

            val baseContainer = Modifier
                .width(itemWidth)
                .height(itemHeight)

            when {
                isSelected -> {
                    // 選中的那天：白底填滿
                    Box(
                        modifier = baseContainer
                            .clickable { onSelect(d) }
                            .drawBehind {
                                val fraction = selectedBgWidthFraction.coerceIn(0.6f, 1f)
                                val chipW = size.width * fraction
                                val chipH = size.height
                                val left = (size.width - chipW) / 2f
                                drawRoundRect(
                                    color = Color(0xFFFF8A33).copy(alpha = 0.85f),
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
                            style = DotStyle.Dashed,
                            dashedPath = dashedPath,
                            onClick = {},
                            useClickable = false,
                            isSelected = true // ← 加上這行
                        )
                    }
                }

                isToday && (selected != today) -> {
                    // ★ ① 今天但未被選：淺白底「填滿」
                    Box(
                        modifier = baseContainer
                            .clickable(enabled = enabled) { if (enabled) onSelect(d) }
                            .drawBehind {
                                val fraction = selectedBgWidthFraction.coerceIn(0.6f, 1f)
                                val chipW = size.width * fraction
                                val chipH = size.height
                                val left = (size.width - chipW) / 2f
                                drawRoundRect(
                                    color = Color.Gray.copy(alpha = 0.25f), // 淺白
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
                            style = DotStyle.Dashed,
                            dashedPath = dashedPath,
                            onClick = { if (enabled) onSelect(d) }
                        )
                    }
                }

                else -> {
                    // 其他（一般日或未來日）
                    val dotStyle = if (isFuture) DotStyle.SolidStroke else DotStyle.Dashed
                    Box(
                        modifier = baseContainer
                            .clickable(enabled = enabled) { if (enabled) onSelect(d) },
                        contentAlignment = Alignment.Center
                    ) {
                        DayDot(
                            date = d,
                            width = itemWidth,
                            enabled = enabled,
                            style = dotStyle,
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

private enum class DotStyle { Dashed, SolidStroke }

/** 星期縮寫 → 間距 → 圓圈 + 日期數字 */
@Composable
private fun DayDot(
    date: LocalDate,
    width: Dp,
    enabled: Boolean,
    style: DotStyle,
    dashedPath: PathEffect,
    onClick: () -> Unit,
    useClickable: Boolean = true,
    isSelected: Boolean = false
) {
    val weekdayColor = Color.Black
    val dashedStrokeColor = Color(0xFF000000)
    val disabledStrokeColor = Color(0xFF9CA3AF)

    val futureStrokeColor = Color(0xFF9CA3AF)
    val futureStrokeWidthPx = 5f

    val textColor = if (enabled) Color.Black else Color(0xFF9CA3AF)
    val alpha = if (enabled) 1f else 0.85f

    val dotSize = 34.dp

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
                val radius = size.minDimension / 2f - 2f
                when (style) {
                    DotStyle.Dashed -> {
                        val dashedStrokeWidth = if (isSelected) 5f else 3f
                        drawCircle(
                            color = (if (enabled) dashedStrokeColor else disabledStrokeColor).copy(alpha = alpha),
                            radius = radius,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(
                                width = dashedStrokeWidth,
                                pathEffect = dashedPath
                            )
                        )
                    }

                    DotStyle.SolidStroke -> {
                        drawCircle(
                            color = futureStrokeColor.copy(alpha = alpha),
                            radius = radius,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = futureStrokeWidthPx)
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
