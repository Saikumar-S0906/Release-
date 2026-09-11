package com.pixijoy.ai.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.pixijoy.ai.domain.model.PhotoFilter
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoEditProcessor @Inject constructor() {

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun crop(bitmap: Bitmap, aspectWidth: Float, aspectHeight: Float): Bitmap {
        val targetRatio = aspectWidth / aspectHeight
        val srcRatio = bitmap.width.toFloat() / bitmap.height
        val (w, h, x, y) = if (srcRatio > targetRatio) {
            val h = bitmap.height
            val w = (h * targetRatio).toInt()
            val x = (bitmap.width - w) / 2
            Quintuple(w, h, x, 0)
        } else {
            val w = bitmap.width
            val h = (w / targetRatio).toInt()
            val y = (bitmap.height - h) / 2
            Quintuple(w, h, 0, y)
        }
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    fun cropFree(bitmap: Bitmap, rect: Rect): Bitmap {
        val safe = Rect(
            rect.left.coerceIn(0, bitmap.width - 1),
            rect.top.coerceIn(0, bitmap.height - 1),
            rect.right.coerceIn(1, bitmap.width),
            rect.bottom.coerceIn(1, bitmap.height)
        )
        return Bitmap.createBitmap(bitmap, safe.left, safe.top, safe.width(), safe.height())
    }

    fun resize(bitmap: Bitmap, maxSide: Int): Bitmap = BitmapUtils.downscaleIfNeeded(bitmap, maxSide)

    fun adjustExposureContrast(bitmap: Bitmap, exposure: Float, contrast: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val brightness = exposure * 40f
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f + brightness
        val matrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun applyFilter(bitmap: Bitmap, filter: PhotoFilter): Bitmap {
        if (filter == PhotoFilter.NONE) return BitmapUtils.copy(bitmap)
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val matrix = when (filter) {
            PhotoFilter.VIVID -> {
                ColorMatrix().apply { setSaturation(1.35f) }
            }
            PhotoFilter.WARM -> ColorMatrix(
                floatArrayOf(
                    1.1f, 0f, 0f, 0f, 12f,
                    0f, 1.05f, 0f, 0f, 6f,
                    0f, 0f, 0.9f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.COOL -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 4f,
                    0f, 0f, 1.15f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.BW -> {
                ColorMatrix().apply { setSaturation(0f) }
            }
            PhotoFilter.CINEMATIC -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0.05f, 0.05f, 0f, -8f,
                    0.05f, 0.9f, 0.05f, 0f, -8f,
                    0.05f, 0.05f, 1.1f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.VINTAGE -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0.3f, 0.1f, 0f, 20f,
                    0.2f, 0.8f, 0.1f, 0f, 10f,
                    0.1f, 0.2f, 0.7f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.PORTRAIT -> {
                val sat = ColorMatrix().apply { setSaturation(0.85f) }
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.05f, 0f, 0f, 0f, 8f,
                        0f, 1.02f, 0f, 0f, 4f,
                        0f, 0f, 0.98f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                sat.apply { postConcat(warm) }
            }
            PhotoFilter.AI_GLOW -> {
                ColorMatrix().apply {
                    setSaturation(1.2f)
                    postConcat(
                        ColorMatrix(
                            floatArrayOf(
                                1.08f, 0f, 0f, 0f, 14f,
                                0f, 1.08f, 0f, 0f, 10f,
                                0f, 0f, 1.12f, 0f, 8f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    )
                }
            }
            PhotoFilter.AI_HDR -> {
                ColorMatrix(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, -12f,
                        0f, 1.18f, 0f, 0f, -12f,
                        0f, 0f, 1.18f, 0f, -12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            PhotoFilter.NONE -> ColorMatrix()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private data class Quintuple(val w: Int, val h: Int, val x: Int, val y: Int)
}
