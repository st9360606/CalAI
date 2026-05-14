package com.calai.bitecal.ui.home.components.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.components.scan.ScanCameraIcon

private val ScrimColor = Color.Black.copy(alpha = 0.16f)
private val TileColor = Color(0xFFF0F1F6)
private val LabelColor = Color(0xFF202124)

private val MenuSidePadding = 20.dp
private val MenuBottomPadding = 116.dp
private val MenuCardSpacing = 12.dp

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
                .padding(
                    start = MenuSidePadding,
                    end = MenuSidePadding,
                    bottom = MenuBottomPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(MenuCardSpacing)
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            )

            QuickActionCard(
                label = stringResource(R.string.quick_add_scan_food),
                testTag = "quick_add_scan_food_card",
                onClick = onScanFoodClick,
                icon = {
                    ScanCameraIcon(
                        modifier = Modifier.size(30.dp),
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
            .width(170.dp)
            .height(125.dp)
            .testTag(testTag),
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        border = CardStyles.Border,
        colors = CardDefaults.cardColors(
            containerColor = CardStyles.Bg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 21.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = TileColor,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                color = LabelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}