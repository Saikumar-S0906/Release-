package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.min

/**
 * Hue-band ranges for [PhotoOp.SelectiveHsl] (Lightroom-style 7 bands).
 * Soft triangular mask with wrap-around for Reds.
 */
enum class SelectiveHslRange(
    /** Band center in degrees [0, 360). */
    val centerDeg: Float,
    /** Half-width to zero weight (degrees). */
    val halfWidthDeg: Float
) {
    Reds(0f, 40f),
    Oranges(30f, 35f),
    Yellows(60f, 35f),
    Greens(120f, 55f),
    Cyans(180f, 35f),
    Blues(240f, 55f),
    Magentas(300f, 40f);

    /** Soft weight in [0, 1] for a pixel hue (degrees). */
    fun weight(hueDeg: Float): Float {
        val d = circularHueDistance(hueDeg, centerDeg)
        if (d >= halfWidthDeg) return 0f
        return (1f - d / halfWidthDeg).coerceIn(0f, 1f)
    }
}

fun circularHueDistance(a: Float, b: Float): Float {
    val d = abs(((a % 360f) + 360f) % 360f - ((b % 360f) + 360f) % 360f)
    return min(d, 360f - d)
}

fun PhotoOp.SelectiveHsl.isIdentity(
    hueEps: Float = 0.01f,
    satEps: Float = 0.01f,
    lightEps: Float = 0.01f
): Boolean =
    abs(hue) < hueEps && abs(sat - 1f) < satEps && abs(light) < lightEps

/**
 * In-place HSV selective adjust. [hsv] is Color.colorToHSV layout (H,S,V).
 * [sat] is a multiplier (1 = unchanged), [light] is additive on V (-1..1 typical).
 */
fun applySelectiveHslToHsv(
    hsv: FloatArray,
    range: SelectiveHslRange,
    hue: Float,
    sat: Float,
    light: Float
) {
    val w = range.weight(hsv[0])
    if (w <= 0f) return
    hsv[0] = (hsv[0] + hue * w + 360f) % 360f
    val satTarget = (hsv[1] * sat).coerceIn(0f, 1f)
    hsv[1] = hsv[1] + (satTarget - hsv[1]) * w
    hsv[2] = (hsv[2] + light * w).coerceIn(0f, 1f)
}

fun applySelectiveHsl(
    src: Bitmap,
    range: SelectiveHslRange,
    hue: Float,
    sat: Float,
    light: Float
): Bitmap {
    if (abs(hue) < 0.01f && abs(sat - 1f) < 0.01f && abs(light) < 0.01f) {
        return src
    }
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val hsv = FloatArray(3)
    for (i in px.indices) {
        val c = px[i]
        if (Color.alpha(c) == 0) continue
        Color.colorToHSV(c, hsv)
        applySelectiveHslToHsv(hsv, range, hue, sat, light)
        px[i] = (c and 0xFF000000.toInt()) or (Color.HSVToColor(hsv) and 0x00FFFFFF)
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}
