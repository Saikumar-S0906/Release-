package com.pixijoy.ai.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

data class EditLayer(
    val id: String,
    val name: String,
    val bitmap: Bitmap,
    val opacity: Float = 1f,
    val visible: Boolean = true
)

data class HslAdjustments(
    val hue: Float = 0f,
    val saturation: Float = 1f,
    val lightness: Float = 0f
)

data class CurveAdjustments(
    val shadows: Float = 0f,
    val midtones: Float = 0f,
    val highlights: Float = 0f
)

data class EditKeyframe(
    val timeMs: Int,
    val exposure: Float = 0f,
    val contrast: Float = 1f
)

@Singleton
class AdvancedEditEngine @Inject constructor(
    private val photoEditProcessor: PhotoEditProcessor
) {
    fun compositeLayers(base: Bitmap, layers: List<EditLayer>): Bitmap {
        val output = BitmapUtils.copy(base)
        val canvas = Canvas(output)
        canvas.drawBitmap(base, 0f, 0f, null)
        layers.filter { it.visible }.forEach { layer ->
            val paint = Paint().apply { alpha = (layer.opacity * 255).toInt().coerceIn(0, 255) }
            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
        }
        return output
    }

    fun applyHsl(bitmap: Bitmap, hsl: HslAdjustments): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val satMatrix = ColorMatrix().apply { setSaturation(hsl.saturation.coerceIn(0f, 2f)) }
        val hueDeg = hsl.hue.coerceIn(-1f, 1f) * 180f
        if (kotlin.math.abs(hueDeg) > 0.5f) {
            satMatrix.postConcat(ColorMatrix().apply { setRotate(0, hueDeg) })
            satMatrix.postConcat(ColorMatrix().apply { setRotate(1, hueDeg * 0.35f) })
            satMatrix.postConcat(ColorMatrix().apply { setRotate(2, -hueDeg * 0.2f) })
        }
        val light = hsl.lightness * 40f
        val bright = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, light,
                0f, 1f, 0f, 0f, light,
                0f, 0f, 1f, 0f, light,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(bright)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(satMatrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun applyCurves(bitmap: Bitmap, curves: CurveAdjustments): Bitmap =
        photoEditProcessor.adjustExposureContrast(
            bitmap,
            exposure = curves.midtones * 0.5f + curves.highlights * 0.2f,
            contrast = 1f + curves.shadows * 0.3f + curves.midtones * 0.2f
        )

    fun applyMaskBrush(base: Bitmap, mask: Bitmap, effect: Bitmap): Bitmap {
        val output = BitmapUtils.copy(base)
        val canvas = Canvas(output)
        canvas.drawBitmap(base, 0f, 0f, null)
        val paint = Paint()
        for (y in 0 until base.height step 4) {
            for (x in 0 until base.width step 4) {
                val alpha = android.graphics.Color.alpha(mask.getPixel(x.coerceAtMost(mask.width - 1), y.coerceAtMost(mask.height - 1)))
                if (alpha > 64) {
                    paint.alpha = alpha
                    canvas.drawBitmap(effect, x.toFloat(), y.toFloat(), paint)
                }
            }
        }
        return output
    }

    fun renderKeyframeFrame(base: Bitmap, keyframes: List<EditKeyframe>, timeMs: Int): Bitmap {
        val kf = keyframes.minByOrNull { kotlin.math.abs(it.timeMs - timeMs) } ?: return base
        return photoEditProcessor.adjustExposureContrast(base, kf.exposure, kf.contrast)
    }

    fun buildTimelineFrames(base: Bitmap, keyframes: List<EditKeyframe>, frameCount: Int = 8): List<Bitmap> {
        if (keyframes.isEmpty()) return List(frameCount) { base }
        val maxTime = keyframes.maxOf { it.timeMs }.coerceAtLeast(1)
        return (0 until frameCount).map { index ->
            val t = (index * maxTime / (frameCount - 1).coerceAtLeast(1))
            renderKeyframeFrame(base, keyframes, t)
        }
    }
}
