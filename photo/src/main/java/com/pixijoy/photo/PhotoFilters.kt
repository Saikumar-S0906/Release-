package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun sampleCornerColor(pixels: IntArray, width: Int, height: Int): Int {
    val points = intArrayOf(
        2, 2, width - 3, 2, 2, height - 3, width - 3, height - 3,
        width / 2, 2, width / 2, height - 3
    )
    var r = 0
    var g = 0
    var b = 0
    var n = 0
    var i = 0
    while (i < points.size) {
        val x = points[i].coerceIn(0, width - 1)
        val y = points[i + 1].coerceIn(0, height - 1)
        val c = pixels[y * width + x]
        r += Color.red(c)
        g += Color.green(c)
        b += Color.blue(c)
        n++
        i += 2
    }
    return Color.rgb(r / n, g / n, b / n)
}

fun colorDistance(a: Int, b: Int): Float {
    val dr = (((a shr 16) and 0xff) - ((b shr 16) and 0xff)).toFloat()
    val dg = (((a shr 8) and 0xff) - ((b shr 8) and 0xff)).toFloat()
    val db = ((a and 0xff) - (b and 0xff)).toFloat()
    return sqrt(dr * dr + dg * dg + db * db)
}

fun applyCutout(src: Bitmap, threshold: Float = 42f, feather: Float = 28f): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val key = sampleCornerColor(px, w, h)
    for (i in px.indices) {
        val d = colorDistance(px[i], key)
        val alpha = when {
            d <= threshold -> 0
            d >= threshold + feather -> Color.alpha(px[i])
            else -> ((d - threshold) / feather * Color.alpha(px[i])).roundToInt()
        }
        px[i] = (alpha shl 24) or (px[i] and 0x00FFFFFF)
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun applyHeal(src: Bitmap, stamps: List<Stamp>): Bitmap {
    if (stamps.isEmpty()) return src
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    for (stamp in stamps) {
        val cx = (stamp.x * w).roundToInt()
        val cy = (stamp.y * h).roundToInt()
        val radius = (stamp.r * min(w, h)).roundToInt().coerceAtLeast(2)
        val srcY = (cy - radius * 2).coerceIn(0, h - 1)
        for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(h - 1)) {
            for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(w - 1)) {
                if (hypot((x - cx).toDouble(), (y - cy).toDouble()) > radius) continue
                val sy = (srcY + (y - cy)).coerceIn(0, h - 1)
                px[y * w + x] = px[sy * w + x]
            }
        }
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun applyErase(src: Bitmap, stamps: List<Stamp>): Bitmap {
    if (stamps.isEmpty()) return src
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    for (stamp in stamps) {
        val cx = (stamp.x * w).roundToInt()
        val cy = (stamp.y * h).roundToInt()
        val radius = (stamp.r * min(w, h)).roundToInt().coerceAtLeast(2)
        for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(h - 1)) {
            for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(w - 1)) {
                val d = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
                if (d > radius) continue
                val fade = ((1f - d / radius) * 255f).roundToInt()
                val a = (Color.alpha(px[y * w + x]) - fade).coerceAtLeast(0)
                px[y * w + x] = (a shl 24) or (px[y * w + x] and 0x00FFFFFF)
            }
        }
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun applyHsl(src: Bitmap, hue: Float, saturation: Float): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val hsv = FloatArray(3)
    for (i in px.indices) {
        val c = px[i]
        Color.colorToHSV(c, hsv)
        hsv[0] = (hsv[0] + hue + 360f) % 360f
        hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)
        px[i] = (c and 0xFF000000.toInt()) or (Color.HSVToColor(hsv) and 0x00FFFFFF)
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun applyUnsharp(src: Bitmap, amount: Float = 0.5f): Bitmap {
    val w = src.width
    val h = src.height
    val srcPx = IntArray(w * h)
    src.getPixels(srcPx, 0, w, 0, 0, w, h)
    val blur = IntArray(srcPx.size)
    boxBlur(srcPx, blur, w, h)
    val outPx = IntArray(srcPx.size)
    for (i in srcPx.indices) {
        val a = Color.alpha(srcPx[i])
        fun mix(channel: (Int) -> Int): Int {
            val v = channel(srcPx[i]) + amount * (channel(srcPx[i]) - channel(blur[i]))
            return v.roundToInt().coerceIn(0, 255)
        }
        outPx[i] = Color.argb(a, mix(Color::red), mix(Color::green), mix(Color::blue))
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(outPx, 0, w, 0, 0, w, h)
    return out
}

fun applyRestore(src: Bitmap): Bitmap {
    val contrast = applyGradeBitmap(src, 0.1f, 1.16f)
    val sat = applyHsl(contrast, 0f, 1.12f)
    if (contrast !== src && !contrast.isRecycled) contrast.recycle()
    val sharp = applyUnsharp(sat, 0.42f)
    if (!sat.isRecycled) sat.recycle()
    return sharp
}

fun applyExpand(src: Bitmap, fraction: Float): Bitmap {
    val padX = (src.width * fraction).roundToInt().coerceAtLeast(8)
    val padY = (src.height * fraction).roundToInt().coerceAtLeast(8)
    val out = Bitmap.createBitmap(src.width + padX * 2, src.height + padY * 2, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val shader = BitmapShader(src, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
    val matrix = Matrix().apply { postTranslate(padX.toFloat(), padY.toFloat()) }
    shader.setLocalMatrix(matrix)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
    canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), paint)
    canvas.drawBitmap(src, padX.toFloat(), padY.toFloat(), null)
    return out
}

fun applyText(src: Bitmap, op: PhotoOp.TextLayer): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = op.color
        textAlign = Paint.Align.CENTER
        textSize = op.size * src.width
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setShadowLayer(6f, 0f, 2f, Color.argb(160, 0, 0, 0))
    }
    canvas.drawText(op.text, op.x * src.width, op.y * src.height, paint)
    return out
}

fun applySticker(src: Bitmap, op: PhotoOp.Sticker): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = op.scale * src.width
        setShadowLayer(8f, 0f, 2f, Color.argb(140, 0, 0, 0))
    }
    canvas.drawText(op.glyph, op.x * src.width, op.y * src.height, paint)
    return out
}

fun applyCollage(sources: List<Bitmap>, columns: Int, maxEdge: Int): Bitmap {
    val cols = columns.coerceIn(1, 4)
    val count = max(sources.size, 1)
    val rows = (count + cols - 1) / cols
    val cell = maxEdge / max(cols, rows)
    val out = Bitmap.createBitmap(cell * cols, cell * rows, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.BLACK)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    sources.take(cols * rows).forEachIndexed { index, bmp ->
        val col = index % cols
        val row = index / cols
        val dest = android.graphics.Rect(col * cell, row * cell, (col + 1) * cell, (row + 1) * cell)
        canvas.drawBitmap(bmp, null, dest, paint)
    }
    return out
}

fun applyGradeBitmap(src: Bitmap, exposureEv: Float, contrast: Float): Bitmap {
    val gain = Math.pow(2.0, exposureEv.toDouble()).toFloat()
    val translate = (1f - contrast) * 0.18f * 255f
    val matrix = android.graphics.ColorMatrix(
        floatArrayOf(
            contrast * gain, 0f, 0f, 0f, translate,
            0f, contrast * gain, 0f, 0f, translate,
            0f, 0f, contrast * gain, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}

fun evalCurve(t: Float, ys: FloatArray): Float {
    val x = t.coerceIn(0f, 1f)
    if (ys.size < 2) return x
    val n = ys.size - 1
    val pos = x * n
    val i = pos.toInt().coerceIn(0, n - 1)
    val f = pos - i
    return (ys[i] * (1f - f) + ys[i + 1] * f).coerceIn(0f, 1f)
}

fun applyCurves(src: Bitmap, ys: FloatArray): Bitmap {
    val lut = IntArray(256) { i -> (evalCurve(i / 255f, ys) * 255f).roundToInt().coerceIn(0, 255) }
    val w = src.width
    val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    for (i in px.indices) {
        val c = px[i]
        val a = Color.alpha(c)
        val r = lut[Color.red(c)]
        val g = lut[Color.green(c)]
        val b = lut[Color.blue(c)]
        px[i] = Color.argb(a, r, g, b)
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun applyVignette(src: Bitmap, amount: Float): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val cx = w / 2f
    val cy = h / 2f
    val maxD = hypot(cx, cy)
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val a = amount.coerceIn(0f, 1f)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            val d = hypot(x - cx, y - cy) / maxD
            val fall = ((d - (1f - a)).coerceIn(0f, 1f) / a.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
            val m = 1f - a * fall * fall
            val c = px[i]
            px[i] = Color.argb(
                Color.alpha(c),
                (Color.red(c) * m).roundToInt().coerceIn(0, 255),
                (Color.green(c) * m).roundToInt().coerceIn(0, 255),
                (Color.blue(c) * m).roundToInt().coerceIn(0, 255)
            )
        }
    }
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

fun applyBlurPasses(src: Bitmap, radius: Int): Bitmap {
    var current = src
    repeat(radius.coerceIn(1, 8)) {
        val next = boxBlurBitmap(current)
        if (current !== src && !current.isRecycled) current.recycle()
        current = next
    }
    return current
}

fun boxBlurBitmap(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    val dest = IntArray(px.size)
    boxBlur(px, dest, w, h)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(dest, 0, w, 0, 0, w, h)
    return out
}

fun applyMaskAlpha(src: Bitmap, mask: Bitmap): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    val scaled = if (mask.width == w && mask.height == h) mask
    else Bitmap.createScaledBitmap(mask, w, h, true)
    val srcPx = IntArray(w * h)
    val maskPx = IntArray(w * h)
    out.getPixels(srcPx, 0, w, 0, 0, w, h)
    scaled.getPixels(maskPx, 0, w, 0, 0, w, h)
    for (i in srcPx.indices) {
        val a = Color.red(maskPx[i]).coerceIn(0, 255)
        srcPx[i] = (a shl 24) or (srcPx[i] and 0x00FFFFFF)
    }
    out.setPixels(srcPx, 0, w, 0, 0, w, h)
    if (scaled !== mask && !scaled.isRecycled) scaled.recycle()
    return out
}

fun lumaHistogram(src: Bitmap): IntArray {
    val bins = IntArray(256)
    val w = src.width
    val h = src.height
    val step = max(1, min(w, h) / 128)
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    var y = 0
    while (y < h) {
        var x = 0
        while (x < w) {
            val c = px[y * w + x]
            val luma = (77 * Color.red(c) + 150 * Color.green(c) + 29 * Color.blue(c)) shr 8
            bins[luma.coerceIn(0, 255)]++
            x += step
        }
        y += step
    }
    return bins
}

private fun boxBlur(src: IntArray, dest: IntArray, w: Int, h: Int) {
    src.copyInto(dest)
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            var r = 0
            var g = 0
            var b = 0
            var a = 0
            for (oy in -1..1) {
                for (ox in -1..1) {
                    val c = src[(y + oy) * w + (x + ox)]
                    a += Color.alpha(c)
                    r += Color.red(c)
                    g += Color.green(c)
                    b += Color.blue(c)
                }
            }
            dest[y * w + x] = Color.argb(a / 9, r / 9, g / 9, b / 9)
        }
    }
}
