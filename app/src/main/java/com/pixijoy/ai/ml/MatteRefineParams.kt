package com.pixijoy.ai.ml

/**
 * C04 edge refine controls (0f..1f), aligned with [com.pixijoy.ai.ui.board.ctools.BoardFlowC04Session].
 * - precision: harder binary matte (tighter subject)
 * - feather: soft fringe width
 * - smooth: mask blur / noise reduction
 */
data class MatteRefineParams(
    val precision: Float = 0.7f,
    val feather: Float = 0.4f,
    val smooth: Float = 0.1f
) {
    val precisionClamped: Float get() = precision.coerceIn(0f, 1f)
    val featherClamped: Float get() = feather.coerceIn(0f, 1f)
    val smoothClamped: Float get() = smooth.coerceIn(0f, 1f)
}
