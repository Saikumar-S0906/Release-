package com.pixijoy.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Product type roles. Uses SansSerif so the designsystem stays self-contained;
 * Noto Sans downloadable fonts in the app module optical-match on devices that resolve them.
 */
val PixiFontFamily: FontFamily = FontFamily.SansSerif

val PixiDisplay = TextStyle(
    fontFamily = PixiFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.3).sp
)

val PixiTitle = TextStyle(
    fontFamily = PixiFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.15).sp
)

val PixiLabel = TextStyle(
    fontFamily = PixiFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp
)

/** Large live slider / meter value. */
val PixiValue = TextStyle(
    fontFamily = PixiFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

val PixiHint = TextStyle(
    fontFamily = PixiFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.15.sp
)

val PixiTypography = Typography(
    displayLarge = PixiDisplay.copy(fontSize = 34.sp, lineHeight = 40.sp),
    displayMedium = PixiDisplay,
    displaySmall = PixiDisplay.copy(fontSize = 24.sp, lineHeight = 30.sp),
    headlineLarge = PixiTitle.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = PixiTitle.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = PixiTitle,
    titleLarge = PixiTitle.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = PixiLabel.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = PixiLabel.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(
        fontFamily = PixiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PixiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = PixiHint,
    labelLarge = PixiLabel.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = PixiLabel,
    labelSmall = PixiHint.copy(fontWeight = FontWeight.Medium)
)
