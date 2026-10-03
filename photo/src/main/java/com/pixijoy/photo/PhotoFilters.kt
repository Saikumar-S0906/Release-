package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.floor
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
    val srcPx = IntArray(w * h)
    val dstPx = IntArray(w * h)
    out.getPixels(srcPx, 0, w, 0, 0, w, h)
    srcPx.copyInto(dstPx)
    for (stamp in stamps) {
        val cx = (stamp.x * w).roundToInt()
        val cy = (stamp.y * h).roundToInt()
        val radius = (stamp.r * min(w, h)).roundToInt().coerceAtLeast(3)
        if (stamp.hasSource) {
            val ox = ((stamp.sx!! - stamp.x) * w).roundToInt()
            val oy = ((stamp.sy!! - stamp.y) * h).roundToInt()
            cloneStamp(srcPx, dstPx, w, h, cx, cy, radius, ox, oy)
        } else {
            ringHealStamp(srcPx, dstPx, w, h, cx, cy, radius)
        }
    }
    out.setPixels(dstPx, 0, w, 0, 0, w, h)
    return out
}

fun cloneStamp(
    srcPx: IntArray,
    dstPx: IntArray,
    w: Int,
    h: Int,
    cx: Int,
    cy: Int,
    radius: Int,
    ox: Int,
    oy: Int
) {
    for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(h - 1)) {
        for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(w - 1)) {
            val d = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
            if (d > radius) continue
            val sx = (x + ox).coerceIn(0, w - 1)
            val sy = (y + oy).coerceIn(0, h - 1)
            val t = (1f - d / radius).coerceIn(0f, 1f)
            dstPx[y * w + x] = mixArgb(dstPx[y * w + x], srcPx[sy * w + sx], t)
        }
    }
}

private fun ringHealStamp(
    srcPx: IntArray,
    dstPx: IntArray,
    w: Int,
    h: Int,
    cx: Int,
    cy: Int,
    radius: Int
) {
    val inner = radius
    val outer = (radius * 1.7f).roundToInt()
    for (y in (cy - inner).coerceAtLeast(0)..(cy + inner).coerceAtMost(h - 1)) {
        for (x in (cx - inner).coerceAtLeast(0)..(cx + inner).coerceAtMost(w - 1)) {
            val d = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
            if (d > inner) continue
            var ar = 0.0
            var ag = 0.0
            var ab = 0.0
            var aa = 0.0
            var n = 0
            for (yy in (cy - outer).coerceAtLeast(0)..(cy + outer).coerceAtMost(h - 1)) {
                for (xx in (cx - outer).coerceAtLeast(0)..(cx + outer).coerceAtMost(w - 1)) {
                    val dd = hypot((xx - cx).toDouble(), (yy - cy).toDouble()).toFloat()
                    if (dd < inner + 1 || dd > outer) continue
                    val c = srcPx[yy * w + xx]
                    ar += (c shr 16) and 0xff
                    ag += (c shr 8) and 0xff
                    ab += c and 0xff
                    aa += (c ushr 24) and 0xff
                    n++
                }
            }
            if (n == 0) continue
            val fill = (aa / n).roundToInt().shl(24) or
                (ar / n).roundToInt().shl(16) or
                (ag / n).roundToInt().shl(8) or
                (ab / n).roundToInt()
            dstPx[y * w + x] = mixArgb(srcPx[y * w + x], fill, (1f - d / inner).coerceIn(0f, 1f))
        }
    }
}

private fun mixArgb(orig: Int, fill: Int, t: Float): Int {
    val a = mixByte((orig ushr 24) and 0xff, (fill ushr 24) and 0xff, t)
    val r = mixByte((orig shr 16) and 0xff, (fill shr 16) and 0xff, t)
    val g = mixByte((orig shr 8) and 0xff, (fill shr 8) and 0xff, t)
    val b = mixByte(orig and 0xff, fill and 0xff, t)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun mixByte(a: Int, b: Int, t: Float): Int =
    (a + (b - a) * t).roundToInt().coerceIn(0, 255)

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

/** Hue then saturation, the same matrix as the live slider. Not an HSV rotate. */
fun applyHsl(src: Bitmap, hue: Float, saturation: Float): Bitmap =
    applyColorMatrix(src, LiveGrade.colorMatrix(0f, 1f, hue, saturation))

private fun applyColorMatrix(src: Bitmap, matrix: FloatArray): Bitmap {
    val w = src.width
    val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    for (i in px.indices) {
        val c = px[i]
        val a = Color.alpha(c)
        px[i] = Color.argb(
            matrixChannel(matrix, 3, c, a),
            matrixChannel(matrix, 0, c, a),
            matrixChannel(matrix, 1, c, a),
            matrixChannel(matrix, 2, c, a)
        )
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
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

/**
 * Stored collage count stays 1–4. The 4-up chip encodes as 4 and draws as a
 * 2×2 grid, not one row of four.
 */
fun collageGrid(columns: Int, sourceCount: Int): Pair<Int, Int> {
    val stored = columns.coerceIn(1, 4)
    val count = max(sourceCount, 1)
    if (stored == 4) return 2 to 2
    val rows = (count + stored - 1) / stored
    return stored to rows
}

fun applyCollage(sources: List<Bitmap>, columns: Int, maxEdge: Int): Bitmap {
    val (cols, rows) = collageGrid(columns, sources.size)
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

fun applyGradeBitmap(src: Bitmap, exposureEv: Float, contrast: Float): Bitmap =
    applyColorMatrix(src, LiveGrade.gradeMatrix(exposureEv, contrast))

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

/** Normalized falloff from [LiveGrade.vignetteMul], including on non-square frames. */
fun applyVignette(src: Bitmap, amount: Float): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val w = out.width
    val h = out.height
    if (w <= 0 || h <= 0) return out
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val a = amount.coerceIn(0f, 1f)
    for (y in 0 until h) {
        val ny = y.toFloat() / h
        for (x in 0 until w) {
            val i = y * w + x
            val m = LiveGrade.vignetteMul(x.toFloat() / w, ny, a)
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

/**
 * Matches [LiveGrade.DETAIL_AGSL]: 5×5 taps spaced by [radius] pixels,
 * mixed by `radius / 8`. At or below 0.5 the shader does not blur.
 */
fun applyBlurPasses(src: Bitmap, radius: Float): Bitmap {
    val w = src.width
    val h = src.height
    val blur = radius.coerceIn(0f, 16f)
    if (w <= 0 || h <= 0 || blur <= 0.5f) return src.copy(Bitmap.Config.ARGB_8888, false)
    val spread = max(blur, 1f)
    val mix = (blur / 8f).coerceIn(0f, 1f)
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    val outPx = IntArray(px.size)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var accA = 0f
            var accR = 0f
            var accG = 0f
            var accB = 0f
            for (oy in -2..2) {
                for (ox in -2..2) {
                    val s = sampleClamp(px, w, h, x + ox * spread, y + oy * spread)
                    accA += Color.alpha(s)
                    accR += Color.red(s)
                    accG += Color.green(s)
                    accB += Color.blue(s)
                }
            }
            val c = px[y * w + x]
            outPx[y * w + x] = Color.argb(
                mixChannel(Color.alpha(c), accA, mix),
                mixChannel(Color.red(c), accR, mix),
                mixChannel(Color.green(c), accG, mix),
                mixChannel(Color.blue(c), accB, mix)
            )
        }
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(outPx, 0, w, 0, 0, w, h)
    return out
}

private fun mixChannel(base: Int, acc: Float, mix: Float): Int =
    (base + (acc / 25f - base) * mix).roundToInt().coerceIn(0, 255)

private fun sampleClamp(px: IntArray, w: Int, h: Int, fx: Float, fy: Float): Int {
    val x0 = floor(fx).toInt()
    val y0 = floor(fy).toInt()
    val tx = fx - x0
    val ty = fy - y0
    fun at(x: Int, y: Int): Int = px[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]
    if (tx == 0f && ty == 0f) return at(x0, y0)
    val c00 = at(x0, y0)
    val c10 = at(x0 + 1, y0)
    val c01 = at(x0, y0 + 1)
    val c11 = at(x0 + 1, y0 + 1)
    fun channel(sel: (Int) -> Int): Int {
        val v = sel(c00) * (1f - tx) * (1f - ty) +
            sel(c10) * tx * (1f - ty) +
            sel(c01) * (1f - tx) * ty +
            sel(c11) * tx * ty
        return v.roundToInt().coerceIn(0, 255)
    }
    return Color.argb(channel(Color::alpha), channel(Color::red), channel(Color::green), channel(Color::blue))
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

private val histogramIdentity = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

fun lumaHistogram(src: Bitmap): IntArray = lumaHistogramGraded(src, histogramIdentity, 0f)

/** Bins after the live grade matrix and vignette. Blur and sharpen are not folded in. */
fun lumaHistogramGraded(src: Bitmap, matrix: FloatArray, vignette: Float): IntArray {
    val bins = IntArray(256)
    val w = src.width
    val h = src.height
    if (w <= 0 || h <= 0) return bins
    val step = max(1, min(w, h) / 128)
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    val vig = vignette.coerceIn(0f, 1f)
    var y = 0
    while (y < h) {
        val ny = y.toFloat() / h
        var x = 0
        while (x < w) {
            val c = px[y * w + x]
            val a = Color.alpha(c)
            var r = matrixChannel(matrix, 0, c, a)
            var g = matrixChannel(matrix, 1, c, a)
            var b = matrixChannel(matrix, 2, c, a)
            if (vig > 0f) {
                val m = LiveGrade.vignetteMul(x.toFloat() / w, ny, vig)
                r = (r * m).roundToInt().coerceIn(0, 255)
                g = (g * m).roundToInt().coerceIn(0, 255)
                b = (b * m).roundToInt().coerceIn(0, 255)
            }
            val luma = (77 * r + 150 * g + 29 * b) shr 8
            bins[luma.coerceIn(0, 255)]++
            x += step
        }
        y += step
    }
    return bins
}

private fun matrixChannel(matrix: FloatArray, row: Int, color: Int, alpha: Int): Int {
    val i = row * 5
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    return (matrix[i] * r + matrix[i + 1] * g + matrix[i + 2] * b + matrix[i + 3] * alpha + matrix[i + 4])
        .roundToInt()
        .coerceIn(0, 255)
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

fun applyLut(src: Bitmap, assetKey: String, lutDir: java.io.File? = null): Bitmap {
    if (BundledLooks.isIdentity(assetKey) && !ImportedLuts.isImported(assetKey)) return src
    if (ImportedLuts.isImported(assetKey)) {
        val file = ImportedLuts.resolve(lutDir, assetKey)
            ?: return src
        return when (val parsed = CubeLutParser.parseFile(file, maxSize = 33)) {
            is LutParseResult.Ok -> applyCubeLut(src, parsed.lut)
            is LutParseResult.Err -> src
        }
    }
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(BundledLooks.androidMatrix(assetKey))
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}

fun applyCubeLut(src: Bitmap, lut: CubeLut): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        val c = pixels[i]
        val a = Color.alpha(c)
        val rgb = CubeLutParser.sampleNearest(
            lut,
            Color.red(c) / 255f,
            Color.green(c) / 255f,
            Color.blue(c) / 255f
        )
        pixels[i] = Color.argb(
            a,
            (rgb[0] * 255f).toInt().coerceIn(0, 255),
            (rgb[1] * 255f).toInt().coerceIn(0, 255),
            (rgb[2] * 255f).toInt().coerceIn(0, 255)
        )
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return out
}