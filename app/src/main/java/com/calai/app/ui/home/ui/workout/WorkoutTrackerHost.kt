package com.calai.app.ui.home.ui.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerHost(
    vm: WorkoutViewModel,
    visible: Boolean,
    // ★ 完整關閉（收合 + 清 VM 對話框/暫存）
    onCloseFull: () -> Unit,
    // ★ 只收合（不清 VM，給 DurationPicker 使用）
    onCollapseOnly: () -> Unit,
    @Suppress("UNUSED_PARAMETER") sheetState: SheetState? = null,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // 若尚未載入，這裡可以安全 init（不會重複）
    LaunchedEffect(Unit) {
        if (ui.presets.isEmpty() || ui.today == null) vm.init()
    }

    // Host 只負責承載 UnifiedSheet，不處理吐司（吐司統一在 Home 顯示）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        WorkoutUnifiedSheet(
            vm = vm,
            visible = visible,
            onClose = { // ★ 關閉：清 VM
                vm.dismissDialogs()
                onCloseFull()
            },
            onCollapse = { // ★ 只收合：不清 VM（保留 showDurationPickerFor）
                onCollapseOnly()
            }
        )
    }
}
