package com.pixijoy.studio.ui.session

/**
 * The live slider number stays centered and still after a drag or a typed commit.
 * It does not follow the thumb. 0 and 1 are the track ends; 0.4 is the exposure entry.
 * The touch box stays 48dp, and the reveal asks for clearance under it so the bottom fade does not cover it.
 */
internal const val SLIDER_VALUE_MIN_TOUCH_DP = 48

/** Extra space requested under the readout so the bottom fade does not cover the 48dp box. */
internal const val SLIDER_VALUE_REVEAL_CLEARANCE_DP = 48

internal fun sliderValueFraction(
    value: Float,
    range: ClosedFloatingPointRange<Float>
): Float {
    if (!value.isFinite()) return 0f
    val span = range.endInclusive - range.start
    if (!span.isFinite() || span <= 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}

/** Horizontal shift of the live number. Always zero: it stays centered and does not ride the thumb. */
internal fun sliderReadoutShiftPx(fraction: Float): Float {
    if (!fraction.isFinite()) return 0f
    return 0f
}

/** Height passed to bring-into-view: the readout plus clearance, never a collapsed box. */
internal fun sliderValueRevealHeightPx(valueHeightPx: Float, clearancePx: Float): Float =
    valueHeightPx.coerceAtLeast(0f) + clearancePx.coerceAtLeast(0f)
