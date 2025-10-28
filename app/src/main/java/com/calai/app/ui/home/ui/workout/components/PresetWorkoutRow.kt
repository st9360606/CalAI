package com.calai.app.ui.home.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto

@Composable
fun PresetWorkoutRow(
    preset: PresetWorkoutDto,
    onClickPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF111114),
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "${preset.kcalPer30Min} kcal per 30 min",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }

        // 右側圓形 + 處理 (和 1.jpg 相同概念)
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clickable { onClickPlus() },
            shape = CircleShape,
            color = Color(0xFFE5E7EB),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("+", color = Color(0xFF111114), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WorkoutEstimatingDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = null,
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 這裡可以放一個圓形進度 + "Estimating effort, calculating calories..."
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Estimating effort, calculating calories...",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Please do not close the app or lock your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        containerColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WorkoutConfirmDialog(
    result: EstimateResponse,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 大綠圓icon（6.jpg），這裡用簡化 block
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF4CAF50), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏃", color = Color.White)
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "${result.minutes ?: 0} min ${result.activityDisplay ?: ""}",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${result.kcal ?: 0} kcal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Save Activity") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
        containerColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WorkoutScanFailedDialog(
    onTryAgain: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "Uh-oh! Scan Failed",
                color = Color(0xFF111114),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Here's what might've happened: the activity description might be incorrectly provided, or there's a weak or no internet connection.",
                color = Color(0xFF111114)
            )
        },
        confirmButton = {
            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111114),
                    contentColor = Color.White
                )
            ) { Text("Try Again") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF111114)) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DurationPickerSheet(
    presetName: String,
    onSave: (minutes: Int) -> Unit,
    onCancel: () -> Unit
) {
    // 這裡用簡化版：讓使用者直接挑「幾分鐘」。
    // 你可以依照 2.jpg 改成雙滾輪 (hour/min)。
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("$presetName", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select the duration to log this activity")
                Spacer(Modifier.height(12.dp))
                // 這裡先用固定 30 分鐘做示範
                Text("30 min", style = MaterialTheme.typography.headlineMedium)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(30) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111114),
                    contentColor = Color.White
                )
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF111114)) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
