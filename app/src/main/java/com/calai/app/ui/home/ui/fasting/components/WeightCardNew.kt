package com.calai.app.ui.home.ui.fasting.components

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
import com.calai.app.ui.home.components.GaugeRing
import com.calai.app.ui.home.components.RingColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    primary: String,                        // 例：+0.8 kg / +2 lbs
    secondary: String? = "to goal",
    ringColor: Color = Color(0xFF06B6D4),
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    cardHeight: Dp,
    // 右側圓環尺寸
    ringSize: Dp = 74.dp,
    ringStroke: Dp = 6.dp,
    centerDisk: Dp = 36.dp,

    // 標題頂欄（與 FastingPlan 一致）
    topBarTitle: String = "Weight",
    topBarHeight: Dp = TopBarDefaults.Height,
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,

    // ★ primary 可調
    primaryTextStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    primaryFontSize: TextUnit? = null,      // 同時給時，以這個優先生效
    primaryYOffset: Dp = 0.dp,              // 負值 = 往上
    primaryTopSpacing: Dp = 10.dp,          // 與頂欄距離
    // ★ secondary 可調
    secondaryTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    secondaryFontSize: TextUnit? = null,
    secondaryYOffset: Dp = 0.dp,            // 負值 = 往上
    gapPrimaryToSecondary: Dp = 2.dp,       // 主次文字間距

    // ★ 新增：點擊加號做什麼（例如打開修改體重/目標的畫面）
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
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ========= 左半：Box 疊層，文字在上，+ 按鈕固定左下 =========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 文字群組（貼上方）
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(Modifier.height(primaryTopSpacing))

                    // Primary (+0.8 kg / +2 lbs)
                    Text(
                        text = primary,
                        style = primaryTextStyle,
                        fontSize = primaryFontSize ?: primaryTextStyle.fontSize,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = primaryYOffset)
                    )

                    Spacer(Modifier.height(gapPrimaryToSecondary))

                    // Secondary ("to goal")
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

                // 加號按鈕：固定貼在左下角，尺寸不被壓縮
                WeightAddButton(
                    onClick = onAddWeightClick,
                    outerSizeDp = 36.dp,  // 觸控區 & 灰閃圈 (和 Water 卡一致)
                    innerSizeDp = 28.dp, // 黑底圓按鈕大小 (和 Water 卡一致)
                    iconSizeDp = 24.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
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
                    Surface(
                        color = RingColors.CenterFill,
                        shape = CircleShape,
                        modifier = Modifier.size(centerDisk)
                    ) {}
                }
            }
        }
    }
}

/**
 * WeightAddButton
 *
 * - 外圈 50dp：實際可點擊區 / 灰色閃光範圍（和 Workout/Water 的 + 一致）
 * - 內圈 38dp：黑色圓底 + 白色「+」
 * - 點擊時會閃一個半透明深灰圓，120ms 後自動淡掉
 *
 * 注意：這裡直接複製互動邏輯，讓 Weight 卡不用去 import 其他 package。
 */
@Composable
private fun WeightAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outerSizeDp: Dp = 36.dp,   // 和 Water/Workout 一致
    innerSizeDp: Dp = 30.dp,    // 黑色圓的實際直徑
    iconSizeDp: Dp = 24.dp    // ✅ 白色「＋」加大

) {
    val scope = rememberCoroutineScope()

    // 0f ~ 0.4f 控制閃光強度
    var flashAlphaTarget by remember { mutableFloatStateOf(0f) }

    // 用動畫淡出閃光
    val animatedAlpha by animateFloatAsState(
        targetValue = flashAlphaTarget,
        label = "weightAddFlash"
    )

    // 不用 ripple，用我們自己的灰圓閃光
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(outerSizeDp)
            .clickable(
                interactionSource = noRipple,
                indication = null
            ) {
                scope.launch {
                    flashAlphaTarget = 0.4f
                    delay(120)
                    flashAlphaTarget = 0f
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 灰色閃光圈（比黑按鈕大）
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

        // 黑色圓按鈕本體
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
