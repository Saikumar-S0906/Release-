package com.pixijoy.ai.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.pixijoy.ai.domain.model.AvatarStyle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarStyleProcessor @Inject constructor() {

    fun apply(bitmap: Bitmap, style: AvatarStyle): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrixFor(style))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        if (style == AvatarStyle.SKETCH) {
            drawSketchOverlay(canvas, bitmap)
        }
        return output
    }

    private fun matrixFor(style: AvatarStyle): ColorMatrix = when (style) {
        AvatarStyle.CARTOON -> ColorMatrix(
            floatArrayOf(
                1.4f, 0f, 0f, 0f, -20f,
                0f, 1.4f, 0f, 0f, -20f,
                0f, 0f, 1.4f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        AvatarStyle.ANIME -> ColorMatrix(
            floatArrayOf(
                1.2f, 0.1f, 0f, 0f, 10f,
                0f, 1.1f, 0.1f, 0f, 10f,
                0.1f, 0f, 1.3f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        AvatarStyle.SKETCH -> ColorMatrix().apply { setSaturation(0f) }
        AvatarStyle.GLOW -> ColorMatrix(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, 30f,
                0f, 1.1f, 0f, 0f, 20f,
                0f, 0f, 1.2f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun drawSketchOverlay(canvas: Canvas, source: Bitmap) {
        val step = 6
        val paint = Paint().apply {
            color = android.graphics.Color.argb(40, 0, 0, 0)
            strokeWidth = 1f
        }
        for (y in 0 until source.height step step) {
            for (x in 0 until source.width step step) {
                val pixel = source.getPixel(x.coerceAtMost(source.width - 1), y.coerceAtMost(source.height - 1))
                val lum = android.graphics.Color.red(pixel) * 0.3 + android.graphics.Color.green(pixel) * 0.59 + android.graphics.Color.blue(pixel) * 0.11
                if (lum < 110) canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }
    }
}

@Singleton
class TalkingPhotoAnimator @Inject constructor() {

    /**
     * Ken Burns path: interpolate [startScale]/[endScale] and pan (0–1 focal point).
     * Returns an empty list if the source is recycled, tiny, or hardware-copy fails.
     */
    fun animate(
        bitmap: Bitmap,
        frames: Int = 16,
        startScale: Float = 1.05f,
        endScale: Float = 1.22f,
        startPanX: Float = 0.5f,
        startPanY: Float = 0.5f,
        endPanX: Float = 0.5f,
        endPanY: Float = 0.5f
    ): List<Bitmap> {
        if (bitmap.isRecycled) return emptyList()
        if (bitmap.width < 16 || bitmap.height < 16) return emptyList()
        val src = softwareCopy(bitmap) ?: return emptyList()
        val n = frames.coerceIn(2, 24)
        return try {
            (0 until n).map { index ->
                val t = index / (n - 1).toFloat()
                val scale = lerp(startScale, endScale, t).coerceIn(1.01f, 2.4f)
                val panX = lerp(startPanX, endPanX, t).coerceIn(0f, 1f)
                val panY = lerp(startPanY, endPanY, t).coerceIn(0f, 1f)
                cropWindow(src, scale, panX, panY)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            if (src !== bitmap && !src.isRecycled) {
                runCatching { src.recycle() }
            }
        }
    }

    private fun softwareCopy(bitmap: Bitmap): Bitmap? {
        if (bitmap.isRecycled) return null
        val config = bitmap.config
        if (config != null && config != Bitmap.Config.HARDWARE) {
            return if (config == Bitmap.Config.ARGB_8888) bitmap
            else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun cropWindow(src: Bitmap, scale: Float, panX: Float, panY: Float): Bitmap {
        val cropW = (src.width / scale).toInt().coerceIn(1, src.width)
        val cropH = (src.height / scale).toInt().coerceIn(1, src.height)
        val maxX = (src.width - cropW).coerceAtLeast(0)
        val maxY = (src.height - cropH).coerceAtLeast(0)
        val x = (panX * maxX).toInt().coerceIn(0, maxX)
        val y = (panY * maxY).toInt().coerceIn(0, maxY)
        return Bitmap.createBitmap(src, x, y, cropW, cropH)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
