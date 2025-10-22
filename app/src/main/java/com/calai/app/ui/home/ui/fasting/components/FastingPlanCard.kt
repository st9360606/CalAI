package com.calai.app.ui.home.ui.fasting.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calai.app.ui.home.ui.fasting.model.FastingUiState


@Composable
fun FastingPlanCard(
    state: FastingUiState,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Fasting Plan", style = MaterialTheme.typography.titleLarge)
                Text(state.selected.code, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.padding(top = 8.dp)) {
                    Column(Modifier.padding(end = 16.dp)) {
                        Text("start time", style = MaterialTheme.typography.labelSmall)
                        Text("%02d:%02d".format(state.start.hour, state.start.minute))
                    }
                    Column {
                        Text("end time", style = MaterialTheme.typography.labelSmall)
                        Text("%02d:%02d".format(state.end.hour, state.end.minute))
                    }
                }
            }
            // 卡片上不直接切換，避免無權限狀態誤操作；開關在詳頁
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
    }
}
