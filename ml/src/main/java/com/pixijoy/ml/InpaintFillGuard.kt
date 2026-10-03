package com.pixijoy.ml

import kotlin.math.abs

/**
 * A bound inpaint graph can still emit a dead hole: almost no texture, while
 * the surrounding photo has texture. That is the untrained residual (flat
 * mid-gray), not a repair. A flat hole on a flat surround is kept.
 */
internal fun inpaintHoleIsDeadFill(holeRgb: FloatArray, surroundRgb: FloatArray): Boolean {
    if (holeRgb.size < 48 || holeRgb.size % 3 != 0) return false
    if (surroundRgb.size < 48 || surroundRgb.size % 3 != 0) return false
    return channelMad(holeRgb) < 0.02f && channelMad(surroundRgb) > 0.06f
}

private fun channelMad(rgb: FloatArray): Float {
    val n = rgb.size / 3
    var r = 0f
    var g = 0f
    var b = 0f
    var i = 0
    while (i < rgb.size) {
        r += rgb[i]
        g += rgb[i + 1]
        b += rgb[i + 2]
        i += 3
    }
    r /= n
    g /= n
    b /= n
    var mad = 0.0
    i = 0
    while (i < rgb.size) {
        mad += abs(rgb[i] - r) + abs(rgb[i + 1] - g) + abs(rgb[i + 2] - b)
        i += 3
    }
    return (mad / (n * 3)).toFloat()
}
