// app/src/main/java/com/calai/app/ui/landing/LandingVideo.kt
@file:OptIn(androidx.media3.common.util.UnstableApi::class)
@file:Suppress("UnsafeOptInUsageError")

package com.calai.app.ui.landing

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.withFrameNanos

/** 在模擬器避免建立音訊 Renderer，縮短初始化並避免雜訊 */
@UnstableApi
private class NoAudioRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        // 不加入任何音訊 renderer
    }
}

/** 影片容器（先畫佔位 -> 第一幀後才建 ExoPlayer） */
@Composable
fun LandingVideo(
    modifier: Modifier,
    @RawRes resId: Int,
    cornerDp: Int = 28,
    placeholderColor: Color = Color(0xFFF2F2F2)
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerDp.dp)

    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 等待第一幀（避免阻塞啟動畫面）
        withFrameNanos { /* no-op */ }
        ready = true
    }

    if (!ready) {
        Box(modifier = modifier.clip(shape).background(placeholderColor))
        return
    }

    val renderersFactory = remember(context) { NoAudioRenderersFactory(context) }
    val mediaUri = remember(resId) {
        Uri.parse("android.resource://${context.packageName}/$resId")
    }

    // 建立與釋放 ExoPlayer
    val exo = remember(renderersFactory, mediaUri) {
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build().apply {
                setMediaItem(MediaItem.fromUri(mediaUri))
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
    }

    AndroidView(
        modifier = modifier.clip(shape),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = exo  // ← 關鍵：明確指定 PlayerView.player
            }
        }
    )

    DisposableEffect(exo) {
        onDispose { exo.release() }
    }
}
