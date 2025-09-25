// app/src/main/java/com/calai/app/ui/landing/LandingVideo.kt
package com.calai.app.ui.landing

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex

@Composable
fun LandingVideo(
    modifier: Modifier,
    @RawRes resId: Int,
    posterResId: Int? = null,               // 有海報就顯示，否則用純白
    placeholderColor: Color = Color.White
) {
    // 先顯示佔位層；等第一幀渲染好了再移除
    var placeholderVisible by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        // 1) 影片（在下層）
        VideoPlayerRaw(
            resId = resId,
            modifier = Modifier.fillMaxSize().zIndex(0f),
            onFirstFrame = { placeholderVisible = false } // 收到首幀才拿掉佔位，避免黑閃
        )

        // 2) 佔位層（在上層）
        if (placeholderVisible) {
            if (posterResId != null) {
                Image(
                    painter = painterResource(posterResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor)
                        .zIndex(1f)
                )
            }
        }
    }
}
