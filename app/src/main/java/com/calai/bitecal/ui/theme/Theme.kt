package com.calai.bitecal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.calai.bitecal.ui.common.design.BiteCalColors

private val DarkColorScheme = darkColorScheme(
    primary = BiteCalColors.Dark.primaryButtonContainer,
    onPrimary = BiteCalColors.Dark.primaryButtonContent,
    secondary = BiteCalColors.Dark.textSecondary,
    onSecondary = BiteCalColors.Dark.textPrimary,
    background = BiteCalColors.Dark.background,
    onBackground = BiteCalColors.Dark.textPrimary,
    surface = BiteCalColors.Dark.surface,
    onSurface = BiteCalColors.Dark.textPrimary,
    surfaceVariant = BiteCalColors.Dark.surfaceMuted,
    onSurfaceVariant = BiteCalColors.Dark.textSecondary,
    outline = BiteCalColors.Dark.border,
    error = BiteCalColors.Dark.error,
)

private val LightColorScheme = lightColorScheme(
    primary = BiteCalColors.Light.primaryButtonContainer,
    onPrimary = BiteCalColors.Light.primaryButtonContent,
    secondary = BiteCalColors.Light.textSecondary,
    onSecondary = BiteCalColors.Light.textPrimary,
    background = BiteCalColors.Light.background,
    onBackground = BiteCalColors.Light.textPrimary,
    surface = BiteCalColors.Light.surface,
    onSurface = BiteCalColors.Light.textPrimary,
    surfaceVariant = BiteCalColors.Light.surfaceMuted,
    onSurfaceVariant = BiteCalColors.Light.textSecondary,
    outline = BiteCalColors.Light.border,
    error = BiteCalColors.Light.error,
)

@Composable
fun CalAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
