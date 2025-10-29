package com.calai.app.ui.home.ui.workout.components

import androidx.compose.foundation.background
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

