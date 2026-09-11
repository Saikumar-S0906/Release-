package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton

enum class PortraitEffect { RELIGHT, TEETH, HAIR, YOUTHFUL, GLOW }

@Singleton
class PortraitStudioProcessor @Inject constructor() {

    fun apply(bitmap: Bitmap, effect: PortraitEffect, intensity: Float = 1f): Bitmap {
        return when (effect) {
            PortraitEffect.RELIGHT -> applyRelight(bitmap, intensity)
            PortraitEffect.TEETH -> applyTeethWhiten(bitmap, intensity)
            PortraitEffect.HAIR -> applyHairEnhance(bitmap, intensity)
            PortraitEffect.YOUTHFUL -> applyYouthful(bitmap, intensity)
            PortraitEffect.GLOW -> applyGlow(bitmap, intensity)
        }
    }

    fun applyAll(bitmap: Bitmap): Bitmap {
        var result = bitmap
        PortraitEffect.entries.forEach { effect ->
            val next = apply(result, effect, 0.65f)
            if (next != result) result = next
        }
        return result
    }

    private fun applyRelight(bitmap: Bitmap, intensity: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix(
            floatArrayOf(
                1.05f, 0f, 0f, 0f, 12f * intensity,
                0f, 1.02f, 0f, 0f, 8f * intensity,
                0f, 0f, 0.98f, 0f, 4f * intensity,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                bitmap.width * 0.5f, bitmap.height * 0.35f,
                bitmap.width * 0.45f,
                intArrayOf(0x55FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), glow)
        return output
    }

    private fun applyTeethWhiten(bitmap: Bitmap, intensity: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val matrix = ColorMatrix().apply { setSaturation(0.9f + 0.1f * intensity) }
        val brighten = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 18f * intensity,
                0f, 1f, 0f, 0f, 18f * intensity,
                0f, 0f, 1f, 0f, 18f * intensity,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(brighten)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        val mouthTop = (bitmap.height * 0.55f).toInt()
        canvas.save()
        canvas.clipRect(0, mouthTop, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
        canvas.drawBitmap(bitmap, 0f, 0f, Paint().apply { alpha = 180 })
        return output
    }

    private fun applyHairEnhance(bitmap: Bitmap, intensity: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val matrix = ColorMatrix().apply { setSaturation(1.2f + 0.3f * intensity) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        val hairBottom = (bitmap.height * 0.45f).toInt()
        canvas.save()
        canvas.clipRect(0, 0, bitmap.width, hairBottom)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
        canvas.drawBitmap(bitmap, 0f, 0f, Paint().apply { alpha = 200 })
        return output
    }

    private fun applyYouthful(bitmap: Bitmap, intensity: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val matrix = ColorMatrix(
            floatArrayOf(
                1.08f, 0f, 0f, 0f, 10f * intensity,
                0f, 1.06f, 0f, 0f, 8f * intensity,
                0f, 0f, 1.1f, 0f, 12f * intensity,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.setSaturation(1.05f + 0.15f * intensity)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
            isFilterBitmap = true
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applyGlow(bitmap: Bitmap, intensity: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val matrix = ColorMatrix().apply { setSaturation(1.15f + 0.2f * intensity) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
