package com.pixijoy.ai.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central PixiJoy design tokens (Design Bible §4–9, §27, §55).
 * Board + app chrome should consume these — avoid one-off dp/sp/hex in hosts.
 */
object PixiSpacing {
    /** Bible XS */
    val xs: Dp = 4.dp
    /** Bible S */
    val sm: Dp = 8.dp
    /** Bible SM */
    val md: Dp = 12.dp
    /** Bible M — default screen horizontal padding */
    val lg: Dp = 16.dp
    /** Bible L */
    val xl: Dp = 24.dp
    /** Bible XL */
    val xxl: Dp = 32.dp
    /** Bible XXL */
    val xxxl: Dp = 48.dp
    /** Bible XXXL */
    val huge: Dp = 64.dp
    /** Min touch target (Bible §5) */
    val touch: Dp = 48.dp
}

object PixiRadii {
    /** Small control */
    val sm: Dp = 8.dp
    /** Standard card */
    val md: Dp = 16.dp
    /** Large card */
    val lg: Dp = 20.dp
    /** Bottom sheet / dialog */
    val sheet: Dp = 28.dp
    val pill: Dp = 999.dp
}

/**
 * Bible §27 animation targets (ms).
 * Tap 100–150 · small/fast 150–250 · component 200–300 · screen 250–400.
 * Pair with [com.pixijoy.ai.ui.board.components.rememberBoardReduceMotion].
 */
object PixiMotionMs {
    const val Tap = 120
    const val Fast = 150
    const val Normal = 250
    const val Component = 280
    const val Screen = 350
    const val Slow = 400
}
