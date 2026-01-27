package com.calai.bitecal.ui.home.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R

@Immutable
data class RecentlyUploadedEmptyStyle(
    val outerContainerColor: Color,
    val outerBorderColor: Color,
    val innerPillColor: Color,
    val innerPillBorderColor: Color,
    val iconBgColor: Color,
    val skeletonColor: Color,
    val hintColor: Color
) {
    companion object {
        val Default = RecentlyUploadedEmptyStyle(
            outerContainerColor = Color(0xFFF6F7FB),
            outerBorderColor = CardStyles.BorderColor,
            innerPillColor = CardStyles.Bg,
            innerPillBorderColor = Color(0xFFE6E8EE),
            iconBgColor = Color(0xFFF0F2F6),
            skeletonColor = Color(0xFFE3E6EC),
            hintColor = Color(0xFF6B7280)
        )
    }
}

@Composable
fun RecentlyUploadedEmptySection(
    modifier: Modifier = Modifier,
    thumbnailPainter: Painter? = null,
    cardHeight: Dp = 100.dp,
    style: RecentlyUploadedEmptyStyle = RecentlyUploadedEmptyStyle.Default,
    titleStartPadding: Dp = 14.dp,
    titleBottomPadding: Dp = 10.dp
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.recently_uploaded),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = titleStartPadding, bottom = titleBottomPadding)
        )
        RecentlyUploadedEmptyCard(
            thumbnailPainter = thumbnailPainter,
            cardHeight = cardHeight,
            style = style,
            modifier = Modifier.testTag("recent_empty_card")
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RecentlyUploadedEmptyCard(
    modifier: Modifier = Modifier,
    thumbnailPainter: Painter? = null,
    cardHeight: Dp = 100.dp,
    style: RecentlyUploadedEmptyStyle = RecentlyUploadedEmptyStyle.Default,

    contentPaddingV: Dp = 16.dp,
    pillHeight: Dp = 80.dp,
    pillWidthFraction: Float = 0.82f,
    outerCorner: Dp = 20.dp,
    pillCorner: Dp = 18.dp,

    // 邊框粗度
    outerBorderWidth: Dp = 0.9.dp,
    pillBorderWidth: Dp = 0.85.dp
) {
    val outerShape = RoundedCornerShape(outerCorner)
    val pillShape = RoundedCornerShape(pillCorner)

    // 外層：無陰影（移除 .shadow），elevation=0
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = cardHeight),
        shape = outerShape,
        colors = CardDefaults.cardColors(containerColor = style.outerContainerColor),
        border = BorderStroke(outerBorderWidth, SolidColor(style.outerBorderColor)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = contentPaddingV),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 內層 pill：無陰影（移除 .shadow），elevation=0
            Card(
                modifier = Modifier
                    .fillMaxWidth(pillWidthFraction)
                    .height(pillHeight),
                shape = pillShape,
                colors = CardDefaults.cardColors(containerColor = style.innerPillColor),
                border = BorderStroke(pillBorderWidth, SolidColor(style.innerPillBorderColor)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val iconSize = 48.dp
                    val gap = 12.dp

                    val safe = 28.dp
                    val available = maxWidth - iconSize - gap - safe
                    val barsBlockWidth = remember(maxWidth) {
                        available.coerceIn(160.dp, 240.dp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .background(style.iconBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumbnailPainter != null) {
                                Image(
                                    painter = thumbnailPainter,
                                    contentDescription = "recent_meal_placeholder",
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Text(text = "\uD83E\uDD57", fontSize = 28.sp) // 🥗
                            }
                        }

                        Spacer(Modifier.width(gap))

                        Column(
                            modifier = Modifier.width(barsBlockWidth),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .testTag("recent_skeleton_1"),
                                color = style.skeletonColor
                            )
                            Spacer(Modifier.height(10.dp))
                            SkeletonLine(
                                modifier = Modifier
                                    .fillMaxWidth(0.58f)
                                    .testTag("recent_skeleton_2"),
                                color = style.skeletonColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.recently_uploaded_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = style.hintColor
            )
        }
    }
}

@Composable
private fun SkeletonLine(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}
