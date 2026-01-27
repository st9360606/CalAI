package com.calai.bitecal.ui.home.ui.workout.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp

/**
 * 一個固定在底部、完全不跟隨鍵盤位移的「自製」Modal Sheet」。
 * - 以 Dialog 呈現（usePlatformDefaultWidth=false, decorFitsSystemWindows=false）
 * - panel 放在**最後一個參數**，讓尾隨 lambda 自動對到 panel。
 */
@Composable
fun FixedModalSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    scrimColor: Color = Color(0x73000000),
    onScrimClick: (() -> Unit)? = onDismissRequest,
    panel: @Composable BoxScope.() -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .let {
                        if (onScrimClick != null) it.clickable { onScrimClick.invoke() } else it
                    }
            )

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = true,
                enter = slideInVertically(animationSpec = tween(180)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(tween(120))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White, // ✅ 改成純白底
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp
                ) {
                    panel()
                }
            }
        }
    }
}

