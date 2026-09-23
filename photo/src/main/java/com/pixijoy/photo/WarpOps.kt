package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * C5 — perspective skew via destination quad (normalized 0..1 corners).
 */
fun applyPerspective(
    src: Bitmap,
    tlX: Float,
    tlY: Float,
    trX: Float,
    trY: Float,
    brX: Float,
    brY: Float,
    blX: Float,
    blY: Float
): Bitmap {
    val w = src.width.toFloat()
    val h = src.height.toFloat()
    val srcPts = floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
    val dstPts = floatArrayOf(
        tlX * w, tlY * h,
        trX * w, trY * h,
        brX * w, brY * h,
        blX * w, blY * h
    )
    val matrix = Matrix()
    if (!matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)) return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    return out
}

/** Convenience: horizontal / vertical keystone in −0.35..0.35. */
fun applyPerspectiveKeystone(src: Bitmap, horizontal: Float, vertical: Float): Bitmap {
    val hx = horizontal.coerceIn(-0.35f, 0.35f)
    val vy = vertical.coerceIn(-0.35f, 0.35f)
    return applyPerspective(
        src,
        tlX = 0f + hx, tlY = 0f + vy,
        trX = 1f - hx, trY = 0f + vy,
        brX = 1f - hx, brY = 1f - vy,
        blX = 0f + hx, blY = 1f - vy
    )
}

/**
 * C5 — local liquify push within [radiusNorm]; amount > 0 pushes out, < 0 pinches.
 */
fun applyLiquify(
    src: Bitmap,
    cxNorm: Float,
    cyNorm: Float,
    radiusNorm: Float,
    amount: Float
): Bitmap {
    if (kotlin.math.abs(amount) < 0.001f || radiusNorm <= 0f) return src
    val w = src.width
    val h = src.height
    val cx = cxNorm.coerceIn(0f, 1f) * w
    val cy = cyNorm.coerceIn(0f, 1f) * h
    val radius = (radiusNorm.coerceIn(0.02f, 0.5f) * min(w, h)).coerceAtLeast(2f)
    val strength = amount.coerceIn(-1f, 1f)
    val srcPx = IntArray(w * h)
    src.getPixels(srcPx, 0, w, 0, 0, w, h)
    val dst = IntArray(w * h)
    srcPx.copyInto(dst)
    val r2 = radius * radius
    for (y in 0 until h) {
        for (x in 0 until w) {
            val dx = x - cx
            val dy = y - cy
            val d2 = dx * dx + dy * dy
            if (d2 >= r2 || d2 < 0.25f) continue
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val t = 1f - d / radius
            val falloff = t * t * (3f - 2f * t)
            val push = strength * falloff * radius * 0.35f
            val sx = (x - dx / d * push).roundToInt().coerceIn(0, w - 1)
            val sy = (y - dy / d * push).roundToInt().coerceIn(0, h - 1)
            dst[y * w + x] = srcPx[sy * w + sx]
        }
    }
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    out.setPixels(dst, 0, w, 0, 0, w, h)
    return out
}

/**
 * C8 lightweight portrait soften: average neighborhood on warm midtones (skin-like).
 * Model-heavy retouch stays R5.
 */
fun applyPortraitSoften(src: Bitmap, amount: Float): Bitmap {
    val a = amount.coerceIn(0f, 1f)
    if (a < 0.01f) return src
    val w = src.width
    val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    val out = IntArray(w * h)
    val hsv = FloatArray(3)
    val rad = (1 + a * 2).roundToInt().coerceIn(1, 3)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            argbToHsv(px[i], hsv)
            val skin = hsv[0] in 5f..50f && hsv[1] in 0.12f..0.65f && hsv[2] in 0.25f..0.95f
            if (!skin && a < 0.5f) {
                out[i] = px[i]
                continue
            }
            var r = 0
            var g = 0
            var b = 0
            var n = 0
            for (dy in -rad..rad) {
                for (dx in -rad..rad) {
                    val xx = (x + dx).coerceIn(0, w - 1)
                    val yy = (y + dy).coerceIn(0, h - 1)
                    val c = px[yy * w + xx]
                    r += (c shr 16) and 0xff
                    g += (c shr 8) and 0xff
                    b += c and 0xff
                    n++
                }
            }
            val soft = (0xff shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            val t = if (skin) a * 0.85f else a * 0.15f
            out[i] = mixSoft(px[i], soft, t)
        }
    }
    val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
    bmp.setPixels(out, 0, w, 0, 0, w, h)
    return bmp
}

private fun mixSoft(a: Int, b: Int, t: Float): Int {
    val u = t.coerceIn(0f, 1f)
    val aa = (a ushr 24) and 0xff
    val r = ((1f - u) * ((a shr 16) and 0xff) + u * ((b shr 16) and 0xff)).roundToInt()
    val g = ((1f - u) * ((a shr 8) and 0xff) + u * ((b shr 8) and 0xff)).roundToInt()
    val bl = ((1f - u) * (a and 0xff) + u * (b and 0xff)).roundToInt()
    return (aa shl 24) or (r shl 16) or (g shl 8) or bl
}
