package com.pixijoy.ai.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// PixiJoy Assets All-30-Flows design_tokens.json + locked surfaces

val BackgroundDark = Color(0xFF0B0B10)
val SurfaceDark = Color(0xFF15151C)
val SurfaceVariantDark = Color(0xFF20202A)
val PixiJoyBorder = Color(0xFF30303A)

val BackgroundLight = Color(0xFFFAFAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0F0F5)
val PixiJoyBorderLight = Color(0xFFE2E2EA)

val AccentPurple = Color(0xFF6C4DFF)
val AccentPink = Color(0xFFFF5AA5)
val AccentCyan = Color(0xFF22D3EE)
val BrandWarm = Color(0xFFFFB52E)

val BrandPink = AccentPink
val BrandOrange = BrandWarm
val BrandCoral = Color(0xFFFF5E7E)
val TealAccent = Color(0xFF14B8A6)
val TealDark = Color(0xFF0D9488)
val ActionBlue = Color(0xFF3B82F6)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB7B7C7)
val TextPrimaryOnLight = Color(0xFF11111A)
val TextSecondaryOnLight = Color(0xFF646477)

val SuccessGreen = Color(0xFF25B878)
val WarningAmber = Color(0xFFF4B740)
val ErrorRed = Color(0xFFE94D63)
val WhatsAppGreen = Color(0xFF25D366)

// Legacy aliases used by meme / creative screens
val MemeBackground = BackgroundDark
val MemeSurface = SurfaceDark
val TextPrimaryLight = TextPrimaryOnLight
val TextSecondaryLight = TextSecondaryOnLight
val TextTertiaryLight = Color(0xFF8A8A9A)
val CyanAccent = Color(0xFF06B6D4)
val YellowAccent = Color(0xFFFFE66D)

val PrimaryGradient = Brush.linearGradient(listOf(AccentPurple, AccentPink))
val AccentGradient = PrimaryGradient
val TealGradient = Brush.linearGradient(listOf(TealAccent, TealDark))
val SunsetGradient = PrimaryGradient
