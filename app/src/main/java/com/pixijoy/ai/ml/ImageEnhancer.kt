package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ImageEnhancer @Inject constructor() {
    fun enhance(bitmap: Bitmap): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                1.12f, 0f, 0f, 0f, 8f,
                0f, 1.12f, 0f, 0f, 8f,
                0f, 0f, 1.12f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val saturationMatrix = ColorMatrix().apply { setSaturation(1.08f) }
        contrastMatrix.postConcat(saturationMatrix)
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return sharpen(output, amount = 0.35f)
    }

    fun sharpen(bitmap: Bitmap, amount: Float = 0.35f): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val result = pixels.copyOf()

        val kernel = arrayOf(
            floatArrayOf(0f, -1f, 0f),
            floatArrayOf(-1f, 5f, -1f),
            floatArrayOf(0f, -1f, 0f)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f
                var g = 0f
                var b = 0f
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val color = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[ky + 1][kx + 1]
                        r += ((color shr 16) and 0xFF) * weight
                        g += ((color shr 8) and 0xFF) * weight
                        b += (color and 0xFF) * weight
                    }
                }
                val original = pixels[y * width + x]
                val or = (original shr 16) and 0xFF
                val og = (original shr 8) and 0xFF
                val ob = original and 0xFF
                val nr = (or + (r - or) * amount).toInt().coerceIn(0, 255)
                val ng = (og + (g - og) * amount).toInt().coerceIn(0, 255)
                val nb = (ob + (b - ob) * amount).toInt().coerceIn(0, 255)
                result[y * width + x] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        val sharpened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(result, 0, width, 0, 0, width, height)
        if (sharpened != bitmap) bitmap.recycle()
        return sharpened
    }
}
