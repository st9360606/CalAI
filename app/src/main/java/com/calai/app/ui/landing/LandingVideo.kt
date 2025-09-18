package com.calai.app.ui.landing

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.calai.app.ui.VideoPlayerRaw

/**
 * 首幀後再載入影片：
 * - 先讓畫面完成第一幀，避免與啟動期爭資源造成卡頓
 * - 之後再顯示實際播放器；在此檔不直接觸碰 Media3，可避開 UnstableApi Lint
 */
@Composable
fun LandingVideo(
    modifier: Modifier,
    @RawRes resId: Int
) {
    var showVideo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 等第一幀繪製完成，再顯示播放器
        withFrameNanos { /* no-op */ }
        showVideo = true
    }

    if (showVideo) {
        // 你的現有播放器（內部才會使用 Media3）
        VideoPlayerRaw(resId = resId, modifier = modifier)
    } else {
        // 先畫與影片相同尺寸/圓角的佔位，避免版面跳動
        Box(modifier = modifier.background(Color(0xFFF2F2F2)))
    }
}
