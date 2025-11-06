package com.calai.app.ui.home.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.ui.home.ui.workout.model.WorkoutUiState
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import com.calai.app.ui.home.ui.workout.components.WorkoutFlowSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet

private enum class WhiteMode { Tracker, Duration }

// 顏色
private val Black = Color(0xFF111114)
private val LightGrayBg = Color(0xFFF3F4F6)
private val Gray300 = Color(0xFFE5E7EB)
private val Gray500 = Color(0xFF6B7280)
private val Gray600 = Color(0xFF4B5563)
private val DividerGray = Color(0xFFD1D5DB)
private val TextPrimary = Color(0xFF111114)
private val TextSecondary = Color(0xFF4B5563)
private val HandleGray = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerHost(
    vm: WorkoutViewModel,
    onClose: () -> Unit,
    sheetState: SheetState,
    visible: Boolean
) {
    val ui = vm.ui.collectAsStateWithLifecycle().value
    LaunchedEffect(Unit) { if (ui.presets.isEmpty() || ui.today == null) vm.init() }

    val flowSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val inFlow = ui.estimating || ui.estimateResult != null || ui.errorScanFailed

    // ✅ 一偵測到 saving 就立刻把白色面板收掉（避免任何回彈一幀）
    LaunchedEffect(ui.saving) {
        if (ui.saving) onClose()
    }

    val shouldShowWhite =
        visible && !ui.saving && !inFlow

    if (shouldShowWhite) {
        val picker = ui.showDurationPickerFor
        if (picker != null) {
            DurationPickerSheet(
                presetName = picker.name,
                onSaveMinutes = { minutes -> vm.savePresetDuration(minutes) },
                onCancel = { vm.dismissDialogs() },
                sheetState = sheetState
            )
        } else {
            WorkoutTrackerSheet(
                uiState = ui,
                onClose = onClose,
                onTextChanged = vm::onTextChanged,
                onAddWorkout = { vm.estimateWithSpinner() },
                onClickPresetPlus = { preset -> vm.openDurationPicker(preset) },
                onToastCleared = { vm.clearToast() },
                sheetState = sheetState
            )
        }
    }

    if (inFlow) {
        WorkoutFlowSheet(
            sheetState = flowSheetState,
            ui = ui,
            onSave = { vm.confirmSaveFromEstimate() },
            onTryAgain = { vm.dismissDialogs() },
            onCancelAll = { vm.dismissDialogs() }
        )
    }
}


/* ------------------------- 以下是白色內容（不要再建 Sheet） ------------------------- */

@Composable
private fun TrackerContent(
    uiState: WorkoutUiState,
    onClose: () -> Unit,
    onTextChanged: (String) -> Unit,
    onAddWorkout: () -> Unit,
    onClickPresetPlus: (PresetWorkoutDto) -> Unit
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad: Dp = navBottom + 12.dp

    var expanded by rememberSaveable { mutableStateOf(false) }
    val initialLimit = 20
    val totalCount = uiState.presets.size
    val presetsToShow = if (expanded) uiState.presets else uiState.presets.take(initialLimit)
    val remaining = (totalCount - initialLimit).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPad)
    ) {
        item {
            HeaderSection(
                title = "Workout Tracker",
                subtitle = "Describe the Type of Exercise and the duration",
                onClose = onClose
            )

            OutlinedTextField(
                value = uiState.textInput,
                onValueChange = { onTextChanged(it) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("Examples: 45 min Running", color = Gray500) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = TextPrimary,
                    focusedContainerColor = LightGrayBg,
                    unfocusedContainerColor = LightGrayBg,
                    disabledContainerColor = LightGrayBg,
                    focusedBorderColor = Gray300,
                    unfocusedBorderColor = Gray300,
                    focusedPlaceholderColor = Gray500,
                    unfocusedPlaceholderColor = Gray500
                )
            )

            Spacer(Modifier.height(20.dp))

            val isEnabled = uiState.textInput.isNotBlank()
            Button(
                onClick = onAddWorkout,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black, contentColor = Color.White,
                    disabledContainerColor = Black, disabledContentColor = Color.White
                )
            ) { Text("Add Workout", color = Color.White) }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerGray)
                Text(
                    text = "or select from the list",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray600
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerGray)
            }

            Spacer(Modifier.height(16.dp))
        }

        items(presetsToShow) { preset ->
            PresetWorkoutRowLight(
                preset = preset,
                onClickPlus = { onClickPresetPlus(preset) }
            )
            HorizontalDivider(color = Gray300)
        }

        if (totalCount > initialLimit) {
            item {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { expanded = !expanded }) {
                        val label = if (expanded) "Show less" else "Show more ($remaining)"
                        Text(text = label, color = Black, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp).height(4.dp)
                .background(color = HandleGray.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary, textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "close", tint = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PresetWorkoutRowLight(
    preset: PresetWorkoutDto,
    onClickPlus: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF84CC16)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("${preset.kcalPer30Min} kcal per 30 min", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClickPlus() },
            contentAlignment = Alignment.Center
        ) { Icon(imageVector = Icons.Filled.Add, contentDescription = "add preset", tint = Color.White) }
    }
}
