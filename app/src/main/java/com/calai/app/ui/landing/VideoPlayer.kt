@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.calai.app.ui.landing

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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

/** 不建立任何音訊 renderer 的 RenderersFactory（解 emulator 音訊 I/O 錯誤） */
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
        // 不加入音訊 renderer → 完全不觸發音訊初始化
    }
}

@Composable
fun LoopVideo(
    modifier: Modifier = Modifier,
    @RawRes resId: Int,
    cornerRadiusDp: Int = 24
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    // 先讓首屏畫出來，再初始化播放器
    var showVideo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { _: Long -> }   // 明確標型別，避免 "Cannot infer type"
        // 如果想更穩可以再加：kotlinx.coroutines.delay(120)
        showVideo = true
    }

    if (!showVideo) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color(0xFFF2F2F2))
        )
        return
    }

    // 使用不含音訊的 RenderersFactory（相容所有 Media3 版本）
    val renderersFactory = remember(context) { NoAudioRenderersFactory(context) }

    val mediaUri = remember(resId) {
        Uri.parse("android.resource://${context.packageName}/$resId")
    }

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
                player = exo
            }
        }
    )

    DisposableEffect(Unit) { onDispose { exo.release() } }
}
