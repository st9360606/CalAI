    package com.calai.app.ui.home.ui.workout

    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.filled.Close
    import androidx.compose.material3.CenterAlignedTopAppBar
    import androidx.compose.material3.Divider
    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.compose.material3.FilledIconButton
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.IconButtonDefaults
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.SheetState
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors
    import androidx.compose.material3.rememberModalBottomSheetState
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.compose.runtime.setValue
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
    fun WorkoutHistoryScreen(
        vm: WorkoutViewModel,
        onBack: () -> Unit
    ) {
        // 初始化
        LaunchedEffect(Unit) { vm.init() }
        LaunchedEffect(Unit) { vm.refreshToday() }

        val ui by vm.ui.collectAsStateWithLifecycle()
        val today = ui.today
        val total = today?.totalKcalToday ?: 0
        val list = today?.sessions ?: emptyList()

        var showTracker by rememberSaveable { mutableStateOf(false) }
        var navigated by rememberSaveable { mutableStateOf(false) }

        // 共用 BottomSheetState（僅建立一次）
        val trackerSheetState: SheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

        // ✅ 更快返回：一偵測到開始儲存就立刻回 HOME（Toast 交給 HOME 顯示）
        LaunchedEffect(ui.saving) {
            if (ui.saving && !navigated) {
                navigated = true
                showTracker = false
                onBack()
            }
        }

        // （相容路徑：若 VM 仍可能設 navigateHistoryOnce，就消化掉）
        LaunchedEffect(ui.navigateHistoryOnce) {
            if (ui.navigateHistoryOnce) {
                showTracker = false
                vm.consumeNavigateHistory()
            }
        }

        val surface = Color.White
        val onSurface = Color(0xFF111114)
        val onSurfaceSecondary = Color(0xFF6B7280)
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
                        IconButton(onClick = onBack, modifier = Modifier.padding(top = 6.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Workout History",
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    actions = {
                        FilledIconButton(
                            onClick = { onBack() }, // ★ 直接關閉 WorkoutHistoryScreen
                            modifier = Modifier
                                .padding(end = 14.dp, top = 10.dp)
                                .size(32.dp)                 // ★ 保持 36dp 黑圓
                                .clip(CircleShape),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF111114), // 黑底
                                contentColor = Color.White          // 白色 X
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp) // ← X 更大
                            )
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

        // ❌ 不在 History 顯示成功 Toast（交給 HOME 顯示）
        // if (ui.toastMessage != null && !showTracker) { ... } ← 已移除

        // Host 常駐，用 visible 控制顯示
        // Host 常駐，用 visible 控制顯示
        WorkoutTrackerHost(
            vm = vm,
            visible = showTracker,
            onCloseFull = {
                // 完整關閉：收合 + 清 VM 對話框/暫存
                showTracker = false
                vm.dismissDialogs()
            },
            onCollapseOnly = {
                // 只收合：不清 VM（保留 showDurationPickerFor，讓 DurationPicker 的 Save 能成功）
                showTracker = false
                // ⚠️ 千萬不要在這裡呼叫 vm.dismissDialogs()
            },
            sheetState = trackerSheetState
        )
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
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFFB5B5B5),
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

            Text(
                text = session.timeLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = metaColor,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
