package com.calai.bitecal.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.model.HomeRecentUploadUi
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RecentUploadCard(
    item: HomeRecentUploadUi,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = CardStyles.Border,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecentUploadThumb(
                item = item,
                modifier = Modifier
                    .width(108.dp)
                    .height(100.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                when (item) {
                    is HomeRecentUploadUi.Pending -> PendingContent(item)
                    is HomeRecentUploadUi.Success -> SuccessContent(item)
                }
            }
        }
    }
}

/**
 * 依照狀態切換縮圖樣式：
 * - Pending：外圈旋轉 loading ring
 * - Success：靜態縮圖
 */
@Composable
private fun RecentUploadThumb(
    item: HomeRecentUploadUi,
    modifier: Modifier = Modifier
) {
    when (item) {
        is HomeRecentUploadUi.Pending -> PendingRecentUploadThumb(
            previewUri = item.previewUri,
            modifier = modifier
        )

        is HomeRecentUploadUi.Success -> StaticRecentUploadThumb(
            previewUri = item.previewUri,
            modifier = modifier
        )
    }
}

/**
 * 成功狀態：一般縮圖
 */
@Composable
private fun StaticRecentUploadThumb(
    previewUri: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (!previewUri.isNullOrBlank()) {
            AsyncImage(
                model = previewUri,
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F2F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍽️",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

/**
 * Pending 狀態：縮圖外圍顯示旋轉中的 loading ring
 */
@Composable
private fun PendingRecentUploadThumb(
    previewUri: String?,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pending_thumb_ring")

    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1450,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pending_thumb_ring_angle"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(96.dp)
                .testTag("pending_ring")
        ) {
            val strokeWidth = 6.dp.toPx()
            val orbitRadius = (size.minDimension / 2f) - strokeWidth

            // 底部圓環
            drawCircle(
                color = Color(0xFFE9ECF3),
                style = Stroke(width = strokeWidth)
            )

            // 固定淡色弧段
            drawArc(
                color = Color(0xFFD7DDE8),
                startAngle = -90f,
                sweepAngle = 64f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // 旋轉小圓點
            val rad = Math.toRadians((angle - 90f).toDouble())
            val cx = center.x + orbitRadius * cos(rad).toFloat()
            val cy = center.y + orbitRadius * sin(rad).toFloat()

            drawCircle(
                color = Color(0xFFB9C1D0),
                radius = 3.5.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        if (!previewUri.isNullOrBlank()) {
            AsyncImage(
                model = previewUri,
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F2F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍽️",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

/**
 * Pending 文字區：標題 + 時間 + shimmer skeleton bars
 */
@Composable
private fun PendingContent(
    item: HomeRecentUploadUi.Pending
) {
    val shimmerTransition = rememberInfiniteTransition(label = "pending_shimmer")

    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1050,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pending_shimmer_progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.foodlog_pending_analysis),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedSkeletonBar(
            widthFraction = 0.60f,
            progress = shimmerProgress,
            modifier = Modifier.testTag("pending_skeleton_1")
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedSkeletonBar(
            widthFraction = 0.42f,
            progress = shimmerProgress,
            modifier = Modifier.testTag("pending_skeleton_2")
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedSkeletonBar(
            widthFraction = 0.48f,
            progress = shimmerProgress,
            modifier = Modifier.testTag("pending_skeleton_3")
        )
    }
}

/**
 * Success 狀態：顯示分析結果
 */
@Composable
private fun SuccessContent(
    item: HomeRecentUploadUi.Success
) {
    val titleText = item.title.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.foodlog_analysis_done)

    val kcalText = stringResource(
        R.string.foodlog_nutrition_kcal,
        item.kcal
    )
    val proteinText = stringResource(
        R.string.foodlog_nutrition_protein_g,
        item.proteinG
    )
    val carbsText = stringResource(
        R.string.foodlog_nutrition_carbs_g,
        item.carbsG
    )
    val fatText = stringResource(
        R.string.foodlog_nutrition_fat_g,
        item.fatG
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = kcalText,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = proteinText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = carbsText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = fatText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Shimmer skeleton bar
 * progress 建議範圍：-1f ~ 2f
 */
@Composable
private fun AnimatedSkeletonBar(
    widthFraction: Float,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(0xFFE6E9F0)
    val highlightColor = Color(0xFFF2F4F8)

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(8.dp)
            .clip(RoundedCornerShape(6.dp))
            .drawWithCache {
                val startX = size.width * (progress - 1f)
                val endX = size.width * progress

                val brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        highlightColor,
                        baseColor
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(endX, size.height)
                )

                onDrawBehind {
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
    )
}
