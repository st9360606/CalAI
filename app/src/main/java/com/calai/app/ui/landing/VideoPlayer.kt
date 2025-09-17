package com.calai.app.ui.landing

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun LoopVideo(modifier: Modifier = Modifier, @RawRes resId: Int) {
    val context = LocalContext.current
    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.fromUri("android.resource://${context.packageName}/$resId")
            setMediaItem(item)
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(24)),
        factory = { PlayerView(it).apply { useController = false; player = exo } }
    )
    DisposableEffect(Unit) { onDispose { exo.release() } }
}
