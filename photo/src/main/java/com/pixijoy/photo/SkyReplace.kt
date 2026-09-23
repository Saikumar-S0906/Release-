package com.pixijoy.photo

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * C4 — replace sky or flat background with a solid fill.
 *
 * Sky uses a local HSV + top-bias heuristic matte (no ML pack).
 * Background prefers an optional subject mask; falls back to corner-key similarity
 * (same family as [applyCutout]).
 */
enum class SkyReplaceTarget { Sky, Background }

object SkyReplacePresets {
    val BlueSky = 0xFF87CEEB.toInt()
    val DeepBlue = 0xFF1E3A8A.toInt()
    val Sunset = 0xFFE07A3D.toInt()
    val Overcast = 0xFF9CA3AF.toInt()
    val StudioGreen = 0xFF00A86B.toInt()

    fun all(): List<Pair<String, Int>> = listOf(
        "sky_blue" to BlueSky,
        "sky_deep" to DeepBlue,
        "sky_sunset" to Sunset,
        "sky_overcast" to Overcast,
        "bg_green" to StudioGreen
    )
}

fun applySkyReplace(
    src: Bitmap,
    target: SkyReplaceTarget,
    color: Int,
    subjectMask: Bitmap? = null
): Bitmap {
    val w = src.width
    val h = src.height
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val matte = when (target) {
        SkyReplaceTarget.Sky -> buildSkyMatte(px, w, h)
        SkyReplaceTarget.Background -> buildBackgroundMatte(px, w, h, subjectMask)
    }
    softMatte(matte, w, h)
    val fr = (color shr 16) and 0xff
    val fg = (color shr 8) and 0xff
    val fb = color and 0xff
    for (i in px.indices) {
        val t = matte[i]
        if (t <= 0.001f) continue
        val c = px[i]
        val a = (c ushr 24) and 0xff
        val r = ((1f - t) * ((c shr 16) and 0xff) + t * fr).roundToInt().coerceIn(0, 255)
        val g = ((1f - t) * ((c shr 8) and 0xff) + t * fg).roundToInt().coerceIn(0, 255)
        val b = ((1f - t) * (c and 0xff) + t * fb).roundToInt().coerceIn(0, 255)
        px[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

/** Soft sky confidence 0..1 per pixel. */
internal fun buildSkyMatte(px: IntArray, w: Int, h: Int): FloatArray {
    val matte = FloatArray(px.size)
    val hsv = FloatArray(3)
    for (y in 0 until h) {
        val topBias = (1.15f - y.toFloat() / h.toFloat()).coerceIn(0.15f, 1f)
        val row = y * w
        for (x in 0 until w) {
            argbToHsv(px[row + x], hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]
            val blueish = hue in 175f..255f
            val cyanish = hue in 160f..200f && sat > 0.08f
            val brightLowSat = value > 0.72f && sat < 0.28f
            val vividSky = blueish && value > 0.45f && sat in 0.05f..0.85f
            val score = when {
                vividSky || cyanish && value > 0.5f -> 0.85f + 0.15f * value
                brightLowSat && y < h * 0.55f -> 0.55f + 0.35f * value
                blueish && value > 0.35f -> 0.4f * value
                else -> 0f
            }
            matte[row + x] = (score * topBias).coerceIn(0f, 1f)
        }
    }
    return matte
}

internal fun buildBackgroundMatte(
    px: IntArray,
    w: Int,
    h: Int,
    subjectMask: Bitmap?
): FloatArray {
    if (subjectMask != null &&
        !subjectMask.isRecycled &&
        subjectMask.width == w &&
        subjectMask.height == h
    ) {
        val maskPx = IntArray(w * h)
        subjectMask.getPixels(maskPx, 0, w, 0, 0, w, h)
        return backgroundMatteFromMaskArgb(maskPx)
    }
    return backgroundMatteFromCornerKey(px, w, h)
}

/** Subject opaque → keep (0); transparent/black → replace (1). */
internal fun backgroundMatteFromMaskArgb(maskPx: IntArray): FloatArray {
    val matte = FloatArray(maskPx.size)
    for (i in matte.indices) {
        val c = maskPx[i]
        val a = (c ushr 24) and 0xff
        val lum = (((c shr 16) and 0xff) + ((c shr 8) and 0xff) + (c and 0xff)) / 3
        val subject = max(a, lum) / 255f
        matte[i] = (1f - subject).coerceIn(0f, 1f)
    }
    return matte
}

internal fun backgroundMatteFromCornerKey(px: IntArray, w: Int, h: Int): FloatArray {
    val key = sampleCornerColor(px, w, h)
    val threshold = 42f
    val feather = 36f
    val matte = FloatArray(px.size)
    for (i in px.indices) {
        val d = colorDistance(px[i], key)
        matte[i] = when {
            d <= threshold -> 1f
            d >= threshold + feather -> 0f
            else -> 1f - (d - threshold) / feather
        }
    }
    return matte
}

/** 3×3 box blur to soften matte edges. */
internal fun softMatte(matte: FloatArray, w: Int, h: Int) {
    val copy = matte.copyOf()
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            var s = 0f
            for (dy in -1..1) {
                for (dx in -1..1) {
                    s += copy[(y + dy) * w + (x + dx)]
                }
            }
            matte[y * w + x] = s / 9f
        }
    }
}

/** HSV: H in 0..360, S/V in 0..1. JVM-safe (no android.graphics.Color). */
internal fun argbToHsv(argb: Int, out: FloatArray) {
    val r = ((argb shr 16) and 0xff) / 255f
    val g = ((argb shr 8) and 0xff) / 255f
    val b = (argb and 0xff) / 255f
    val maxC = maxOf(r, g, b)
    val minC = minOf(r, g, b)
    val d = maxC - minC
    out[2] = maxC
    out[1] = if (maxC == 0f) 0f else d / maxC
    out[0] = when {
        d == 0f -> 0f
        maxC == r -> (60f * (((g - b) / d) % 6f) + 360f) % 360f
        maxC == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
}
