package com.calai.app.ui.home.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet
import com.calai.app.ui.home.ui.workout.components.WorkoutConfirmDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutEstimatingDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutScanFailedDialog
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel

/**
 * Host: 進入 Routes.WORKOUT 時呼叫這個。
 *
 * - 第一次進來時呼叫 vm.init() 去抓 presets & today。
 * - 根據 vm.ui.showDurationPickerFor：
 *   * null    -> 顯示主的 WorkoutTrackerSheet (白底，輸入框/預設清單)
 *   * 非 null -> 顯示 DurationPickerSheet (挑時長)
 *
 * 另外三個情境：
 *   ui.estimating            -> loading 動畫 (5.jpg)
 *   ui.estimateResult !=null -> 確認 kcal 頁 (6.jpg)
 *   ui.errorScanFailed       -> Uh-oh Scan Failed (7.jpg)
 *
 * onClose(): 使用者往下滑或按右上 X 時 -> nav.popBackStack()
 */
@Composable
fun WorkoutTrackerHost(
    vm: WorkoutViewModel,
    onClose: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // ✅ 這段是新的：當這個 Host 第一次被組進樹裡的時候，
    //    如果還沒載入 presets / today，就叫 vm.init()
    LaunchedEffect(Unit) {
        if (ui.presets.isEmpty() || ui.today == null) {
            vm.init()
        }
    }

    // 拿出 picker 方便 smart cast
    val picker = ui.showDurationPickerFor

    if (picker != null) {
        // 第二層：深色 "選分鐘數" sheet (2.jpg)
        DurationPickerSheet(
            presetName = picker.name,
            onSaveMinutes = { minutes ->
                vm.savePresetDuration(minutes) // /log -> 更新 today + toast
            },
            onCancel = {
                vm.dismissDialogs() // 關閉 picker，回到主單
            }
        )
    } else {
        // 第一層：白底 Workout Tracker 主單 (1.jpg)
        WorkoutTrackerSheet(
            uiState = ui,
            onClose = onClose,
            onTextChanged = vm::onTextChanged,
            onAddWorkout = { vm.estimate() }, // /estimate -> estimating dialog 等
            onClickPresetPlus = { preset ->
                vm.openDurationPicker(preset)
            },
            onToastCleared = { vm.clearToast() }
        )
    }

    // Loading (5.jpg)
    if (ui.estimating) {
        WorkoutEstimatingDialog(
            onDismiss = { /* loading 狀態下不讓用戶主動取消 */ }
        )
    }

    // 估算成功 (6.jpg)
    ui.estimateResult?.let { r ->
        WorkoutConfirmDialog(
            result = r,
            onSave = {
                vm.confirmSaveFromEstimate() // /log -> 更新 today + 清輸入
            },
            onCancel = { vm.dismissDialogs() }
        )
    }

    // Scan Failed (7.jpg)
    if (ui.errorScanFailed) {
        WorkoutScanFailedDialog(
            onTryAgain = { vm.dismissDialogs() },
            onCancel = { vm.dismissDialogs() }
        )
    }
}

