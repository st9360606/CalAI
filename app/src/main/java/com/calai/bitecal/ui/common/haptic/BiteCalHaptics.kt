package com.calai.bitecal.ui.common.haptic

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/**
 * BiteCal shared haptic gateway.
 *
 * Design rules:
 * - click(): only call from explicit actions such as Button/IconButton/clickable rows.
 * - wheelTick(): call when a wheel picker crosses/snaps to a new centered value.
 * - Uses View.performHapticFeedback(), so Android respects the user's system haptic setting.
 * - Does not require VIBRATE permission.
 */
class BiteCalHaptics internal constructor(
    private val view: View
) {
    fun click() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun wheelTick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
fun rememberBiteCalHaptics(): BiteCalHaptics {
    val view = LocalView.current
    return remember(view) { BiteCalHaptics(view) }
}

@Composable
fun rememberClickWithHaptic(
    enabled: Boolean = true,
    onClick: () -> Unit
): () -> Unit {
    val haptics = rememberBiteCalHaptics()
    val latestOnClick by rememberUpdatedState(onClick)

    return remember(haptics, enabled) {
        {
            if (enabled) {
                haptics.click()
                latestOnClick()
            }
        }
    }
}

@Composable
fun Modifier.biteCalClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier {
    val wrappedClick = rememberClickWithHaptic(enabled = enabled, onClick = onClick)

    return if (interactionSource != null || indication != null) {
        val safeInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
        clickable(
            interactionSource = safeInteractionSource,
            indication = indication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = wrappedClick
        )
    } else {
        clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = wrappedClick
        )
    }
}

@Composable
fun Modifier.clickWithoutHaptic(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier {
    return if (interactionSource != null || indication != null) {
        val safeInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
        clickable(
            interactionSource = safeInteractionSource,
            indication = indication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
    } else {
        clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
    }
}

@Composable
fun Modifier.consumeClickWithoutHaptic(): Modifier {
    return clickWithoutHaptic(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {}
    )
}

@Composable
fun HapticWheelTickEffect(
    tickKey: Any?,
    enabled: Boolean = true
) {
    val haptics = rememberBiteCalHaptics()
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(tickKey, enabled) {
        if (!enabled) {
            initialized = false
            return@LaunchedEffect
        }

        if (initialized) {
            haptics.wheelTick()
        } else {
            initialized = true
        }
    }
}
