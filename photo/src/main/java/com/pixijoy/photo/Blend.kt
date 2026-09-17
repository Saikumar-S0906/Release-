package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.min

fun blendBitmaps(dst: Bitmap, src: Bitmap, opacity: Float, mode: BlendMode): Bitmap {
    val w = min(dst.width, src.width)
    val h = min(dst.height, src.height)
    val out = dst.copy(Bitmap.Config.ARGB_8888, true)
    val a = IntArray(w * h)
    val b = IntArray(w * h)
    out.getPixels(a, 0, w, 0, 0, w, h)
    src.getPixels(b, 0, w, 0, 0, w, h)
    val o = opacity.coerceIn(0f, 1f)
    for (i in a.indices) {
        a[i] = blendPixel(a[i], b[i], o, mode)
    }
    out.setPixels(a, 0, w, 0, 0, w, h)
    return out
}

fun blendPixel(dst: Int, src: Int, opacity: Float, mode: BlendMode): Int {
    val sr = Color.red(src)
    val sg = Color.green(src)
    val sb = Color.blue(src)
    val dr = Color.red(dst)
    val dg = Color.green(dst)
    val db = Color.blue(dst)
    val br = blendChannel(dr, sr, mode)
    val bg = blendChannel(dg, sg, mode)
    val bb = blendChannel(db, sb, mode)
    val r = mix(dr, br, opacity)
    val g = mix(dg, bg, opacity)
    val b = mix(db, bb, opacity)
    return Color.argb(Color.alpha(src).coerceAtLeast(Color.alpha(dst)), r, g, b)
}

fun blendChannel(d: Int, s: Int, mode: BlendMode): Int = when (mode) {
    BlendMode.Normal -> s
    BlendMode.Multiply -> (d * s) / 255
    BlendMode.Screen -> 255 - (255 - d) * (255 - s) / 255
    BlendMode.Overlay -> if (d < 128) 2 * d * s / 255 else 255 - 2 * (255 - d) * (255 - s) / 255
}

private fun mix(a: Int, b: Int, t: Float): Int =
    (a + (b - a) * t).toInt().coerceIn(0, 255)
