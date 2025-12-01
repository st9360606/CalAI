package com.calai.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ScrollingNumberWheel（亮色版）
 *
 * - 白底情境下使用黑字
 * - 中央行：20.sp、Bold、alpha=1f
 * - 其他行：19.sp、Normal、alpha=0.35f
 * - 上下用 null padding 讓 00/30 能一開始就在中央
 * - 滑動停止後自動 snap 並回報 onValueChange
 */
@Composable
fun ScrollingNumberWheel(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF111114), // 黑字為預設
) {
    val itemHeight = 48.dp
    val visibleCount = 5            // 上2 + 中1 + 下2
    val paddingCount = 2            // 上下各補2列null
    val centerOffset = paddingCount // 螢幕中央相對firstVisible的位置 (=2)

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // e.g. [null, null, 0,1,2,3,...,59, null, null]
    val dataList: List<Int?> = remember(range) {
        val head = List(paddingCount) { null }
        val tail = List(paddingCount) { null }
        head + range.toList() + tail
    }

    // 現在 value 在 dataList 裡的 index
    val valueIndexInData = remember(range, value) {
        val raw = (value - range.first).coerceIn(0, range.count() - 1)
        paddingCount + raw
    }

    // LazyColumn 起始 firstVisible，要讓 value 出現在中央(=index+2)
    val initialFirstVisible = remember(range, value) {
        (valueIndexInData - centerOffset)
            .coerceIn(0, dataList.lastIndex)
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisible
    )

    // 停止滾動 => snap central line
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val pxPerItem = with(density) { itemHeight.toPx() }

            val floatingIndex =
                listState.firstVisibleItemIndex.toFloat() +
                        (listState.firstVisibleItemScrollOffset.toFloat() / pxPerItem)

            // 中央 index = topIndex + 2
            val centerIndex = (floatingIndex + centerOffset)
                .roundToInt()
                .coerceIn(0, dataList.lastIndex)

            // 我們希望 firstVisible = centerIndex - 2
            val goalFirstVisible = (centerIndex - centerOffset)
                .coerceIn(0, dataList.lastIndex)

            scope.launch {
                listState.animateScrollToItem(goalFirstVisible)
            }

            val newValue = dataList.getOrNull(centerIndex)
            if (newValue != null && newValue != value) {
                onValueChange(newValue)
            }
        }
    }

    Box(
        modifier = modifier
            .width(60.dp)
            .height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(dataList.size) { idx ->
                val number = dataList[idx]

                val currentCenterIndex = (listState.firstVisibleItemIndex + centerOffset)
                    .coerceIn(0, dataList.lastIndex)

                val isSelected = idx == currentCenterIndex
                val isPlaceholder = (number == null)

                // 這裡沿用你目前的字級（中心20sp，其它19sp）
                val fontSize = if (isSelected) 20.sp else 19.sp
                val alpha = when {
                    isPlaceholder -> 0f
                    isSelected    -> 1f
                    else          -> 0.35f
                }
                val weight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = if (number == null) "" else number.toString().padStart(2, '0'),
                        color = textColor.copy(alpha = alpha),
                        fontSize = fontSize,
                        fontWeight = weight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
