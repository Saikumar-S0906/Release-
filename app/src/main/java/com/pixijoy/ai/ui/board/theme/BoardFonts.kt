package com.pixijoy.ai.ui.board.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Crash-safe board typeface for Compose.
 *
 * Always returns [FontFamily.SansSerif]. Downloadable Noto XML under `res/font/`
 * is used only by [com.pixijoy.ai.util.CanvasFontHelper] with try/catch fallbacks —
 * never wired through Compose `Font(R.font…)` (that path can crash when the
 * provider XML is unresolved on emulator / offline).
 */
fun loadBoardFontFamily(): FontFamily = FontFamily.SansSerif
