package com.calai.bitecal.ui.home.ui.fasting.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.calai.bitecal.ui.home.components.GaugeRing
import com.calai.bitecal.ui.home.components.RingColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.calai.bitecal.R

/**
 * WeightCardNew v2
 *
 * 改動：
 * 1. 左半用 Box 疊層，保證按鈕不會被 Column 擠壓變形
 * 2. 新增 onAddWeightClick callback
 * 3. 加入 WeightAddButton()（黑色圓+灰閃光，和 Workout/Water 一致）
 */
@Composable
fun WeightCardNew(
    modifier: Modifier = Modifier,
    primary: String,
    secondary: String? = "to goal",
    ringColor: Color = Color(0xFF06B6D4),
    progress: Float = 0f,
    cardHeight: Dp,
    ringSize: Dp = 74.dp,
    ringStroke: Dp = 6.dp,
    centerDisk: Dp = 40.dp,

    // ✅ 新增：中心 icon（你要放 bullseye 就用預設）
    centerIconRes: Int = R.drawable.flag,
    centerIconDescription: String = "weight target",
    centerIconTint: Color? = null,                    // Vector 想統一色可給 Color(0xFF111114)

    topBarTitle: String = "Weight",
    topBarHeight: Dp = TopBarDefaults.Height,
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    primaryTextStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    primaryFontSize: TextUnit? = null,
    primaryYOffset: Dp = 0.dp,
    primaryTopSpacing: Dp = 10.dp,
    secondaryTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    secondaryFontSize: TextUnit? = null,
    secondaryYOffset: Dp = 0.dp,
    gapPrimaryToSecondary: Dp = 2.dp,
    onAddWeightClick: () -> Unit = {}
) {
    TopBarCard(
        title = topBarTitle,
        topBarHeight = topBarHeight,
        topBarTextStyle = topBarTextStyle,
        showWhiteTriangle = true,
        modifier = modifier.height(cardHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ========= 左半：文字 + 固定左下 + =========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(Modifier.height(primaryTopSpacing))

                    Text(
                        text = primary,
                        style = primaryTextStyle,
                        fontSize = primaryFontSize ?: primaryTextStyle.fontSize,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(x = (-4).dp, y = primaryYOffset)
                    )

                    Spacer(Modifier.height(gapPrimaryToSecondary))

                    if (!secondary.isNullOrBlank()) {
                        Text(
                            text = secondary,
                            style = secondaryTextStyle,
                            fontSize = secondaryFontSize ?: secondaryTextStyle.fontSize,
                            color = Color(0xFF111114),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.offset(y = secondaryYOffset)
                        )
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }
                }

                WeightAddButton(
                    onClick = onAddWeightClick,
                    outerSizeDp = 34.dp,
                    innerSizeDp = 26.dp,
                    iconSizeDp = 21.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-5).dp, y = (1).dp)
                )
            }

            // ========= 右半：圓環 =========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
                    GaugeRing(
                        progress = progress,
                        sizeDp = ringSize,
                        strokeDp = ringStroke,
                        trackColor = Color(0xFFEFF0F3),
                        progressColor = ringColor,
                        drawTopTick = true,
                        tickColor = ringColor
                    )

                    // ✅ 中心灰圓底 + bullseye
                    Surface(
                        color = RingColors.CenterFill,
                        shape = CircleShape,
                        modifier = Modifier.size(centerDisk)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = centerIconRes),
                                contentDescription = centerIconDescription,
                                modifier = Modifier
                                    .size(21.dp)
                                    .padding(0.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = centerIconTint?.let { ColorFilter.tint(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outerSizeDp: Dp = 36.dp,
    innerSizeDp: Dp = 30.dp,
    iconSizeDp: Dp = 24.dp
) {
    val scope = rememberCoroutineScope()
    var flashAlphaGoal by remember { mutableFloatStateOf(0f) }

    val animatedAlpha by animateFloatAsState(
        targetValue = flashAlphaGoal,
        label = "weightAddFlash"
    )

    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(outerSizeDp)
            .clickable(interactionSource = noRipple, indication = null) {
                scope.launch {
                    flashAlphaGoal = 0.4f
                    delay(120)
                    flashAlphaGoal = 0f
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (animatedAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = animatedAlpha * 0.4f),
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(innerSizeDp)
                .background(Color(0xFF111114), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "add weight entry",
                tint = Color.White,
                modifier = Modifier.size(iconSizeDp)
            )
        }
    }
}
