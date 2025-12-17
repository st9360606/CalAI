package com.calai.app.ui.home.ui.fasting.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import com.calai.app.ui.home.components.CardStyles
import com.calai.app.ui.home.components.TitlePrefixTriangle

object TopBarDefaults {
    val Height = 26.dp
    val HorizontalPadding = 16.dp
}

@Composable
fun TopBarCard(
    title: String,
    modifier: Modifier = Modifier,
    topBarHeight: Dp = TopBarDefaults.Height,
    topBarTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    showWhiteTriangle: Boolean = false,           // Weight 需要
    triangleSide: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            // ★ 統一陰影：讓卡片浮起來（跟 Home 其他卡片一致）
            .shadow(
                CardStyles.Elevation,
                CardStyles.Corner,
                clip = false
            ),
        shape = CardStyles.Corner, // 20.dp 圓角一致
        colors = CardDefaults.cardColors(
            containerColor = CardStyles.Bg // 微暖白，不是死白
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardStyles.Border      // 更明顯的灰邊框
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 黑底白字頂欄（可選擇顯示白色三角形）
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showWhiteTriangle) {
                        TitlePrefixTriangle(side = triangleSide, color = Color.White)
                    }
                    Text(
                        text = title,
                        style = topBarTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 卡片內容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                content = content
            )
        }
    }
}
