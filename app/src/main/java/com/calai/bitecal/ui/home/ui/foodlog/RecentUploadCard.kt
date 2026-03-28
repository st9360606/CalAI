package com.calai.bitecal.ui.home.ui.foodlog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.model.HomeRecentUploadUi
private val TitleColor = Color(0xFF111111)
private val SecondaryTextColor = Color(0xFF111111)
private val TimeColor = Color(0xFF111111)
private val SkeletonBase = Color(0xFFD7D7E0)
private val SkeletonHighlight = Color(0xFFECECF3)
private val ThumbPlaceholder = Color(0xFFF2F3F6)

@Composable
fun RecentUploadCard(
    item: HomeRecentUploadUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isLoadingLike = item is HomeRecentUploadUi.Pending || item is HomeRecentUploadUi.Delayed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("recent_upload_card"),
        shape = RoundedCornerShape(22.dp),
        border = CardStyles.Border,
        colors = CardDefaults.cardColors(
            containerColor = CardStyles.Bg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(end = 12.dp)
                .alpha(if (isLoadingLike) 0.99f else 1f),
            verticalAlignment = Alignment.CenterVertically
        )  {
            when (item) {
                is HomeRecentUploadUi.Pending -> {
                    LoadingThumb(
                        previewUri = item.previewUri,
                        modifier = Modifier
                            .width(118.dp)
                            .fillMaxHeight()
                    )
                }

                is HomeRecentUploadUi.Delayed -> {
                    LoadingThumb(
                        previewUri = item.previewUri,
                        modifier = Modifier
                            .width(118.dp)
                            .fillMaxHeight()
                    )
                }

                is HomeRecentUploadUi.Success -> {
                    SuccessThumb(
                        previewUri = item.previewUri,
                        modifier = Modifier
                            .width(118.dp)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(118.dp),
                verticalArrangement = Arrangement.Center
            ) {
                when (item) {
                    is HomeRecentUploadUi.Pending -> PendingContent(
                        title = "正在分析食物..."
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
            dimmed = false,
            modifier = Modifier.matchParentSize()
        )

        Canvas(
            modifier = Modifier
                .size(38.dp)
                .testTag("recent_upload_loading_ring")
        ) {
            val strokeWidth = 8.dp.toPx()

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
            dimmed = false,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun ThumbImage(
    previewUri: String?,
    dimmed: Boolean,
    modifier: Modifier = Modifier
)  {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = 26.dp,
                    bottomStart = 26.dp,
                    topEnd = 18.dp,
                    bottomEnd = 18.dp
                )
            )
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

        if (dimmed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.22f))
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
    val displayTitle = item.title.ifBlank { "Food Analysis" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TitleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.timeText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TimeColor
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "🔥 ${item.kcal} 卡路里",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TitleColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("recent_upload_kcal")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacroText("🥩 ${item.proteinG}g")
            MacroText("🌾 ${item.carbsG}g")
            MacroText("🥑 ${item.fatG}g")
        }
    }
}

@Composable
private fun MacroText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = SecondaryTextColor,
            fontWeight = FontWeight.SemiBold
        ),
        maxLines = 1,
        overflow = TextOverflow.Clip
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
