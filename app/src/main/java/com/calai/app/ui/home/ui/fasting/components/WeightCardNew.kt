package com.calai.app.ui.home.ui.fasting.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.calai.app.ui.home.components.GaugeRing
import com.calai.app.ui.home.components.RingColors

@Composable
fun WeightCardNew(
    primary: String,                        // 例：+0.8 kg / +2 lbs
    secondary: String? = "to goal",
    ringColor: Color = Color(0xFF06B6D4),
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    cardHeight: Dp,
    // 右側圓環尺寸
    ringSize: Dp = 74.dp,
    ringStroke: Dp = 6.dp,
    centerDisk: Dp = 32.dp,
    // 左下的加號鈕（如果要）
    plusButtonSize: Dp = 24.dp,
    plusIconSize: Dp = 19.dp,
    // 標題頂欄（與 FastingPlan 一致）
    topBarTitle: String = "Weight",
    topBarHeight: Dp = TopBarDefaults.Height,
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,

    // ★ primary 可調
    primaryTextStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    primaryFontSize: TextUnit? = null,      // 同時給時，以這個優先生效
    primaryYOffset: Dp = 0.dp,              // 負值 = 往上
    primaryTopSpacing: Dp = 10.dp,          // 與頂欄距離

    // ★ secondary 可調（新增）
    secondaryTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    secondaryFontSize: TextUnit? = null,
    secondaryYOffset: Dp = 0.dp,            // 負值 = 往上
    gapPrimaryToSecondary: Dp = 2.dp        // 主次文字間距
) {
    TopBarCard(
        title = topBarTitle,
        topBarHeight = topBarHeight,
        topBarTextStyle = topBarTextStyle,
        showWhiteTriangle = true,                // 白色三角形
        modifier = modifier.height(cardHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左欄：主/副文字 + 加號
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(primaryTopSpacing))

                // Primary
                Text(
                    text = primary,
                    style = primaryTextStyle,
                    fontSize = primaryFontSize ?: primaryTextStyle.fontSize,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = primaryYOffset)
                )

                // 與 secondary 的間距
                Spacer(Modifier.height(gapPrimaryToSecondary))

                // Secondary（可上移）
                if (!secondary.isNullOrBlank()) {
                    Text(
                        text = secondary,
                        style = secondaryTextStyle,
                        fontSize = secondaryFontSize ?: secondaryTextStyle.fontSize,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = secondaryYOffset)
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                }

                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.requiredSize(plusButtonSize),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.Black
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.requiredSize(plusIconSize)
                        )
                    }
                }
            }

            // 右欄：圓環
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                    GaugeRing(
                        progress = progress,
                        sizeDp = ringSize,
                        strokeDp = ringStroke,
                        trackColor = Color(0xFFE8EAEE),
                        progressColor = ringColor,
                        drawTopTick = true,
                        tickColor = ringColor
                    )
                    Surface(
                        color = RingColors.CenterFill, // ★ 更淺
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(centerDisk)
                    ) {}
                }
            }
        }
    }
}
