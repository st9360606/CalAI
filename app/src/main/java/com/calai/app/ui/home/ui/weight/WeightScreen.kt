package com.calai.app.ui.home.ui.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calai.app.ui.home.ui.components.ErrorTopToast
import com.calai.app.ui.home.ui.components.SuccessTopToast
import com.calai.app.ui.home.ui.weight.components.FilterTabs
import com.calai.app.ui.home.ui.weight.components.HistoryRow
import com.calai.app.ui.home.ui.weight.components.SegmentedButtons
import com.calai.app.ui.home.ui.weight.components.SummaryCards
import com.calai.app.ui.home.ui.weight.components.WeightChartCard
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    vm: WeightViewModel,
    onLogClick: () -> Unit,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.initIfNeeded() }

    Scaffold(
        containerColor = Color(0xFFF5F5F5), // ★ 整頁白底
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5),      // ★ AppBar 白底
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                ),
                title = {
                    Text(
                        text = "Weight",
                        style = MaterialTheme.typography.headlineMedium, // 比 Overview 大一階
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp) // 與螢幕邊緣更遠
                            .size(48.dp)          // 觸控目標 48dp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp) // 箭頭放大
                        )
                    }
                }
                // ← actions 移除，SegmentedButtons 會放到 Overview 右側
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("+ Log Weight") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onLogClick
            )
        }
    ) { inner ->
        Box(Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // ★ 保險：內容區白底
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                // ↓ 把 top 從 16.dp 改為 8.dp，縮短 Weight 與 Overview 的距離
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Overview（更大更粗） + 右側單位切換
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleLarge, // 比先前 titleMedium 大一點
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)   // 微調視覺右移
                        )
                        SegmentedButtons(
                            selected = ui.unit,
                            onSelect = { vm.setUnit(it) },
                            // 這裡可放寬高度，依畫面調整
                            width = 108.dp,
                            height = 36.dp,
                            pillExtraWidth = 6.dp,
                            labelPadding = 6.dp
                        )
                    }
                }
                item { SummaryCards(ui = ui) }
                item {
                    FilterTabs(
                        selected = ui.range,
                        onSelect = { vm.setRange(it) }
                    )
                }
                item {
                    WeightChartCard(
                        ui = ui,
                        // ★ 把全時段第一筆傳給 chart
                        startWeightAllTimeKg = ui.firstWeightAllTimeKg  // ★ 這行一定要傳
                    )
                }
                item {
                    // History：與 Overview 同層級
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                items(ui.history7) { item ->
                    HistoryRow(item, ui.unit)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }

            // 成功提示（2 秒後清除）
            ui.toastMessage?.let { toast ->
                SuccessTopToast(message = toast, modifier = Modifier.align(Alignment.TopCenter))
                LaunchedEffect(toast) {
                    delay(2000)
                    vm.clearToast()
                }
            }

            // 錯誤提示
            ui.error?.let { err ->
                ErrorTopToast(message = err, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
