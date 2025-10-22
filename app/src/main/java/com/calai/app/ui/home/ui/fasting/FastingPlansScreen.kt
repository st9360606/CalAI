package com.calai.app.ui.home.ui.fasting

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calai.app.R
import com.calai.app.data.fasting.model.FastingPlan
import com.calai.app.ui.home.ui.fasting.model.FastingPlanViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingPlansScreen(
    vm: FastingPlanViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // ✅ 只有當 Composition 有 ActivityResultRegistryOwner 時才建立 launcher
    val registryOwner = LocalActivityResultRegistryOwner.current
    val notifPermissionLauncher =
        if (registryOwner != null)
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    vm.onToggleEnabled(true, onNeedPermission = {}, onDenied = {})
                } else {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.need_notification_permission)) }
                }
            }
        else
            null

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fasting_plans)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            // 計畫清單
            LazyColumn {
                items(FastingPlan.entries) { plan ->
                    PlanRow(
                        plan = plan,
                        selected = plan == state.selected,
                        onSelect = { vm.onPlanSelected(plan) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Start time（可修改）
            Text(text = stringResource(R.string.start_time), style = MaterialTheme.typography.labelSmall)
            OutlinedButton(onClick = {
                TimePickerDialog(
                    ctx,
                    { _, h, m -> vm.onChangeStart(LocalTime.of(h, m)) },
                    state.start.hour,
                    state.start.minute,
                    /* is24Hour = */ true
                ).show()
            }) {
                Text("%02d:%02d".format(state.start.hour, state.start.minute))
            }

            Spacer(Modifier.height(8.dp))

            // End time（僅顯示）
            Text(text = stringResource(R.string.end_time), style = MaterialTheme.typography.labelSmall)
            OutlinedButton(onClick = { /* disabled */ }, enabled = false) {
                Text("%02d:%02d".format(state.end.hour, state.end.minute))
            }

            Spacer(Modifier.height(16.dp))

            // 開關（先請權限，拒絕維持 OFF + Snackbar）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.enable_reminders), Modifier.weight(1f))
                Switch(
                    checked = state.enabled,
                    onCheckedChange = { on ->
                        vm.onToggleEnabled(
                            requested = on,
                            onNeedPermission = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    // ✅ 有 launcher 就 runtime 請求；否則降級為 Snackbar 提示
                                    notifPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        ?: scope.launch {
                                            snackbar.showSnackbar(ctx.getString(R.string.need_notification_permission))
                                        }
                                }
                            },
                            onDenied = {
                                scope.launch { snackbar.showSnackbar(ctx.getString(R.string.need_notification_permission)) }
                            }
                        )
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save：會 upsert 並以 /next-triggers 的 UTC 排程（DST 安全）
            Button(
                onClick = { vm.persistAndReschedule(); onBack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.fasting_plan_save)) }
        }
    }
}

@Composable
private fun PlanRow(plan: FastingPlan, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(plan.code, style = MaterialTheme.typography.titleLarge)
            Text("${plan.eatingHours}h eating window", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
