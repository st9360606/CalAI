package com.calai.app.ui.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 頂部置中的白色膠囊成功提示（綠色勾勾），可透過參數調整尺寸。
 * ※ 2 秒後請於呼叫端自行 clear。
 */
@Composable
fun SuccessTopToast(
    message: String,
    modifier: Modifier = Modifier,
    // 🔧 在這裡控制膠囊尺寸（預設已經是「中小型」）
    minWidth: Dp = 240.dp,
    minHeight: Dp = 30.dp
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topInset + 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = minWidth)   // ⬅️ 用參數
                .heightIn(min = minHeight),// ⬅️ 用參數
            shape = MaterialTheme.shapes.large, // 膠囊
            color = Color.White,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF84CC16)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy( // ✅ 文字更大
                        fontWeight = FontWeight.SemiBold,            // ✅ 文字加粗
                        color = Color(0xFF111114)
                    )
                )
            }
        }
    }
}
