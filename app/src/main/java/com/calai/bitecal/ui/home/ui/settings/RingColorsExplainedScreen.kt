package com.calai.bitecal.ui.home.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.common.design.BiteCalTopBar

private object RingColorsExplainedTheme {
    val Ink = Color(0xFF111114)
    val InkSoft = Color(0xFF2F3137)
    val Muted = Color(0xFF6B7280)
    val Subtle = Color(0xFF8C929D)
    val Card = Color.White.copy(alpha = 0.92f)
    val CardSoft = Color(0xFFF8F8FA)
    val Border = Color(0xFFE2E5EA)
    val BorderSoft = Color(0xFFE9EBEF)
    val Green = Color(0xFF7DDF83)
    val Brown = Color(0xFFB45309)
    val Red = Color(0xFFD92D20)
    val Dotted = Color(0xFF555A60)
    val Future = Color(0xFFC1C7D0)
}

@Composable
fun RingColorsExplainedScreen(
    onBack: () -> Unit
) {
    var backConsumed by rememberSaveable { mutableStateOf(false) }
    val onBackDebounced = {
        if (!backConsumed) {
            backConsumed = true
            onBack()
        }
    }

    val scroll = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        LightHomeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                BiteCalTopBar(
                    title = stringResource(R.string.ring_colors_explained_title),
                    onBack = onBackDebounced
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp, bottom = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumHeaderCard()

                PremiumCalendarPreviewCard()

                RingLegendPanel()
            }
        }
    }
}

@Composable
private fun PremiumHeaderCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = RingColorsExplainedTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, RingColorsExplainedTheme.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFF8F9FB).copy(alpha = 0.94f),
                            Color.White.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumRingBadge(
                    tone = RingTone.Dotted,
                    modifier = Modifier.size(46.dp),
                    ringSize = 24.dp,
                    strokeWidth = 1.6.dp,
                    dark = false
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = stringResource(R.string.ring_colors_explained_intro),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RingColorsExplainedTheme.Muted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumRingBadge(
    tone: RingTone,
    modifier: Modifier = Modifier,
    ringSize: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
    dark: Boolean = false
) {
    val background = if (dark) {
        RingColorsExplainedTheme.Ink
    } else {
        Color.White
    }
    val borderColor = if (dark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        RingColorsExplainedTheme.Border
    }
    val badgeTone = if (dark) RingTone.BadgeLight else tone

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        RingStroke(
            tone = badgeTone,
            size = ringSize,
            strokeWidth = strokeWidth
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun PremiumCalendarPreviewCard() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, RingColorsExplainedTheme.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color(0xFFF8F8FA).copy(alpha = 0.96f),
                            Color.White.copy(alpha = 0.94f)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 260.dp, y = (-44).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB).copy(alpha = 0.92f))
            )

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .offset(x = (-30).dp, y = 132.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F6F8).copy(alpha = 0.82f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ring_colors_preview_brand),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = RingColorsExplainedTheme.Ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            )
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.ring_colors_explained),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RingColorsExplainedTheme.Muted,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dayWidth = remember(maxWidth) {
                        ((maxWidth - 6.dp * 6) / 7).coerceAtLeast(34.dp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_sun),
                            day = stringResource(R.string.ring_colors_preview_day_10),
                            tone = RingTone.Red,
                            width = dayWidth
                        )
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_mon),
                            day = stringResource(R.string.ring_colors_preview_day_11),
                            tone = RingTone.Green,
                            width = dayWidth
                        )
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_tue),
                            day = stringResource(R.string.ring_colors_preview_day_12),
                            tone = RingTone.Yellow,
                            width = dayWidth
                        )
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_wed),
                            day = stringResource(R.string.ring_colors_preview_day_13),
                            tone = RingTone.Green,
                            width = dayWidth
                        )
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_thu),
                            day = stringResource(R.string.ring_colors_preview_day_14),
                            tone = RingTone.Dotted,
                            width = dayWidth
                        )
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_fri),
                            day = stringResource(R.string.ring_colors_preview_day_15),
                            tone = RingTone.Dotted,
                            width = dayWidth
                        )
                        PremiumPreviewDay(
                            weekday = stringResource(R.string.progress_day_sat),
                            day = stringResource(R.string.ring_colors_preview_day_16),
                            tone = RingTone.Future,
                            width = dayWidth,
                            muted = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumPreviewDay(
    weekday: String,
    day: String,
    tone: RingTone,
    width: Dp,
    muted: Boolean = false
) {
    val labelColor = if (muted) {
        RingColorsExplainedTheme.Subtle
    } else {
        RingColorsExplainedTheme.InkSoft
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width)
    ) {
        Text(
            text = weekday,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        Spacer(Modifier.height(8.dp))

        Box(contentAlignment = Alignment.Center) {
            RingStroke(
                tone = tone,
                size = 36.dp,
                strokeWidth = 2.6.dp
            )

            Text(
                text = day,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = labelColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun RingLegendPanel() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = RingColorsExplainedTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, RingColorsExplainedTheme.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            LegendSectionHeader()

            Spacer(Modifier.height(10.dp))

            LegendRow(
                tone = RingTone.Green,
                title = stringResource(R.string.ring_colors_green_title),
                body = stringResource(R.string.ring_colors_green_body)
            )

            LegendRow(
                tone = RingTone.Yellow,
                title = stringResource(R.string.ring_colors_yellow_title),
                body = stringResource(R.string.ring_colors_yellow_body)
            )

            LegendRow(
                tone = RingTone.Red,
                title = stringResource(R.string.ring_colors_red_title),
                body = stringResource(R.string.ring_colors_red_body)
            )

            LegendRow(
                tone = RingTone.Dotted,
                title = stringResource(R.string.ring_colors_dotted_title),
                body = stringResource(R.string.ring_colors_dotted_body),
                showDivider = false
            )
        }
    }
}

@Composable
private fun LegendSectionHeader() {
    val headerShape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF8F9FB),
                        Color.White.copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = RingColorsExplainedTheme.BorderSoft,
                shape = headerShape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RingColorsExplainedTheme.Green,
                            RingColorsExplainedTheme.Brown,
                            RingColorsExplainedTheme.Red
                        )
                    )
                )
        )

        Spacer(Modifier.width(11.dp))

        PremiumRingBadge(
            tone = RingTone.Dotted,
            modifier = Modifier.size(32.dp),
            ringSize = 17.dp,
            strokeWidth = 1.35.dp,
            dark = false
        )

        Spacer(Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ring_colors_explained_subtitle),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = RingColorsExplainedTheme.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.ring_colors_explained),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = RingColorsExplainedTheme.Muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}

@Composable
private fun LegendRow(
    tone: RingTone,
    title: String,
    body: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(tone.softBackground)
                    .border(
                        width = 1.dp,
                        color = tone.color.copy(alpha = if (tone == RingTone.Dotted) 0.26f else 0.22f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                RingStroke(
                    tone = tone,
                    size = 30.dp,
                    strokeWidth = if (tone == RingTone.Dotted) 1.6.dp else 2.3.dp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = RingColorsExplainedTheme.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = RingColorsExplainedTheme.Muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RingColorsExplainedTheme.BorderSoft)
            )
        }
    }
}

@Composable
private fun RingStroke(
    tone: RingTone,
    size: Dp,
    strokeWidth: Dp
) {
    val dashedPath = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    Canvas(modifier = Modifier.size(size)) {
        val radius = (this.size.minDimension - strokeWidth.toPx()) / 2f
        drawCircle(
            color = tone.color,
            radius = radius,
            center = Offset(this.size.width / 2f, this.size.height / 2f),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = if (tone == RingTone.Dotted) dashedPath else null
            )
        )
    }
}

private enum class RingTone(
    val color: Color,
    val softBackground: Color
) {
    Green(
        color = RingColorsExplainedTheme.Green,
        softBackground = Color(0xFFF2FBF3)
    ),
    Yellow(
        color = RingColorsExplainedTheme.Brown,
        softBackground = Color(0xFFFBF6F0)
    ),
    Red(
        color = RingColorsExplainedTheme.Red,
        softBackground = Color(0xFFFFF3F4)
    ),
    Dotted(
        color = RingColorsExplainedTheme.Dotted,
        softBackground = Color(0xFFF5F6F8)
    ),
    Future(
        color = RingColorsExplainedTheme.Future,
        softBackground = Color(0xFFF8F9FB)
    ),
    BadgeLight(
        color = Color.White,
        softBackground = Color.Transparent
    )
}
