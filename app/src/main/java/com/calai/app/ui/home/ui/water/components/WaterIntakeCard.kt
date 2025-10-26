package com.calai.app.ui.home.ui.water.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.R
import com.calai.app.data.water.store.WaterUnit
import com.calai.app.ui.home.components.CardStyles
import com.calai.app.ui.home.ui.water.model.WaterUiState

/**
 * 喝水卡片
 * - cardHeight: 直接沿用 TwoPagePager 傳下來的高度，維持版面
 * - state: 從 WaterViewModel.ui 取得
 * - onPlus/onMinus: 呼叫 vm.adjust(+1 / -1)
 * - onSettings: 呼叫 vm.toggleUnit() 或打開設定頁
 */
@Composable
fun WaterIntakeCard(
    cardHeight: Dp,
    state: WaterUiState,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(cardHeight)
            .shadow(
                CardStyles.Elevation,
                CardStyles.Corner,
                clip = false
            ),
        shape = CardStyles.Corner,
        border = CardStyles.Border,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ===== Left: 小 icon + 文字區 =====
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 左側小水杯方塊
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = Color(0xFFF2F3FF), // 淡淡的淺紫/灰底
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 你可以放自己的向量圖 R.drawable.ic_water_glass
                    Icon(
                        painter = painterResource(R.drawable.ic_focus_spoon_foreground),
                        contentDescription = "water",
                        tint = Color(0xFF4D6EEB) // 杯內水的藍色
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Water",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold
                    )

                    // 顯示數值：依照目前單位決定主字串
                    val mainText = when (state.unit) {
                        WaterUnit.ML -> "${state.ml} ml"
                        WaterUnit.OZ -> "${state.flOz} fl oz"
                    }
                    val cupsText = "(${state.cups} cups)"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$mainText $cupsText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(8.dp))

                        // 齒輪：切單位
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "settings",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSettings() }
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // ===== Right: 減號 / 加號 =====
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 減號：白底 + 黑框
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, CircleShape)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF111114),
                            shape = CircleShape
                        )
                        .clickable { onMinus() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "minus",
                        tint = Color(0xFF111114)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // 加號：黑底 + 白色 "+"
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF111114), CircleShape)
                        .clickable { onPlus() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "plus",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
