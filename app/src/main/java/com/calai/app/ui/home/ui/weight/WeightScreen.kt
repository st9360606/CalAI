package com.calai.app.ui.home.ui.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.ui.home.ui.weight.components.FilterTabs
import com.calai.app.ui.home.ui.weight.components.HistoryRow
import com.calai.app.ui.home.ui.weight.components.SegmentedButtons
import com.calai.app.ui.home.ui.weight.components.SummaryCards
import com.calai.app.ui.home.ui.weight.components.WeightChartCard
import com.calai.app.ui.home.ui.weight.components.WeightTopBar
import com.calai.app.ui.home.ui.weight.model.WeightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    fun WeightScreen(
    vm: WeightViewModel,
    onLogClick: () -> Unit,
    onEditTargetWeight: () -> Unit,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.initIfNeeded() }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Weight",
                onBack = onBack
            )
        },
        bottomBar = {
            BottomLogWeightBar(
                onLogClick = onLogClick
            )
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 6.dp,
                    end = 16.dp,
                    bottom = 96.dp // ★ 為底部固定按鈕預留空間
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                // Overview + Unit switch
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overview",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        SegmentedButtons(
                            selected = ui.unit,
                            onSelect = { vm.setUnit(it) },
                            width = 108.dp,
                            height = 36.dp,
                            pillExtraWidth = 6.dp,
                            labelPadding = 6.dp
                        )
                    }
                }
                // Summary cards
                item { SummaryCards(ui = ui) }

                // Filter tabs
                item {
                    FilterTabs(
                        selected = ui.range,
                        onSelect = { vm.setRange(it) }
                    )
                }
                // Chart card
                item {
                    WeightChartCard(
                        ui = ui,
                        startWeightAllTimeKg = ui.firstWeightAllTimeKg,
                        onEditTargetWeight = onEditTargetWeight
                    )
                }
                // History title
                item {
                    Text(
                        text = "History",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                // History items
                items(ui.history7) { item ->
                    HistoryRow(item, ui.unit)
                }
            }
        }
    }
}

@Composable
private fun BottomLogWeightBar(
    onLogClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            // 先吃掉系統 navigation bar 的安全區
            .windowInsetsPadding(WindowInsets.navigationBars)
            // 再自己多加一點底部 padding，讓按鈕往上浮
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 16.dp      // ★ 核心：底部多留一點空間
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onLogClick,
            modifier = Modifier
                .width(158.dp)      // 你調過的寬度，覺得 OK 就維持
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111114),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.height(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Log Weight",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
