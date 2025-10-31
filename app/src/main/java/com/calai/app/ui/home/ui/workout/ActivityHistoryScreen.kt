package com.calai.app.ui.home.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.data.workout.api.WorkoutSessionDto
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    vm: WorkoutViewModel,
    onBack: () -> Unit
) {
    // ✅ 防守式初始化（ViewModel 有 initialized 旗標，不會重複載）
    LaunchedEffect(Unit) { vm.init() }

    val ui by vm.ui.collectAsStateWithLifecycle()
    val today = ui.today
    val total = today?.totalKcalToday ?: 0
    val list = today?.sessions ?: emptyList()

    // 控制是否顯示 WorkoutTrackerSheet
    var showTracker by rememberSaveable { mutableStateOf(false) }

    // ⭐ 關鍵修正：在歷史頁內消化 navigateHistoryOnce，避免回 HOME 後又被導回來
    LaunchedEffect(ui.navigateHistoryOnce) {
        if (ui.navigateHistoryOnce) {
            showTracker = false         // 關 Sheet
            vm.consumeNavigateHistory() // 立刻清旗標
            // 不做任何導航：因為我們已經在歷史頁
        }
    }

    val surface = Color.White
    val onSurface = Color(0xFF111114)
    val onSurfaceSecondary = Color(0xFF6B7280) // 次要文字灰
    val divider = Color(0xFFE5E7EB)

    Scaffold(
        containerColor = surface,
        topBar = {
            CenterAlignedTopAppBar(
                colors = centerAlignedTopAppBarColors(
                    containerColor = surface,
                    navigationIconContentColor = onSurface,
                    titleContentColor = onSurface,
                    actionIconContentColor = onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 6.dp) // 與標題同一水平
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Activity History",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                actions = {
                    // 右上黑圓白加號（點擊彈出 WorkoutTrackerSheet）
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp, top = 6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111114)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { showTracker = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "add workout",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Total today: $total kcal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                )
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list) { s: WorkoutSessionDto ->
                    HistoryRow(
                        session = s,
                        nameColor = onSurface,
                        metaColor = onSurfaceSecondary
                    )
                    Divider(
                        color = divider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }

    // === 這裡直接掛出「新增運動」的 BottomSheet Host ===
    if (showTracker) {
        WorkoutTrackerHost(
            vm = vm,
            onClose = { showTracker = false } // scrim / X / 系統手勢都會關閉
        )
    }
}

@Composable
private fun HistoryRow(
    session: WorkoutSessionDto,
    nameColor: Color,
    metaColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 左側綠色圓（placeholder）
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color(0xFF84CC16),
            contentColor = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = session.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = nameColor
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\u23F0 ${session.minutes} min",
                    style = MaterialTheme.typography.bodyMedium.copy(color = metaColor)
                )
                Text(
                    text = "\uD83D\uDD25 ${session.kcal} kcal",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDC2626))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 右邊時間（後端已提供 24 小時制 "HH:mm"）
        Text(
            text = session.timeLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = metaColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
