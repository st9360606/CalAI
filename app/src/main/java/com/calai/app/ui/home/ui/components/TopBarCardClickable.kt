package com.calai.app.ui.home.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.ui.home.components.CardStyles
import com.calai.app.ui.home.components.TitlePrefixTriangle
import com.calai.app.ui.home.ui.fasting.components.TopBarDefaults

/**
 * 簡易「可點擊版」TopBar 卡片：
 * - 維持 TopBarCard 的外觀與佈局
 * - 整張卡片可點擊（預設不顯示 ripple，避免整張變暗）
 */
@Composable
fun TopBarCardClickable(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    topBarHeight: Dp = TopBarDefaults.Height,
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    showWhiteTriangle: Boolean = false,
    triangleSide: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,           // 若想要墨水波紋可改成 rememberRipple()
                role = Role.Button,
                onClick = onClick
            ),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardStyles.Border
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight)
                        .padding(horizontal = TopBarDefaults.HorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showWhiteTriangle) {
                        TitlePrefixTriangle(side = triangleSide, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = title,
                        style = topBarTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                content = content
            )
        }
    }
}
