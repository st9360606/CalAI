package com.calai.app.ui.home.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.ui.home.components.ScrollingNumberWheel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerSheet(
    presetName: String,
    onSaveMinutes: (Int) -> Unit,
    onCancel: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxSheetHeight = (screenHeightDp * 0.75f).dp

    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(30) }

    val rowItemHeight = 48.dp
    val visibleCount = 5
    val wheelAreaHeight = rowItemHeight * visibleCount

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onCancel() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 168.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = presetName,
                    color = Color(0xFF111114),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Add this workout time to your activity log",
                    color = Color(0xFF6B7280),
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(wheelAreaHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(rowItemHeight)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF2F2F2))
                    )
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollingNumberWheel(
                            value = hours,
                            range = 0..12,
                            onValueChange = { hours = it },
                            textColor = Color(0xFF111114)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("hr", color = Color(0xFF6B7280), fontSize = 19.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(24.dp))
                        ScrollingNumberWheel(
                            value = minutes,
                            range = 0..59,
                            onValueChange = { minutes = it },
                            textColor = Color(0xFF111114)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("min", color = Color(0xFF6B7280), fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(42.dp))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val total = hours * 60 + minutes
                        if (total > 0) {
                            // ✅ 立刻進入 saving（由 VM 設定），不呼叫 hide()，交給上層 gating/導航處理
                            onSaveMinutes(total)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111114),
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB),
                        contentColor = Color(0xFF111114)
                    )
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

