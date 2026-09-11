package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.pixijoy.ai.util.BitmapUtils
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Singleton
class ObjectEraser @Inject constructor() {
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    suspend fun removePeople(bitmap: Bitmap): Bitmap {
        val input = InputImage.fromBitmap(bitmap, 0)
        val faces = faceDetector.process(input).await()
        if (faces.isEmpty()) return BitmapUtils.copy(bitmap)
        var output = BitmapUtils.copy(bitmap)
        faces.forEach { face ->
            val box = face.boundingBox
            val expanded = Rect(
                (box.left - box.width() * 0.2f).toInt().coerceAtLeast(0),
                (box.top - box.height() * 0.2f).toInt().coerceAtLeast(0),
                (box.right + box.width() * 0.2f).toInt().coerceAtMost(bitmap.width),
                (box.bottom + box.height() * 0.2f).toInt().coerceAtMost(bitmap.height)
            )
            val next = inpaintRegion(output, expanded)
            if (output != bitmap && output != next) output.recycle()
            output = next
        }
        return output
    }

    fun eraseAtPoint(bitmap: Bitmap, x: Float, y: Float, radiusPx: Float): Bitmap {
        val rect = Rect(
            (x - radiusPx).toInt().coerceAtLeast(0),
            (y - radiusPx).toInt().coerceAtLeast(0),
            (x + radiusPx).toInt().coerceAtMost(bitmap.width),
            (y + radiusPx).toInt().coerceAtMost(bitmap.height)
        )
        return inpaintRegion(bitmap, rect, circular = true, centerX = x, centerY = y, radius = radiusPx)
    }

    /**
     * Selective dodge/burn: [amount] in -1..1 (positive lightens, negative darkens) inside a soft circle.
     */
    fun selectiveAtPoint(bitmap: Bitmap, x: Float, y: Float, radiusPx: Float, amount: Float): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val r = radiusPx.coerceAtLeast(4f)
        val strength = amount.coerceIn(-1f, 1f)
        if (strength == 0f) return output
        val left = (x - r).toInt().coerceAtLeast(0)
        val top = (y - r).toInt().coerceAtLeast(0)
        val right = (x + r).toInt().coerceAtMost(bitmap.width - 1)
        val bottom = (y + r).toInt().coerceAtMost(bitmap.height - 1)
        val target = if (strength > 0f) 255 else 0
        val abs = kotlin.math.abs(strength)
        for (py in top..bottom) {
            for (px in left..right) {
                val dist = hypot(px - x.toDouble(), py - y.toDouble()).toFloat()
                if (dist > r) continue
                val falloff = (1f - dist / r).coerceIn(0f, 1f)
                val t = abs * falloff
                val c = output.getPixel(px, py)
                val nr = (Color.red(c) + (target - Color.red(c)) * t).toInt().coerceIn(0, 255)
                val ng = (Color.green(c) + (target - Color.green(c)) * t).toInt().coerceIn(0, 255)
                val nb = (Color.blue(c) + (target - Color.blue(c)) * t).toInt().coerceIn(0, 255)
                output.setPixel(px, py, Color.argb(Color.alpha(c), nr, ng, nb))
            }
        }
        return output
    }

    fun eraseRegion(bitmap: Bitmap, rect: Rect): Bitmap = inpaintRegion(bitmap, rect)

    private fun inpaintRegion(
        bitmap: Bitmap,
        rect: Rect,
        circular: Boolean = false,
        centerX: Float = 0f,
        centerY: Float = 0f,
        radius: Float = 0f
    ): Bitmap {
        val output = BitmapUtils.copy(bitmap)
        val fillColor = averageBorderColor(bitmap, rect)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        if (circular) {
            canvas.drawCircle(centerX, centerY, radius, paint)
        } else {
            canvas.drawRect(rect, paint)
        }

        // Soft blend ring for less harsh edges
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            alpha = 120
        }
        if (circular) {
            canvas.drawCircle(centerX, centerY, radius * 1.08f, ringPaint)
        }
        return output
    }

    private fun averageBorderColor(bitmap: Bitmap, rect: Rect): Int {
        val pad = 8
        val samples = mutableListOf<Pair<Int, Int>>()
        for (x in max(0, rect.left - pad)..min(bitmap.width - 1, rect.right + pad) step 4) {
            samples += x to max(0, rect.top - pad)
            samples += x to min(bitmap.height - 1, rect.bottom + pad)
        }
        for (y in max(0, rect.top - pad)..min(bitmap.height - 1, rect.bottom + pad) step 4) {
            samples += max(0, rect.left - pad) to y
            samples += min(bitmap.width - 1, rect.right + pad) to y
        }
        var r = 0; var g = 0; var b = 0; var count = 0
        samples.distinct().forEach { (x, y) ->
            if (hypot((x - rect.centerX()).toDouble(), (y - rect.centerY()).toDouble()) > rect.width() / 2.0 + pad) {
                val pixel = bitmap.getPixel(x, y)
                r += Color.red(pixel); g += Color.green(pixel); b += Color.blue(pixel)
                count++
            }
        }
        if (count == 0) return Color.GRAY
        return Color.rgb(r / count, g / count, b / count)
    }
}
