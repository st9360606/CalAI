package com.calai.app.ui.home.components

// ===== Imports =====
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun FastingPlanCard(
    planTitle: String = "Fasting Plan",        // TODO: stringResource
    planName: String,
    startLabel: String = "start time",         // TODO: stringResource
    startText: String? = null,
    endLabel: String = "end time",             // TODO: stringResource
    endText: String? = null,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    cardHeight: Dp,
    modifier: Modifier = Modifier,

    // ===== 計畫名稱調整（保留你已有的能力） =====
    planNameTextStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    planNameFontSize: TextUnit? = null,
    planNameYOffset: Dp = 0.dp,

    // ===== Switch 尺寸（保留） =====
    switchWidth: Dp = 55.dp,
    switchHeight: Dp = 26.dp,

    // ===== 黑底標題條參數（新） =====
    topBarHeight: Dp = 26.dp,                                       // ★ 固定高度，視覺更薄
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,// 可換 labelMedium 讓更薄
    topBarPaddingH: Dp = 16.dp                                      // 左右內距
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardStyles.Border,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== 上方：黑底白字標題條（固定高度，垂直置中） =====
            Surface(
                color = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左欄：計畫名稱（置中，可 Y 偏移）＋ Switch（置中）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = planName,
                        style = planNameTextStyle,
                        fontSize = planNameFontSize ?: planNameTextStyle.fontSize,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = planNameYOffset) // 可為負值，往上提
                    )
                    Spacer(Modifier.height(8.dp))
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
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = startLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = startText ?: "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = endLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = endText ?: "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF0F172A),
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
