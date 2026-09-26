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

/** Near-black canvas — not pure black, so glass and elev gains depth. */
val PixiBg = Color(0xFF101014)
val PixiSurface = Color(0xFF17171E)
val PixiSurface2 = Color(0xFF22222C)
/** Raised plates / sheets over surface. */
val PixiElevated = Color(0xFF2A2A36)
/** Scrim wash for chrome over media. */
val PixiScrim = Color(0xCC0C0C10)
val PixiText = Color(0xFFFFFFFF)
val PixiTextMuted = Color(0xFFA8A8B8)
/** Hairline outline (~14% white) for quiet chips. */
val PixiBorder = Color(0x24FFFFFF)
/** Brand accent — quieter than the old saturated fill. */
val PixiAccent = Color(0xFF7B6CFF)
/** Soft selected fill (chips, segments, nav). */
val PixiAccentSoft = Color(0x337B6CFF)
/** Muted accent for underlines / secondary state. */
val PixiAccentMuted = Color(0xFF9B90FF)
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
val PixiControlShape = RoundedCornerShape(12.dp)

@Composable
fun PixiTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = PixiTypography,
            content = content
        )
    }
}

private val LocalDark = staticCompositionLocalOf { true }
