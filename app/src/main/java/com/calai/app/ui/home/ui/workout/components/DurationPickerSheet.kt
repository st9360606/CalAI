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
    onCancel: () -> Unit
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxSheetHeight = (screenHeightDp * 0.75f).dp

    // 這個 state 回到「預設」的行為：
    // - 往下拉 sheet -> 關
    // - 點外面灰色背景 -> 關
    // - 按 Cancel -> 我們自己呼叫 onCancel()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // 初始值：00 hr 30 min
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(30) }

    val rowItemHeight = 48.dp
    val visibleCount = 5
    val wheelAreaHeight = rowItemHeight * visibleCount

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = {
            // 系統偵測到「關」的動作（往下拉、點外面）時會呼叫這裡
            onCancel()
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,           // 白底
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        // 整個 sheet 最高只佔畫面大約 75%，避免撐太高
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
        ) {
            //
            // 上半：可捲動內容
            //
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 0.dp,        // 讓 presetName 靠近灰色手把
                        bottom = 168.dp    // 預留底部按鈕區高度，避免被覆蓋
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 標題 (Walking)
                Text(
                    text = presetName,
                    color = Color(0xFF111114),      // 幾乎黑
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                // 副標
                Text(
                    text = "Add this workout time to your activity log",
                    color = Color(0xFF6B7280),      // 灰
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(24.dp))

                // ===== 時間滾輪區塊 =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(wheelAreaHeight),
                    contentAlignment = Alignment.Center
                ) {
                    // 中央膠囊 (淡灰底，圓角)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(rowItemHeight)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF2F2F2)) // 很淡的灰
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 小時數字輪
                        ScrollingNumberWheel(
                            value = hours,
                            range = 0..12, // 你目前的上限
                            onValueChange = { hours = it },
                            textColor = Color(0xFF111114) // 黑字
                        )

                        Spacer(Modifier.width(8.dp))

                        // "hr" 固定
                        Text(
                            text = "hr",
                            color = Color(0xFF6B7280),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.width(24.dp))

                        // 分鐘數字輪
                        ScrollingNumberWheel(
                            value = minutes,
                            range = 0..59,
                            onValueChange = { minutes = it },
                            textColor = Color(0xFF111114)
                        )

                        Spacer(Modifier.width(8.dp))

                        // "min" 固定
                        Text(
                            text = "min",
                            color = Color(0xFF6B7280),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 額外空白讓版面有呼吸感
                Spacer(Modifier.height(42.dp))
            }

            //
            // 下半：固定在底部的按鈕們
            //
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 16.dp,
                        bottom = 32.dp    // 往上浮一點點
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Save（深色底、白字、圓角大 pill）
                Button(
                    onClick = {
                        val total = hours * 60 + minutes
                        if (total > 0) { // 不記錄 0 分鐘
                            onSaveMinutes(total)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111114), // 黑
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Cancel（淺灰底、黑字、圓角 pill）
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB), // 淺灰
                        contentColor = Color(0xFF111114)     // 黑字
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
