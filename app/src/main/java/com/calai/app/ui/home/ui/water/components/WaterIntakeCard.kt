package com.calai.app.ui.home.ui.water.components

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.Role
/**
 * RoundActionButton v16
 *
 * 和 v15 幾乎相同：
 * - outerSizeDp：外圈(高亮圈/點擊區)，比按鈕本體大
 * - innerSizeDp：真正顯示的按鈕大小
 * - 點擊時顯示深灰半透明圓形(比按鈕大)，120ms 後自動淡掉
 *
 * 差異 vs 早期版本：
 * - flashAlphaTarget = 0.4f，顏色是黑色 * 0.4f -> 視覺是淺一點的深灰
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
    var flashAlphaTarget by remember { mutableFloatStateOf(0f) }

    // 用動畫平滑淡出
    val animatedAlpha by animateFloatAsState(
        targetValue = flashAlphaTarget,
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
                    flashAlphaTarget = 0.4f
                    delay(120)
                    flashAlphaTarget = 0f
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 深灰閃光圈，尺寸 = outerSizeDp，比內層按鈕大一圈
        if (animatedAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
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
 * WaterIntakeCard v16
 *
 * 變更點 vs 你給的版本：
 * - Switch：拿掉 Modifier.scale(...)，回復原生大小，所以白色 thumb(圓形)不會縮小。
 * - 其他 spacing、按鈕行為、深灰閃光都維持。
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

            // ===== 右半：(- / +) + Switch 區 =====
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // 靠上（4dp）
                Spacer(Modifier.height(0.dp))

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

                // Switch 再往上靠近 (4dp)
                Spacer(Modifier.height(10.dp))

                // 第二排：oz [Switch] ml
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "oz",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (state.unit == WaterUnit.OZ)
                                Color(0xFF0F172A) // 高亮
                            else
                                Color(0xFF6B7280) // 灰
                        )
                    )

                    Spacer(Modifier.size(6.dp))

                    // 🔥 v16 變更：
                    // 移除 Modifier.scale(...)，用原生 Switch 尺寸
                    // → 白色圓球(thumb) 不會被縮小或壓扁
                    UnitSwitch(
                        checked = (state.unit == WaterUnit.ML),
                        onCheckedChange = { onToggleUnit() },
                        width = 46.dp,
                        height = 32.dp,
                        thumbSize = 18.dp,        // 固定白圓大小
                        checkedTrack = Color(0xFF111114),
                        uncheckedTrack = Color(0xFF9CA3AF)
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
@Composable
private fun UnitSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 46.dp,          // 窄長一點
    height: Dp = 32.dp,         // 與設計相符的高度
    thumbSize: Dp = 18.dp,      // 白色圓固定尺寸（不縮）
    padding: Dp = 3.dp,         // 內距，讓 thumb 不會貼邊
    checkedTrack: Color = Color(0xFF111114),   // 黑色
    uncheckedTrack: Color = Color(0xFF9CA3AF), // 灰色
    thumbColor: Color = Color.White
) {
    val interaction = remember { MutableInteractionSource() }
    val targetX = if (checked) (width - thumbSize - padding) else padding
    val animatedX by animateDpAsState(targetValue = targetX, label = "unitSwitchThumbX")

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(height / 2))
            .background(if (checked) checkedTrack else uncheckedTrack)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
                indication = null, // 不要 ripple
                interactionSource = interaction
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // 固定尺寸的白色圓形 thumb（不會縮小）
        Box(
            modifier = Modifier
                .offset(x = animatedX)
                .size(thumbSize)
                .background(thumbColor, CircleShape)
        )
    }
}

