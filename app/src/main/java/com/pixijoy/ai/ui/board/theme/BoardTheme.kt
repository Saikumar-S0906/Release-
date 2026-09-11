package com.pixijoy.ai.ui.board.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.ai.ui.theme.AccentPink
import com.pixijoy.ai.ui.theme.AccentPurple
import com.pixijoy.ai.ui.theme.BackgroundDark
import com.pixijoy.ai.ui.theme.BackgroundLight
import com.pixijoy.ai.ui.theme.BrandWarm
import com.pixijoy.ai.ui.theme.ErrorRed
import com.pixijoy.ai.ui.theme.PixiJoyBorder
import com.pixijoy.ai.ui.theme.PixiJoyBorderLight
import com.pixijoy.ai.ui.theme.PixiRadii
import com.pixijoy.ai.ui.theme.PrimaryGradient
import com.pixijoy.ai.ui.theme.SuccessGreen
import com.pixijoy.ai.ui.theme.SurfaceDark
import com.pixijoy.ai.ui.theme.SurfaceLight
import com.pixijoy.ai.ui.theme.SurfaceVariantDark
import com.pixijoy.ai.ui.theme.SurfaceVariantLight
import com.pixijoy.ai.ui.theme.TextPrimary
import com.pixijoy.ai.ui.theme.TextPrimaryOnLight
import com.pixijoy.ai.ui.theme.TextSecondary
import com.pixijoy.ai.ui.theme.TextSecondaryOnLight
import com.pixijoy.ai.ui.theme.WarningAmber

/**
 * Board-scoped colors — default **dark** brand (Identity + Design Bible §10).
 * Light tonal set via [BoardColors.light] (Phase 7 completes host pass).
 */
@Immutable
data class BoardColors(
    val primary: Color = AccentPurple,
    val onPrimary: Color = Color.White,
    val primarySoft: Color = Color(0x336848FF),
    val accentPink: Color = AccentPink,
    val accentYellow: Color = BrandWarm,
    val accentPurple: Color = AccentPurple,
    val background: Color = BackgroundDark,
    val surface: Color = SurfaceDark,
    val surfaceMuted: Color = SurfaceVariantDark,
    val border: Color = PixiJoyBorder,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val splashTop: Color = BackgroundDark,
    val splashMid: Color = Color(0xFF2E1065),
    val processingSurface: Color = BackgroundDark,
    val success: Color = SuccessGreen,
    val successSoft: Color = Color(0x2625B878),
    val warning: Color = WarningAmber,
    val danger: Color = ErrorRed,
    val info: Color = Color(0xFF22D3EE),
    /** AI CTA brush — purple→pink; use sparingly (Bible: no excessive gradients). */
    val aiGradient: Brush = PrimaryGradient
) {
    companion object {
        fun dark(): BoardColors = BoardColors()

        /** First-class light tonal set (Bible §10) — brand purple retained (Bible §11). */
        fun light(): BoardColors = BoardColors(
            primary = AccentPurple,
            onPrimary = Color.White,
            primarySoft = Color(0xFFE8E4FF),
            accentPink = AccentPink,
            accentYellow = BrandWarm,
            accentPurple = AccentPurple,
            background = BackgroundLight,
            surface = SurfaceLight,
            surfaceMuted = SurfaceVariantLight,
            border = PixiJoyBorderLight,
            textPrimary = TextPrimaryOnLight,
            textSecondary = TextSecondaryOnLight,
            splashTop = Color(0xFFF3F0FF),
            splashMid = Color(0xFFE8E4FF),
            processingSurface = SurfaceVariantLight,
            success = SuccessGreen,
            successSoft = Color(0x2625B878),
            warning = WarningAmber,
            danger = ErrorRed,
            info = Color(0xFF06B6D4),
            aiGradient = PrimaryGradient
        )
    }
}

@Immutable
data class BoardRadii(
    val sm: Dp = PixiRadii.sm,
    val md: Dp = PixiRadii.md,
    val lg: Dp = PixiRadii.lg,
    val card: Dp = PixiRadii.lg,
    val sheet: Dp = PixiRadii.sheet,
    val pill: Dp = PixiRadii.pill
)

/**
 * Default Compose face is platform sans (never crashes).
 * [BoardThemeProvider] uses [loadBoardFontFamily] (also sans — crash-safe on all devices).
 */
val BoardFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Design Bible §7–8 type scale. Prefer these over ad-hoc sizes.
 *
 * Weight: Regular body · Medium labels · SemiBold headings/actions · Bold hero only.
 */
object BoardType {
    val hero: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    )
    val largeHeadline: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    )
    val headline: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    )
    val title: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp
    )
    val body: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val secondary: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val caption: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val cta: TextStyle = TextStyle(
        fontFamily = BoardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
}

fun boardTypography(fontFamily: FontFamily = BoardFontFamily): Typography {
    val hero = BoardType.hero.copy(fontFamily = fontFamily)
    val largeHeadline = BoardType.largeHeadline.copy(fontFamily = fontFamily)
    val headline = BoardType.headline.copy(fontFamily = fontFamily)
    val title = BoardType.title.copy(fontFamily = fontFamily)
    val body = BoardType.body.copy(fontFamily = fontFamily)
    val secondary = BoardType.secondary.copy(fontFamily = fontFamily)
    val caption = BoardType.caption.copy(fontFamily = fontFamily)
    val cta = BoardType.cta.copy(fontFamily = fontFamily)
    return Typography(
        displayLarge = hero,
        displayMedium = largeHeadline,
        displaySmall = headline,
        headlineLarge = largeHeadline,
        headlineMedium = headline,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = body.copy(fontWeight = FontWeight.Medium),
        titleSmall = secondary.copy(fontWeight = FontWeight.Medium),
        bodyLarge = body,
        bodyMedium = secondary,
        bodySmall = caption.copy(fontWeight = FontWeight.Normal),
        labelLarge = cta,
        labelMedium = caption,
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}

val BoardTypography: Typography by lazy { boardTypography() }

val LocalBoardColors = staticCompositionLocalOf { BoardColors.dark() }
val LocalBoardRadii = staticCompositionLocalOf { BoardRadii() }
val LocalBoardTypography = staticCompositionLocalOf { boardTypography() }

object BoardTheme {
    val colors: BoardColors
        @Composable get() = LocalBoardColors.current
    val radii: BoardRadii
        @Composable get() = LocalBoardRadii.current
    val typography: Typography
        @Composable get() = LocalBoardTypography.current
}

fun boardDarkColorScheme(colors: BoardColors = BoardColors.dark()) = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    secondary = colors.accentPink,
    onSecondary = colors.onPrimary,
    tertiary = colors.accentPurple,
    onTertiary = colors.onPrimary,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceMuted,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.border,
    error = colors.danger,
    onError = colors.onPrimary
)

fun boardLightColorScheme(colors: BoardColors = BoardColors.light()) = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    secondary = colors.accentPink,
    onSecondary = colors.onPrimary,
    tertiary = colors.accentPurple,
    onTertiary = colors.onPrimary,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceMuted,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.border,
    error = colors.danger,
    onError = colors.onPrimary
)

@Composable
fun BoardThemeProvider(
    colors: BoardColors = BoardColors.dark(),
    radii: BoardRadii = BoardRadii(),
    darkTheme: Boolean = true,
    typography: Typography? = null,
    content: @Composable () -> Unit
) {
    val resolvedTypography = typography ?: remember {
        boardTypography(loadBoardFontFamily())
    }
    val scheme = if (darkTheme) boardDarkColorScheme(colors) else boardLightColorScheme(colors)
    CompositionLocalProvider(
        LocalBoardColors provides colors,
        LocalBoardRadii provides radii,
        LocalBoardTypography provides resolvedTypography
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = resolvedTypography,
            content = content
        )
    }
}
