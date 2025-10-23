package com.calai.app.ui.home.ui.fasting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.app.data.fasting.model.FastingPlan
import com.calai.app.ui.home.ui.fasting.model.FastingPlanViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingPlansScreen(
    vm: FastingPlanViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()

    // 第一次進入頁面時（新用戶會建立預設）
    LaunchedEffect(Unit) { if (state.loading) vm.load() }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        val st = rememberTimePickerState(state.start.hour, state.start.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.onChangeStart(LocalTime.of(st.hour, st.minute))
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ showStartPicker = false }) { Text("Cancel") } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start time", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TimePicker(state = st)
                }
            }
        )
    }

    if (showEndPicker) {
        val et = rememberTimePickerState(state.end.hour, state.end.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.onChangeEnd(LocalTime.of(et.hour, et.minute))
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ showEndPicker = false }) { Text("Cancel") } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("End time", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TimePicker(state = et)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fasting Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {  // ← 只返回，不保存
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 0.dp) {
                Button(
                    onClick = {
                        // 先回 HOME（VM 作用域仍是 HOME 的 entry，所以能安全持續工作）
                        onBack()
                        // 再啟動保存與排程（避免 Composable 正在收攤導致閃退）
                        vm.persistAndReschedule()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("儲存", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("選擇禁食計畫", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FastingPlan.entries) { plan ->
                    PlanRow(
                        plan = plan,
                        selected = plan == state.selected,
                        onSelect = { vm.onPlanSelected(plan) }
                    )
                }
            }

            Text("開始時間", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showStartPicker = true }, shape = MaterialTheme.shapes.large) {
                Text("%02d:%02d".format(state.start.hour, state.start.minute),
                    style = MaterialTheme.typography.titleLarge)
            }

            Text("結束時間", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showEndPicker = true }, shape = MaterialTheme.shapes.large) {
                Text("%02d:%02d".format(state.end.hour, state.end.minute),
                    style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}


@Composable
private fun PlanRow(plan: FastingPlan, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        onClick = onSelect
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(plan.code, style = MaterialTheme.typography.titleLarge)
            Text(
                "${plan.fastingHours}h fasting • ${plan.eatingHours}h eating",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
