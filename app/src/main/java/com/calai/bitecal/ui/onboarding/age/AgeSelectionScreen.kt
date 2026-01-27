package com.calai.bitecal.ui.onboarding.age

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.OnboardingProgress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AgeSelectionScreen(
    vm: AgeSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    minAge: Int = 10,
    maxAge: Int = 150
) {
    // 從 DataStore 讀取已保存年齡作為初始值
    val persistedAge = vm.ageState.collectAsState().value
    var selectedAge by remember(persistedAge) {
        mutableIntStateOf(persistedAge.coerceIn(minAge, maxAge))
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F3F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111114)
                            )
                        }
                    }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingProgress(
                            stepIndex = 3,
                            totalSteps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        vm.saveAge(selectedAge)
                        onNext()
                    },
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.continue_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboard_age_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboard_age_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9AA3AF),
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(80.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgeWheel(
                    minAge = minAge,
                    maxAge = maxAge,
                    value = selectedAge,
                    onValueChange = { selectedAge = it },
                    rowHeight = 68.dp,
                    centerTextSize = 38.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(220.dp)
            )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgeWheel(
    minAge: Int,
    maxAge: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    sideAlpha: Float,
    modifier: Modifier = Modifier
) {
    val visibleCount = 5
    val mid = visibleCount / 2

    val items = remember(minAge, maxAge) { (minAge..maxAge).toList() }
    val selectedIdx = remember(value) { (value - minAge).coerceIn(0, items.lastIndex) }

    // 讓 selected 一開始出現在正中（僅影響初始）
    val firstForCenter = remember(selectedIdx, items) {
        (selectedIdx - mid).coerceIn(0, (items.lastIndex - (visibleCount - 1)).coerceAtLeast(0))
    }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = firstForCenter)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // 以「視窗中心點」找出最接近的那一列（黑色＝框內）
    val centerIndex by remember {
        derivedStateOf {
            val li = state.layoutInfo
            if (li.visibleItemsInfo.isEmpty()) return@derivedStateOf selectedIdx
            val viewportCenter = (li.viewportStartOffset + li.viewportEndOffset) / 2
            li.visibleItemsInfo.minByOrNull { info ->
                kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
            }?.index ?: selectedIdx
        }
    }

    // 即時把中心列回傳 → 「下一步」一定存黑色那個
    LaunchedEffect(centerIndex) {
        onValueChange(items[centerIndex])
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * visibleCount)
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = rowHeight * mid),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, age ->
                val isCenter = index == centerIndex
                val alpha = if (isCenter) 1f else sideAlpha
                val size = if (isCenter) centerTextSize else 26.sp
                val weight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .height(rowHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = age.toString(),
                        fontSize = size,
                        fontWeight = weight,
                        color = Color.Black.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 中心框線：中心 ± 半格
        val lineColor = Color(0x11000000)
        val half = rowHeight / 2
        val lineThickness = 1.dp
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = -half)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = half - lineThickness)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor)
        )
    }
}
