package com.calai.bitecal.ui.home.ui.foodlog

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.model.HomeRecentUploadUi
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardHeight = 124.dp
private val CardCorner = 22.dp
private val ThumbSize = 100.dp
private val ThumbCorner = 18.dp
private val ContentStartGap = 14.dp
private val ContentEndPadding = 12.dp
private val DeleteActionColor = Color(0xFFE46A6A)
private val DeleteActionCorner = RoundedCornerShape(CardCorner)

private val TitleColor = Color(0xFF111827)
private val SecondaryTextColor = Color(0xFF667085)
private val TimeColor = Color(0xFF647084)
private val TimeChipBg = Color(0xFFF3F5F7)
private val KcalColor = Color(0xFF0F172A)
private val MacroColor = Color(0xFF344054)
private val SkeletonBase = Color(0xFFD7D7E0)
private val SkeletonHighlight = Color(0xFFECECF3)
private val ThumbPlaceholder = Color(0xFFF2F3F6)

private val TitleTextStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Bold,
    color = TitleColor
)

private val TimeTextStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.SemiBold,
    color = TimeColor
)

private val KcalTextStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 19.sp,
    fontWeight = FontWeight.SemiBold,
    color = KcalColor
)

private val MacroTextStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.SemiBold,
    color = MacroColor
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun RecentUploadCard(
    item: HomeRecentUploadUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val scope = rememberCoroutineScope()

        val actionWidth = maxWidth * 0.25f
        val density = LocalDensity.current
        val actionWidthPx = with(density) { actionWidth.toPx() }
        val openThresholdPx = actionWidthPx * 0.42f
        val flingThresholdPx = with(density) { 380.dp.toPx() }

        val offsetX = remember(item.foodLogId) { Animatable(0f) }
        val isOpened = offsetX.value < -1f

        val dragState = rememberDraggableState { delta ->
            scope.launch {
                val next = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                offsetX.snapTo(next)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight)
                .clip(DeleteActionCorner)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DeleteActionColor)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
                    .fillMaxHeight()
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = rememberClickWithHaptic {
                        scope.launch {
                            offsetX.snapTo(0f)
                        }
                        onDeleteClick()
                    },
                    enabled = isOpened,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("recent_upload_delete_button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash),
                        contentDescription = "Delete recent upload icon",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            RecentUploadCardContent(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = offsetX.value
                    }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            scope.launch {
                                offsetX.stop()
                            }
                        },
                        onDragStopped = { velocity ->
                            val target = when {
                                velocity <= -flingThresholdPx -> -actionWidthPx
                                velocity >= flingThresholdPx -> 0f
                                offsetX.value <= -openThresholdPx -> -actionWidthPx
                                else -> 0f
                            }

                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = 180,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    )
                    .biteCalClickable {
                        if (isOpened) {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 160,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        } else {
                            onClick()
                        }
                    }
            )
        }
    }
}

@Composable
private fun RecentUploadCardContent(
    item: HomeRecentUploadUi,
    modifier: Modifier = Modifier
) {
    val isLoadingLike = item is HomeRecentUploadUi.Pending || item is HomeRecentUploadUi.Delayed

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CardCorner),
        border = CardStyles.Border,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight)
                .padding(
                    start = 12.dp,
                    top = 12.dp,
                    end = ContentEndPadding,
                    bottom = 12.dp
                )
                .alpha(if (isLoadingLike) 0.99f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is HomeRecentUploadUi.Pending -> {
                    LoadingThumb(
                        previewUri = item.previewUri,
                        modifier = Modifier.size(ThumbSize)
                    )
                }

                is HomeRecentUploadUi.Delayed -> {
                    LoadingThumb(
                        previewUri = item.previewUri,
                        modifier = Modifier.size(ThumbSize)
                    )
                }

                is HomeRecentUploadUi.Success -> {
                    SuccessThumb(
                        previewUri = item.previewUri,
                        modifier = Modifier.size(ThumbSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(ContentStartGap))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                when (item) {
                    is HomeRecentUploadUi.Pending -> PendingContent(
                        title = stringResource(R.string.foodlog_pending_analysis)
                    )

                    is HomeRecentUploadUi.Delayed -> PendingContent(
                        title = item.title,
                        subtitle = item.subtitle
                    )

                    is HomeRecentUploadUi.Success -> SuccessContent(item)
                }
            }
        }
    }
}

@Composable
private fun LoadingThumb(
    previewUri: String?,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "recent_upload_loading_ring")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "recent_upload_loading_ring_angle"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ThumbImage(
            previewUri = previewUri,
            modifier = Modifier.matchParentSize()
        )

        Canvas(
            modifier = Modifier
                .size(36.dp)
                .testTag("recent_upload_loading_ring")
        ) {
            val strokeWidth = 7.dp.toPx()

            drawArc(
                color = Color.White.copy(alpha = 0.95f),
                startAngle = angle - 135f,
                sweepAngle = 285f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun SuccessThumb(
    previewUri: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ThumbImage(
            previewUri = previewUri,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun ThumbImage(
    previewUri: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ThumbCorner))
            .background(ThumbPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        if (!previewUri.isNullOrBlank()) {
            AsyncImage(
                model = previewUri,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "☕",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun PendingContent(
    title: String,
    subtitle: String? = null
) {
    val transition = rememberInfiniteTransition(label = "pending_content_shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pending_content_shimmer_progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TitleColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("recent_upload_pending_title")
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SecondaryTextColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("recent_upload_pending_subtitle")
            )

            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        AnimatedSkeletonBar(
            widthFraction = 0.71f,
            heightDp = 7.dp,
            progress = progress,
            modifier = Modifier.testTag("recent_upload_skeleton_1")
        )

        Spacer(modifier = Modifier.height(11.dp))

        AnimatedSkeletonBar(
            widthFraction = 0.41f,
            heightDp = 7.dp,
            progress = progress,
            modifier = Modifier.testTag("recent_upload_skeleton_2")
        )

        Spacer(modifier = Modifier.height(11.dp))

        AnimatedSkeletonBar(
            widthFraction = 0.56f,
            heightDp = 7.dp,
            progress = progress,
            modifier = Modifier.testTag("recent_upload_skeleton_3")
        )
    }
}

@Composable
private fun SuccessContent(
    item: HomeRecentUploadUi.Success
) {
    val displayTitle = item.title.ifBlank { stringResource(R.string.foodlog_analysis_done) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTitle,
                style = TitleTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            RecentUploadTimeChip(
                timeText = item.timeText
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = stringResource(R.string.recent_upload_kcal_text, item.kcal),
            style = KcalTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .offset(x = (-2).dp)
                .testTag("recent_upload_kcal")
        )

        Spacer(modifier = Modifier.height(13.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacroText(
                text = stringResource(R.string.recent_upload_protein_text, item.proteinG)
            )
            MacroText(
                text = stringResource(R.string.recent_upload_carbs_text, item.carbsG)
            )
            MacroText(
                text = stringResource(R.string.recent_upload_fat_text, item.fatG)
            )
        }
    }
}

private fun formatDisplayTime(raw: String): String {
    val input = raw.trim()
    if (input.isBlank()) return "--:--"

    val outputFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    val candidates = listOf(
        DateTimeFormatter.ofPattern("H:mm", Locale.US),
        DateTimeFormatter.ofPattern("HH:mm", Locale.US),
        DateTimeFormatter.ofPattern("h:mm a", Locale.US),
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    )

    for (formatter in candidates) {
        runCatching {
            return LocalTime.parse(input.uppercase(Locale.US), formatter).format(outputFormatter)
        }
    }

    return input
}

@Composable
private fun RecentUploadTimeChip(
    timeText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TimeChipBg)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatDisplayTime(timeText),
            style = TimeTextStyle,
            maxLines = 1
        )
    }
}

@Composable
private fun MacroText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MacroTextStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun AnimatedSkeletonBar(
    widthFraction: Float,
    heightDp: Dp,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(heightDp)
            .clip(RoundedCornerShape(8.dp))
            .drawWithCache {
                val startX = size.width * (progress - 1f)
                val endX = size.width * progress

                val brush = Brush.linearGradient(
                    colors = listOf(
                        SkeletonBase,
                        SkeletonHighlight,
                        SkeletonBase
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(endX, size.height)
                )

                onDrawBehind {
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
    )
}
