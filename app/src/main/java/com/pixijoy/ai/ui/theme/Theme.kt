package com.pixijoy.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PixiJoyDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentPink,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = PixiJoyBorder,
    error = ErrorRed,
    onError = Color.White
)

private val PixiJoyLightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentPink,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = BackgroundLight,
    onBackground = TextPrimaryOnLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryOnLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryOnLight,
    outline = PixiJoyBorderLight,
    error = ErrorRed,
    onError = Color.White
)

enum class PixiJoyThemeMode { Light, Dark, System }

@Composable
fun PixiJoyTheme(
    mode: PixiJoyThemeMode = PixiJoyThemeMode.Dark,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        PixiJoyThemeMode.Light -> false
        PixiJoyThemeMode.Dark -> true
        PixiJoyThemeMode.System -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) PixiJoyDarkColorScheme else PixiJoyLightColorScheme,
        typography = PixiJoyTypography,
        content = content
    )
}
