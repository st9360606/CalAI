package com.calai.app.ui.home.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.data.workout.api.EstimateResponse
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.calai.app.ui.home.ui.workout.model.WorkoutUiState

private val DarkSurface = Color(0xFF111114)
private val Green = Color(0xFF4CAF50)
private val GrayBtn = Color(0xFF374151)

private enum class FlowMode { Estimating, Result, Failed }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFlowSheet(
    sheetState: SheetState,
    ui: WorkoutUiState,
    onSave: () -> Unit,
    onTryAgain: () -> Unit,
    onCancelAll: () -> Unit
) {
    if (!(ui.estimating || ui.estimateResult != null || ui.errorScanFailed)) return

    val h = trackerSheetHeight() // ← 來自 SheetSpec.kt 的單一定義

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { /* lock during flow */ },
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = DarkSurface,
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(h)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            val mode = when {
                ui.estimating -> FlowMode.Estimating
                ui.estimateResult != null -> FlowMode.Result
                else -> FlowMode.Failed
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(0)) togetherWith fadeOut(tween(0)) },
                modifier = Modifier.fillMaxSize()
            ) { m ->
                when (m) {
                    FlowMode.Estimating -> EstimatingBody()
                    FlowMode.Result     -> ResultBody(ui.estimateResult!!, onSave, onCancelAll)
                    FlowMode.Failed     -> FailedBody(onTryAgain, onCancelAll)
                }
            }
        }
    }
}


@Composable private fun EstimatingBody() {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Green)
        )
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Estimating effort, calculating calories...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Please do not close the app or lock your device", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun ResultBody(result: EstimateResponse, onSave: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(Green))
            Spacer(Modifier.height(24.dp))
            Text("${result.minutes ?: 0} min ${result.activityDisplay.orEmpty()}", color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("${result.kcal ?: 0} kcal", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkSurface)
            ){ Text("Save Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayBtn, contentColor = Color.White)
            ){ Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable private fun FailedBody(onTryAgain: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
            Spacer(Modifier.height(16.dp))
            Text("Uh-oh! Scan Failed", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("The activity description may be incorrect, or the internet connection is weak.",
                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Button(onClick = onTryAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkSurface)
            ){ Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayBtn, contentColor = Color.White)
            ){ Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        }
    }
}
