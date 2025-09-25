package com.calai.app.ui.landing

import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.calai.app.R

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerRaw(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    autoPlay: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM, // cover
    onFirstFrame: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // 單例 Player（依 resId 記住，避免重建）
    val player = remember(ctx, resId) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(resId)))
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            playWhenReady = autoPlay
            setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        }
    }

    // 首幀回呼：同時監聽舊/新事件，雙保險
    DisposableEffect(player, onFirstFrame) {
        val listener = object : Player.Listener {
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onRenderedFirstFrame() {
                onFirstFrame?.invoke()
            }
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                    onFirstFrame?.invoke()
                }
            }
        }
        player.addListener(listener)
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // 跟隨生命週期暫停/恢復（不重建、不黑屏）
    DisposableEffect(lifecycle, player) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> if (autoPlay) player.play()
                Lifecycle.Event.ON_STOP  -> player.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    AndroidView(
        modifier = modifier,
        factory = { base ->
            // 用 style 指定 TextureView（避免 SurfaceView 綠線/黑屏）
            val themed = ContextThemeWrapper(base, R.style.PlayerTextureView)
            PlayerView(themed).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setShutterBackgroundColor(Color.TRANSPARENT)
                setBackgroundColor(Color.TRANSPARENT)
                setKeepContentOnPlayerReset(true)
                setResizeMode(resizeMode)
                setPlayer(player)
            }
        },
        update = { view ->
            view.setResizeMode(resizeMode)
            if (view.player !== player) view.setPlayer(player)
        }
    )
}
