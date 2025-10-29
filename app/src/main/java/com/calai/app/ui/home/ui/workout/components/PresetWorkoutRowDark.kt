package com.calai.app.ui.home.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto

/**
 * 單一預設運動列 (像 1.jpg 的 Walking / Running)
 *
 * - 左：綠色圓圈 + 小圖示 (目前先放首字母，你之後可以改成真的走路/跑步 icon)
 * - 中：白色大字 (活動名稱) + 灰色小字 ("140 kcal per 30 min")
 * - 右：深灰圓形 + 白色「＋」
 */
@Composable
fun PresetWorkoutRowDark(
    preset: PresetWorkoutDto,
    onClickPlus: () -> Unit
) {
    val workoutName = preset.name
    // 這個欄位名稱依你的 DTO，假設是 kcalPer30Min
    val kcalText = "${preset.kcalPer30Min} kcal per 30 min"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 綠色圓圈 (左側圖示)
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color(0xFF65A30D) // 綠色，接近螢幕截圖
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 先用活動名稱第一個字母當 placeholder
                Text(
                    text = workoutName.trim().take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // 中間文字區
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = workoutName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
            Text(
                text = kcalText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF)
            )
        }

        // 右邊深灰圓形 + 白色「＋」
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClickPlus() },
            shape = CircleShape,
            color = Color(0xFF4B5563)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "add preset workout",
                    tint = Color.White
                )
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
