package com.calai.bitecal.ui.home.ui.fasting.components

// ===== Imports =====
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.components.HomeCardStyles
import com.calai.bitecal.ui.home.components.GreenSwitch
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

@Composable
fun FastingPlanCard(
    planName: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    cardHeight: Dp,
    modifier: Modifier = Modifier,

    planTitle: String = "Fasting Plan",
    startLabel: String = "Start time",
    startText: String? = null,
    endLabel: String = "End time",
    endText: String? = null,

    planNameTextStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold
    ),
    planNameFontSize: TextUnit? = null,
    planNameYOffset: Dp = 0.dp,
    planNameXOffset: Dp = 0.dp,
    switchWidth: Dp = 56.dp,
    switchHeight: Dp = 30.dp,

    topBarHeight: Dp = HomeCardStyles.TopBar.Height,
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    topBarPaddingH: Dp = HomeCardStyles.TopBar.HorizontalPadding,

    leftColumnWeight: Float = 1f,
    rightColumnWeight: Float = 0.8f
) {
    val hapticClick = rememberClickWithHaptic(onClick = onClick)

    Card(
        modifier = modifier
            .height(cardHeight)
            // ★ 統一陰影＋圓角 → 跟其他卡片同一套視覺（立體浮起來）
            .shadow(
                CardStyles.Elevation,
                CardStyles.Corner,
                clip = false
            ),
        shape = CardStyles.Corner, // 20.dp 圓角，和其他卡片一致
        colors = CardDefaults.cardColors(
            containerColor = CardStyles.Bg // 微暖白
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardStyles.Border,
        onClick = hapticClick
    ){
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== 上方：黑底白字標題條（固定高度，垂直置中） =====
            Surface(
                color = HomeCardStyles.TopBar.Container,
                contentColor = HomeCardStyles.TopBar.Content,
                shape = HomeCardStyles.TopBar.Shape,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight), // ★ 用高度決定厚度
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = planTitle,
                        style = topBarTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = topBarPaddingH)
                    )
                }
            }

            // ===== 內容區：左右分欄（沿用你現在的配置） =====
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左欄：計畫名稱（置中，可 Y 偏移）＋ Switch（置中）
                Column(
                    modifier = Modifier
                        .weight(leftColumnWeight)
                        .fillMaxHeight()
                        .offset(y = (-4).dp),
                    verticalArrangement = Arrangement.spacedBy(
                        space = 15.dp,
                        alignment = Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = planName,
                        style = planNameTextStyle,
                        fontSize = planNameFontSize ?: planNameTextStyle.fontSize,
                        color = HomeCardStyles.Text.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(
                                x = planNameXOffset,
                                y = planNameYOffset
                            )
                    )
                    GreenSwitch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        width = switchWidth,
                        height = switchHeight
                    )
                }

                // 右欄：開始/結束（置中 + 字體較大）
                Column(
                    modifier = Modifier
                        .weight(rightColumnWeight)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = startLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = HomeCardStyles.Text.Label,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = startText ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HomeCardStyles.Text.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = endLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = HomeCardStyles.Text.Label,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = endText ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HomeCardStyles.Text.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
