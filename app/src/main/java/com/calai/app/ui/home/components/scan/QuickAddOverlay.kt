package com.calai.app.ui.home.components.scan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.R

/**
 * 快捷面板 Overlay（只留：记录运动 / 扫描食物）
 * - 不是 ModalBottomSheet，因為你要 TabBar/FAB 都還看得到
 */
@Composable
fun QuickAddOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onLogWorkout: () -> Unit,
    onOpenScanFoods: () -> Unit,
    modifier: Modifier = Modifier,

    // ===== 可微調參數（讓你校到 1:1）=====
    bottomPadding: Dp = 92.dp,
    panelCorner: Dp = 26.dp,
    tileCorner: Dp = 18.dp,
    tileHeight: Dp = 92.dp,
    gap: Dp = 12.dp,
) {
    val scrim = Color.Black.copy(alpha = 0.35f)
    val panelBg = Color(0xFF2A2A2E)
    val tileBg = Color(0xFFE9EBEF)
    val iconBoxBg = Color(0xFFF3F4F6)
    val iconTint = Color(0xFF606774)
    val textColor = Color(0xFF2B2F36)

    val scrimInteraction = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)) + slideInVertically(
            animationSpec = tween(180),
            initialOffsetY = { it / 6 }
        ),
        exit = fadeOut(tween(120)) + slideOutVertically(
            animationSpec = tween(160),
            targetOffsetY = { it / 6 }
        ),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            // 背景半透明遮罩：點擊即可關閉
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        role = Role.Button
                    ) { onDismiss() }
            )

            // 面板（貼近底部）
            Surface(
                color = panelBg,
                shape = RoundedCornerShape(panelCorner),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 18.dp, end = 18.dp, bottom = bottomPadding)
                    .fillMaxWidth()
                    .semantics { }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        QuickAddTile(
                            label = stringResource(R.string.quick_add_log_workout),
                            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                            iconBoxBg = iconBoxBg,
                            tileBg = tileBg,
                            textColor = textColor,
                            iconTint = iconTint,
                            height = tileHeight,
                            corner = tileCorner,
                            onClick = {
                                onLogWorkout()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickAddTile(
                            label = stringResource(R.string.quick_add_scan_food),
                            icon = Icons.Outlined.QrCodeScanner,
                            iconBoxBg = iconBoxBg,
                            tileBg = tileBg,
                            textColor = textColor,
                            iconTint = iconTint,
                            height = tileHeight,
                            corner = tileCorner,
                            onClick = {
                                onOpenScanFoods()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp,
    corner: Dp,
    tileBg: Color,
    iconBoxBg: Color,
    iconTint: Color,
    textColor: Color,
) {
    val interaction = remember { MutableInteractionSource() }

    Surface(
        color = tileBg,
        shape = RoundedCornerShape(corner),
        modifier = modifier
            .height(height)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = iconBoxBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
