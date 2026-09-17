package com.pixijoy.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val PixiBg = Color(0xFF0B0B10)
val PixiSurface = Color(0xFF15151C)
val PixiSurface2 = Color(0xFF20202A)
val PixiText = Color(0xFFFFFFFF)
val PixiTextMuted = Color(0xFFB7B7C7)
val PixiBorder = Color(0xFF30303A)
val PixiAccent = Color(0xFF6C4DFF)
val PixiPink = Color(0xFFFF5AA5)
val PixiSuccess = Color(0xFF25B878)
val PixiError = Color(0xFFE94D63)

private val DarkScheme = darkColorScheme(
    primary = PixiAccent,
    onPrimary = Color.White,
    secondary = PixiPink,
    background = PixiBg,
    onBackground = PixiText,
    surface = PixiSurface,
    onSurface = PixiText,
    surfaceVariant = PixiSurface2,
    onSurfaceVariant = PixiTextMuted,
    outline = PixiBorder,
    error = PixiError
)

private val LightScheme = lightColorScheme(
    primary = PixiAccent,
    onPrimary = Color.White,
    secondary = PixiPink,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF11111A),
    surface = Color.White,
    onSurface = Color(0xFF11111A),
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF646477),
    outline = Color(0xFFE2E2EA),
    error = PixiError
)

val PixiCardShape = RoundedCornerShape(20.dp)
val PixiPillShape = RoundedCornerShape(999.dp)

@Composable
fun PixiTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content
        )
    }
}

private val LocalDark = staticCompositionLocalOf { true }
