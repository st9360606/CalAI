package com.calai.app.ui.home.ui.water.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.R
import com.calai.app.data.water.store.WaterUnit
import com.calai.app.ui.home.components.CardStyles
import com.calai.app.ui.home.ui.water.model.WaterUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RoundActionButton v15
 *
 * 改動 vs v14：
 * 1. 按下去的高亮圈顏色：改成「比較淺的深灰」
 *    - flashAlphaTarget 從 0.5f 降到 0.4f
 *    - 疊色 alpha 係數從 0.5f 降到 0.4f
 *    => 視覺上比 v14 更淡一點，還是灰，不是白
 *
 * 2. 高亮圈還是比按鈕大 (outerSizeDp > innerSizeDp)
 *
 * 3. 我們仍用 coroutine 做瞬間閃光，120ms 後自動收掉
 *    => 快速連打不會卡亮
 */
@Composable
private fun RoundActionButton(
    outerSizeDp: Dp,
    innerSizeDp: Dp,
    bgColor: Color,
    borderColor: Color?,
    iconTint: Color,
    iconVector: ImageVector,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 這是「我們想要呈現的亮度」
    var flashAlphaTarget by remember { mutableFloatStateOf(0f) }

    // 用動畫把 alpha 慢慢往 flashAlphaTarget 跑 (淡出不會硬切)
    val animatedAlpha by animateFloatAsState(
        targetValue = flashAlphaTarget,
        label = "pressFlashAlphaAnim"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(outerSizeDp) // 外圈：決定高亮圈的半徑 (比內層大)
            .clickable(
                indication = null, // 我們自己畫高亮圈，不用 ripple
                interactionSource = interactionSource
            ) {
                scope.launch {
                    // 輕灰 / 深灰感：flashAlphaTarget 越大越深
                    flashAlphaTarget = 0.4f   // v15: 比 v14 稍微再淺一點
                    delay(120)
                    flashAlphaTarget = 0f
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 深灰閃光圈，尺寸 = outerSizeDp
        if (animatedAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        // 用黑色乘透明度做成深灰。係數 0.4f => 比 v14 的 0.5f 再淡一點
                        color = Color.Black.copy(alpha = animatedAlpha * 0.4f),
                        shape = CircleShape
                    )
            )
        }

        // 內層實際可見的按鈕 (40dp)
        Box(
            modifier = Modifier
                .size(innerSizeDp)
                .background(bgColor, CircleShape)
                .let { base ->
                    if (borderColor != null) {
                        base.border(width = 1.dp, color = borderColor, shape = CircleShape)
                    } else {
                        base
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

/**
 * WaterIntakeCard v15
 *
 * 改動 vs v14：
 * 1. - / + 之間距離：20.dp -> 16.dp
 * 2. Switch 往上靠近按鈕：
 *    - Spacer(8.dp) -> Spacer(4.dp)
 * 3. 其他保持不變（字體、杯子大小、switch style 等）
 */
@Composable
fun WaterIntakeCard(
    cardHeight: Dp,
    state: WaterUiState,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onToggleUnit: () -> Unit
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
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ===== 左半：水杯 + 數值 =====
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.Top
            ) {

                // 左邊淡藍底方塊 (主視覺)
                Box(
                    modifier = Modifier
                        .size(60.dp) // 背景塊維持 60dp
                        .background(
                            color = Color(0xFFF2F3FF), // 淺藍/淡紫
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 杯子 icon
                    Icon(
                        painter = painterResource(R.drawable.glass),
                        contentDescription = "water",
                        modifier = Modifier.size(28.dp), // 杯子 28dp
                        tint = Color.Unspecified // 保留原色
                    )
                }

                Spacer(Modifier.size(12.dp))

                Column(
                    verticalArrangement = Arrangement.Top
                ) {
                    // 標題 "Water"：要細，不要粗
                    Text(
                        text = "Water",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A)
                        )
                    )

                    Spacer(Modifier.height(4.dp))

                    // 數值 "237 ml (1 cups)" / "16 fl oz (2 cups)"
                    val mainText = when (state.unit) {
                        WaterUnit.ML -> "${state.ml} ml"
                        WaterUnit.OZ -> "${state.flOz} fl oz"
                    }
                    val cupsText = "(${state.cups} cups)"

                    Text(
                        text = "$mainText $cupsText",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                }
            }

            Spacer(Modifier.size(8.dp))

            // ===== 右半：(- / +) + Switch 區 =====
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // 右半整塊靠上：保持 4.dp (v14 也是 4dp，比 v13 的 16dp 更高)
                Spacer(Modifier.height(4.dp))

                // 第一排：- / +
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 減號：白底 + 黑框 + 黑圖示
                    RoundActionButton(
                        outerSizeDp = 50.dp,   // 外圈(高亮圈/點擊區) 大
                        innerSizeDp = 36.dp,   // 內圈實際按鈕本體
                        bgColor = Color.White,
                        borderColor = Color(0xFF111114),
                        iconTint = Color(0xFF111114),
                        iconVector = Icons.Default.Remove,
                        onClick = onMinus
                    )

                    Spacer(Modifier.size(14.dp)) // v15: 20.dp -> 16.dp

                    // 加號：黑底 + 白圖示
                    RoundActionButton(
                        outerSizeDp = 50.dp,
                        innerSizeDp = 36.dp,
                        bgColor = Color(0xFF111114),
                        borderColor = null,
                        iconTint = Color.White,
                        iconVector = Icons.Default.Add,
                        onClick = onPlus
                    )
                }

                // Switch 再往上靠近一點：
                // v14: 8.dp -> v15: 4.dp
                Spacer(Modifier.height(4.dp))

                // 第二排：oz [Switch] ml
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "oz",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (state.unit == WaterUnit.OZ)
                                Color(0xFF0F172A) // 選中高亮
                            else
                                Color(0xFF6B7280) // 未選灰
                        )
                    )

                    Spacer(Modifier.size(6.dp))

                    Switch(
                        checked = (state.unit == WaterUnit.ML),
                        onCheckedChange = { onToggleUnit() },
                        modifier = Modifier.scale(
                            scaleX = 0.9f, // 拉長
                            scaleY = 0.85f  // 稍扁
                        ),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF111114), // ML 模式：黑底
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF9CA3AF), // OZ 模式：灰底
                            uncheckedBorderColor = Color.Transparent,
                            uncheckedIconColor = Color.White,
                            checkedIconColor = Color.White
                        )
                    )

                    Spacer(Modifier.size(6.dp))

                    Text(
                        text = "ml",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (state.unit == WaterUnit.ML)
                                Color(0xFF0F172A) // 高亮
                            else
                                Color(0xFF6B7280) // 灰
                        )
                    )
                }
            }
        }
    }
}
