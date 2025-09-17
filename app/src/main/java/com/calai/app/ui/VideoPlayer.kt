package com.calai.app.ui

import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun VideoPlayerCore(
    modifier: Modifier = Modifier,
    mute: Boolean = true,
    repeat: Boolean = true,
    autoPlay: Boolean = true,
    rounded: Dp = 24.dp,              // ← 用 Dp
    buildMediaItem: () -> MediaItem,  // ← 放最後，尾隨 lambda
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(buildMediaItem())
            if (repeat) repeatMode = Player.REPEAT_MODE_ALL
            volume = if (mute) 0f else 1f
            prepare()
            playWhenReady = autoPlay
        }
    }

    DisposableEffect(lifecycle, exo) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (autoPlay) exo.play()
                Lifecycle.Event.ON_PAUSE  -> exo.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            exo.release()
        }
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(rounded)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = exo
            }
        }
    )
}

/** 播 res/raw 的本地影片（離線） */
@Composable
fun VideoPlayerRaw(
    @RawRes resId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    VideoPlayerCore(modifier = modifier) {
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        MediaItem.fromUri(uri)
    }
}

/** 播線上影片（CDN/URL） */
@Composable
fun VideoPlayerUrl(
    url: String,
    modifier: Modifier = Modifier
) {
    VideoPlayerCore(modifier = modifier) {
        MediaItem.fromUri(url)
    }
}
