package com.calai.bitecal.ui.home.ui.fasting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.components.HomeCardStyles
import com.calai.bitecal.ui.home.ui.card.TitlePrefixTriangle

object TopBarDefaults {
    val Height = HomeCardStyles.TopBar.Height
    val HorizontalPadding = HomeCardStyles.TopBar.HorizontalPadding
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
        modifier = modifier,
        shape = CardStyles.Corner, // 20.dp 圓角一致
        colors = CardDefaults.cardColors(
            containerColor = CardStyles.Bg // 微暖白，不是死白
        ),
        border = CardStyles.Border      // 更明顯的灰邊框
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 黑底白字頂欄（可選擇顯示白色三角形）
            Surface(
                color = HomeCardStyles.TopBar.Container,
                contentColor = HomeCardStyles.TopBar.Content,
                shape = HomeCardStyles.TopBar.Shape,
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
                        TitlePrefixTriangle(side = triangleSide, color = HomeCardStyles.TopBar.Content)
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
