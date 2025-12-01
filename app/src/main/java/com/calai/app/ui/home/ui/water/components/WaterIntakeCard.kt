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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.data.water.store.WaterUnit
import com.calai.app.ui.home.components.CardStyles
import com.calai.app.ui.home.ui.water.model.WaterUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RoundActionButton v16.1
 * - 覆蓋層改用 fillMaxSize()，避免 matchParentSize() import 爭議。
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

    // 控制閃光的目標亮度
    var flashAlphaGoal by remember { mutableFloatStateOf(0f) }

    // 用動畫平滑淡出
    val animatedAlpha by animateFloatAsState(
        targetValue = flashAlphaGoal,
        label = "pressFlashAlphaAnim"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(outerSizeDp) // 外圈半徑（也是高亮圈大小）
            .clickable(
                indication = null, // 我們自己畫閃光，所以不要 ripple
                interactionSource = interactionSource
            ) {
                scope.launch {
                    // 亮一下深灰圈（比按鈕大）
                    flashAlphaGoal = 0.4f
                    delay(120)
                    flashAlphaGoal = 0f
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 深灰閃光圈，尺寸 = outerSizeDp，比內層按鈕大一圈
        if (animatedAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize() // ← 取代 matchParentSize()
                    .background(
                        color = Color.Black.copy(alpha = animatedAlpha * 0.4f),
                        shape = CircleShape
                    )
            )
        }

        // 內層實際按鈕 (顯示出來的 - / +)
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
 * WaterIntakeCard v17
 * - 單位切換改用 UnitSwitchLabeled（文字內嵌在切換鈕上）
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

                // 左邊淺藍底塊 (主視覺)
                Box(
                    modifier = Modifier
                        .size(60.dp) // 保持 60dp
                        .background(
                            color = Color(0xFFE8EAFF), // 淺藍/淡紫
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
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // "Water" 細字
                    Text(
                        text = "Water",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A)
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    // 當前數值 e.g. "237 ml (1 cups)" or "16 fl oz (2 cups)"
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

            // ===== 右半：(- / +) + 切換鈕 =====
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 第一排：- / +
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 減號：白底 + 黑框 + 黑icon
                    RoundActionButton(
                        outerSizeDp = 50.dp,   // 點擊/閃光區 (比較大)
                        innerSizeDp = 38.dp,   // 按鈕本體
                        bgColor = Color.White,
                        borderColor = Color(0xFF111114),
                        iconTint = Color(0xFF111114),
                        iconVector = Icons.Default.Remove,
                        onClick = onMinus
                    )

                    Spacer(Modifier.size(14.dp)) // 兩顆按鈕距離

                    // 加號：黑底 + 白icon
                    RoundActionButton(
                        outerSizeDp = 50.dp,
                        innerSizeDp = 38.dp,
                        bgColor = Color(0xFF111114),
                        borderColor = null,
                        iconTint = Color.White,
                        iconVector = Icons.Default.Add,
                        onClick = onPlus
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 第二排：單位切換（文字在切換鈕上）
                UnitSwitchLabeled(
                    checked = (state.unit == WaterUnit.ML), // true=ml, false=oz
                    onCheckedChange = { newChecked ->
                        val isMlNow = (state.unit == WaterUnit.ML)
                        if (newChecked != isMlNow) onToggleUnit()
                    },
                    width = 92.dp,
                    height = 30.dp,
                    leftLabel = "oz",
                    rightLabel = "ml",
                    trackBase = Color(0xFF888888).copy(alpha = 0.25f),
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp // ★ 再放大
                    )
                )
            }
        }
    }
}
