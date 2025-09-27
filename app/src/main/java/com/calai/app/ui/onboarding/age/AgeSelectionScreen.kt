package com.calai.app.ui.onboarding.age

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.ui.common.FlagChip
import com.calai.app.ui.common.OnboardingProgress
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AgeSelectionScreen(
    vm: AgeSelectionViewModel,      // ← 傳進 ViewModel
    onBack: () -> Unit,
    onNext: () -> Unit,             // ← 只通知導頁，不再回傳年齡
    minAge: Int = 10,
    maxAge: Int = 100
) {
    // 從 DataStore 讀取已保存年齡作為初始值
    val persistedAge = vm.ageState.collectAsState().value
    var selectedAge by remember(persistedAge) {
        mutableIntStateOf(persistedAge.coerceIn(minAge, maxAge))
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(39.dp)
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
                }
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        vm.saveAge(selectedAge)  // ← 直接在畫面內保存
                        onNext()                 // ← 再通知外層導頁
                    },
                    enabled = true,
                    modifier = Modifier
                        .padding(horizontal = 3.dp, vertical = 26.dp)
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            // ✅ 與性別頁相同位置與邊距的進度條
            OnboardingProgress(
                stepIndex = 3,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Text(
                text = stringResource(R.string.onboard_age_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp),
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
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            AgeWheel(
                minAge = minAge,
                maxAge = maxAge,
                value = selectedAge,
                onValueChange = { selectedAge = it },
                rowHeight = 56.dp,
                centerTextSize = 40.sp,
                sideAlpha = 0.35f
            )
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
    sideAlpha: Float
) {
    val items = remember(minAge, maxAge) { (minAge..maxAge).toList() }
    val initialIndex = remember(value) { (value - minAge).coerceIn(0, items.lastIndex) }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val scope = rememberCoroutineScope()

    // 先在可組合環境取得 density，並把 row 高度換成 px 給 effect 使用
    val density = LocalDensity.current
    val rowPx = remember(density, rowHeight) { with(density) { rowHeight.toPx() } }

    // 停止滾動後 snap 到最近列並回寫值
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val index = state.firstVisibleItemIndex +
                    if (state.firstVisibleItemScrollOffset > rowPx / 2f) 1 else 0
            val clamped = index.coerceIn(0, items.lastIndex)
            onValueChange(items[clamped])
            scope.launch { state.animateScrollToItem(clamped) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight * 5)
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = rowHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, age ->
                val centerIndex = state.firstVisibleItemIndex + 2
                val distance = abs(index - centerIndex)
                val isCenter = distance == 0
                val alpha = if (isCenter) 1f else sideAlpha
                val size = if (isCenter) centerTextSize else 28.sp
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

        val lineColor = Color(0x11000000)
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .offset(y = rowHeight - 1.dp)
                .height(1.dp)
                .background(lineColor)
        )
    }
}
