package com.calai.bitecal.ui.home.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.calai.bitecal.ui.home.ui.components.ProfileEditTopBar

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
                ProfileEditTopBar(
                    title = stringResource(R.string.ring_colors_explained_title),
                    onBack = onBackDebounced
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { inner ->
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .verticalScroll(scroll)
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 28.dp)
                        .navigationBarsPadding()
                ) {
                    RingCalendarPreviewCard()

                    Spacer(Modifier.height(26.dp))

                    Text(
                        text = stringResource(R.string.ring_colors_explained_intro),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 25.sp,
                            lineHeight = 33.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(28.dp))

                    RingLegendItem(
                        ringColor = RingTone.Green,
                        title = stringResource(R.string.ring_colors_green_title),
                        body = stringResource(R.string.ring_colors_green_body)
                    )
                    RingLegendItem(
                        ringColor = RingTone.Yellow,
                        title = stringResource(R.string.ring_colors_yellow_title),
                        body = stringResource(R.string.ring_colors_yellow_body)
                    )
                    RingLegendItem(
                        ringColor = RingTone.Red,
                        title = stringResource(R.string.ring_colors_red_title),
                        body = stringResource(R.string.ring_colors_red_body)
                    )
                    RingLegendItem(
                        ringColor = RingTone.Dotted,
                        title = stringResource(R.string.ring_colors_dotted_title),
                        body = stringResource(R.string.ring_colors_dotted_body)
                    )
                }
            }
        }
    }
}

@Composable
private fun RingCalendarPreviewCard() {
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val mutedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        )
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(cardGradient)
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ring_colors_preview_brand),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, outline)
                ) {
                    Text(
                        text = stringResource(R.string.ring_colors_preview_kcal_value),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = labelColor,
                        modifier = Modifier.padding(horizontal = 19.dp, vertical = 9.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_sun),
                    day = stringResource(R.string.ring_colors_preview_day_10),
                    tone = RingTone.Green,
                    labelColor = labelColor
                )
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_mon),
                    day = stringResource(R.string.ring_colors_preview_day_11),
                    tone = RingTone.Red,
                    labelColor = labelColor
                )
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_tue),
                    day = stringResource(R.string.ring_colors_preview_day_12),
                    tone = RingTone.Dotted,
                    labelColor = labelColor
                )
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_wed),
                    day = stringResource(R.string.ring_colors_preview_day_13),
                    tone = RingTone.Green,
                    labelColor = labelColor
                )
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_thu),
                    day = stringResource(R.string.ring_colors_preview_day_14),
                    tone = RingTone.Yellow,
                    labelColor = labelColor
                )
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_fri),
                    day = stringResource(R.string.ring_colors_preview_day_15),
                    tone = RingTone.Neutral,
                    labelColor = mutedLabelColor
                )
                PreviewDay(
                    weekday = stringResource(R.string.progress_day_sat),
                    day = stringResource(R.string.ring_colors_preview_day_16),
                    tone = RingTone.Neutral,
                    labelColor = mutedLabelColor
                )
            }
        }
    }
}

@Composable
private fun PreviewDay(
    weekday: String,
    day: String,
    tone: RingTone,
    labelColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(39.dp)
    ) {
        Text(
            text = weekday,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                strokeWidth = 3.dp
            )
            Text(
                text = day,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun RingLegendItem(
    ringColor: RingTone,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RingStroke(
            tone = ringColor,
            size = 72.dp,
            strokeWidth = 3.dp
        )
        Spacer(Modifier.width(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val dashedPath = remember { PathEffect.dashPathEffect(floatArrayOf(9f, 8f), 0f) }
    Canvas(modifier = Modifier.size(size)) {
        val ringColor = tone.color
        val radius = (this.size.minDimension - strokeWidth.toPx()) / 2f
        drawCircle(
            color = ringColor,
            radius = radius,
            center = Offset(this.size.width / 2f, this.size.height / 2f),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = if (tone == RingTone.Dotted) dashedPath else null
            )
        )
    }
}

private enum class RingTone(val color: Color) {
    Green(Color(0xFF77DC7A)),
    Yellow(Color(0xFFD99A62)),
    Red(Color(0xFFE16B70)),
    Dotted(Color(0xFF6E7178)),
    Neutral(Color(0xFF85858D))
}
