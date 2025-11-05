package com.calai.app.ui.home.ui.workout

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet
import com.calai.app.ui.home.ui.workout.components.WorkoutConfirmDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutEstimatingDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutScanFailedDialog
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerHost(
    vm: WorkoutViewModel,
    onClose: () -> Unit,
    sheetState: SheetState,
    visible: Boolean
) {
    val ui = vm.ui.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        if (ui.presets.isEmpty() || ui.today == null) vm.init()
    }
    if (!visible) return

    // ★ Save 後直到關閉前，鎖定顯示 Picker，避免在 VM 改狀態時先看到主單一幀
    var keepPickerUntilClose by remember { mutableStateOf(false) }
    // reset 鎖定：當 Host 關閉時清掉
    LaunchedEffect(visible) {
        if (!visible) keepPickerUntilClose = false
    }

    // 記住最後一次的 picker 名稱，若 VM 先清空 picker 再關閉，也能顯示正確標題
    var lastPickerName by remember { mutableStateOf("Workout") }
    LaunchedEffect(ui.showDurationPickerFor?.name) {
        ui.showDurationPickerFor?.name?.let { lastPickerName = it }
    }

    val picker = ui.showDurationPickerFor
    val showPicker = (picker != null) || keepPickerUntilClose

    if (showPicker) {
        DurationPickerSheet(
            presetName = picker?.name ?: lastPickerName,
            onSaveMinutes = { minutes ->
                // ★ 先鎖再交給 VM（A 方案內已先 hide()，這裡是雙重保險）
                keepPickerUntilClose = true
                vm.savePresetDuration(minutes)
            },
            onCancel = { vm.dismissDialogs() },
            sheetState = sheetState
        )
    } else {
        WorkoutTrackerSheet(
            uiState = ui,
            onClose = onClose,
            onTextChanged = vm::onTextChanged,
            onAddWorkout = { vm.estimate() },
            onClickPresetPlus = { preset -> vm.openDurationPicker(preset) },
            onToastCleared = { vm.clearToast() },
            sheetState = sheetState
        )
    }

    if (ui.estimating) {
        WorkoutEstimatingDialog(onDismiss = { /* loading 中不允許取消 */ })
    }
    ui.estimateResult?.let { r ->
        WorkoutConfirmDialog(
            result = r,
            onSave = { vm.confirmSaveFromEstimate() },
            onCancel = { vm.dismissDialogs() }
        )
    }
    if (ui.errorScanFailed) {
        WorkoutScanFailedDialog(
            onTryAgain = { vm.dismissDialogs() },
            onCancel = { vm.dismissDialogs() }
        )
    }
}
