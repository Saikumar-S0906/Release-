package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure / lightly-Android helpers for Create-Extra Restore & Expand pipelines.
 * Dimension math and ColorMatrix coefficients are JVM-testable without Robolectric.
 */
object CreateExtraAlgorithms {

    data class ExpandLayout(
        val outWidth: Int,
        val outHeight: Int,
        val offsetX: Int,
        val offsetY: Int
    )

    /** Caps scaled dimension to avoid OOM on Upscale 4× of large sources. */
    fun cappedScaledSide(srcSide: Int, factor: Int, maxSide: Int): Int {
        require(srcSide > 0 && factor > 0 && maxSide > 0)
        return (srcSide.toLong() * factor).coerceAtMost(maxSide.toLong()).toInt().coerceAtLeast(1)
    }

    /** Expand option chips → outward padding as a fraction of source size. */
    fun expandPaddingRatio(optionIndex: Int): Float = when (optionIndex.coerceAtLeast(0) % 4) {
        0 -> 0.10f
        1 -> 0.18f
        2 -> 0.25f
        else -> 0.35f
    }

    /**
     * Target canvas size and centered offset for an outpaint-style expand.
     * Padding is applied on each side.
     */
    fun expandCanvasLayout(srcWidth: Int, srcHeight: Int, paddingRatio: Float): ExpandLayout {
        require(srcWidth > 0 && srcHeight > 0) { "source size must be positive" }
        val ratio = paddingRatio.coerceIn(0f, 1f)
        val padX = max(1, (srcWidth * ratio).roundToInt())
        val padY = max(1, (srcHeight * ratio).roundToInt())
        return ExpandLayout(
            outWidth = srcWidth + padX * 2,
            outHeight = srcHeight + padY * 2,
            offsetX = padX,
            offsetY = padY
        )
    }

    /**
     * Place [source] on a larger canvas filled with edge-sampled approximate fill color.
     * Uses [expandCanvasLayout]; Bitmap APIs require Android runtime / Robolectric.
     */
    fun expandCanvas(source: Bitmap, paddingRatio: Float): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        val layout = expandCanvasLayout(source.width, source.height, paddingRatio)
        val output = Bitmap.createBitmap(layout.outWidth, layout.outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(edgeFillColor(source))
        canvas.drawBitmap(source, layout.offsetX.toFloat(), layout.offsetY.toFloat(), null)
        return output
    }

    /** 4×5 ColorMatrix: mild contrast lift used in the Restore path. */
    fun restoreContrastMatrix(): FloatArray = floatArrayOf(
        1.15f, 0f, 0f, 0f, 12f,
        0f, 1.15f, 0f, 0f, 12f,
        0f, 0f, 1.15f, 0f, 12f,
        0f, 0f, 0f, 1f, 0f
    )

    fun restoreSaturationFactor(): Float = 1.12f

    /** Apply restore ColorMatrix pipeline to a bitmap (Android ColorMatrix). */
    fun applyRestoreColorMatrix(source: Bitmap): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("Bitmap.copy returned null")
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val contrast = ColorMatrix(restoreContrastMatrix())
        val saturation = ColorMatrix().apply { setSaturation(restoreSaturationFactor()) }
        contrast.postConcat(saturation)
        paint.colorFilter = ColorMatrixColorFilter(contrast)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Apply a 4×5 color matrix to one packed ARGB pixel — pure JVM math for unit tests.
     */
    fun applyColorMatrixArgb(argb: Int, matrix: FloatArray): Int {
        require(matrix.size == 20) { "ColorMatrix requires 20 floats" }
        val a = ((argb ushr 24) and 0xFF).toFloat()
        val r = ((argb ushr 16) and 0xFF).toFloat()
        val g = ((argb ushr 8) and 0xFF).toFloat()
        val b = (argb and 0xFF).toFloat()
        fun row(i: Int): Int =
            (matrix[i] * r + matrix[i + 1] * g + matrix[i + 2] * b + matrix[i + 3] * a + matrix[i + 4])
                .roundToInt()
                .coerceIn(0, 255)
        val nr = row(0)
        val ng = row(5)
        val nb = row(10)
        val na = row(15)
        return (na shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    private fun edgeFillColor(source: Bitmap): Int {
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return Color.GRAY
        val samples = intArrayOf(
            source.getPixel(0, 0),
            source.getPixel(w - 1, 0),
            source.getPixel(0, h - 1),
            source.getPixel(w - 1, h - 1)
        )
        var r = 0
        var g = 0
        var b = 0
        for (c in samples) {
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
        }
        val n = samples.size
        return Color.rgb(r / n, g / n, b / n)
    }

    /** Center source on a square canvas (max side). Pure size helper for tests. */
    fun squarePadSide(width: Int, height: Int): Int {
        require(width > 0 && height > 0) { "source size must be positive" }
        return max(width, height)
    }

    /** Center source on a square canvas (max side). */
    fun squarePad(bitmap: Bitmap): Bitmap {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        val side = squarePadSide(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.rgb(248, 248, 252))
        canvas.drawBitmap(
            bitmap,
            (side - bitmap.width) / 2f,
            (side - bitmap.height) / 2f,
            null
        )
        return output
    }

    fun boostSaturation(bitmap: Bitmap, amount: Float): Bitmap {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("Bitmap.copy returned null")
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(amount) })
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
