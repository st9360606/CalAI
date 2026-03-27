package com.calai.bitecal.ui.home.components.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.components.scan.ScanCameraIcon

private val ScrimColor = Color.Black.copy(alpha = 0.16f)
private val CardColor = Color.White
private val TileColor = Color(0xFFF7F7FB)
private val LabelColor = Color(0xFF202124)

@Composable
fun HomeQuickActionMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSavedFoodsClick: () -> Unit,
    onScanFoodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    BackHandler(enabled = visible, onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("quick_add_menu")
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(ScrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 104.dp)
        ) {
            QuickActionCard(
                label = stringResource(R.string.quick_add_saved_foods),
                testTag = "quick_add_saved_foods_card",
                onClick = onSavedFoodsClick,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF202124),
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            QuickActionCard(
                label = stringResource(R.string.quick_add_scan_food),
                testTag = "quick_add_scan_food_card",
                onClick = onScanFoodClick,
                icon = {
                    ScanCameraIcon(
                        modifier = Modifier.size(24.dp),
                        frameRatio = 0.86f,
                        cornerLenRatio = 0.30f,
                        cornerRoundness = 0.62f,
                        frameStrokeWidth = 1.5.dp,
                        frameAlpha = 0.62f,
                        plusSizeRatio = 0.44f,
                        plusStrokeWidth = 1.8.dp,
                        color = Color(0xFF202124)
                    )
                }
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    testTag: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .width(156.dp)
            .height(116.dp)
            .testTag(testTag),
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = TileColor,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = label,
                color = LabelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
