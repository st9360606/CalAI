package com.calai.bitecal.ui.home.ui.card.water

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.data.water.store.WaterUnit
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.components.HomeCardStyles
import com.calai.bitecal.ui.home.ui.card.water.components.WaterUnitSwitchLabeled
import com.calai.bitecal.ui.home.ui.card.water.model.WaterUiState

/**
 * RoundActionButton
 * - Keep this stateless; rapid water clicks should not start per-click animation coroutines.
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
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(outerSizeDp)
            .biteCalClickable(
                indication = null,
                interactionSource = interactionSource
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
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
            .height(cardHeight),
        shape = CardStyles.Corner,
        border = CardStyles.Border,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg)
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
                        .size(60.dp)
                        .background(
                            color = HomeCardStyles.Palette.Water.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.glass),
                        contentDescription = "Water Icon",
                        modifier = Modifier.size(28.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(Modifier.size(12.dp))

                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // "Water" label：小圓點 + 輕量文字，讓下方攝取量成為主資訊
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = HomeCardStyles.Palette.Water.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                        )

                        Spacer(Modifier.size(6.dp))

                        Text(
                            text = stringResource(R.string.water_intake_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.1.sp,
                                color = HomeCardStyles.Text.Primary.copy(alpha = 0.72f)
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // 當前數值 e.g. "237 ml (1 cups)" or "16 fl oz (2 cups)"
                    val mainText = when (state.unit) {
                        WaterUnit.ML -> "${state.ml} ml"
                        WaterUnit.OZ -> "${state.flOz} fl oz"
                    }
                    val cupsText = stringResource(R.string.water_intake_cups_format, state.cups)

                    Text(
                        text = "$mainText $cupsText",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = HomeCardStyles.Text.Primary
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
                WaterUnitSwitchLabeled(
                    checked = (state.unit == WaterUnit.ML),
                    onCheckedChange = { newChecked ->
                        val isMlNow = (state.unit == WaterUnit.ML)
                        if (newChecked != isMlNow) onToggleUnit()
                    },
                    width = 100.dp,
                    height = 32.dp,
                    leftLabel = "oz",
                    rightLabel = "ml",
                )
            }
        }
    }
}
