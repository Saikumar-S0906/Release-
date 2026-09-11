package com.pixijoy.ai.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.pixijoy.ai.R
import com.pixijoy.ai.util.CanvasFontHelper
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class QuoteCardComposer @Inject constructor() {

    private val gradients = listOf(
        intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#EC4899")),
        intArrayOf(Color.parseColor("#0EA5E9"), Color.parseColor("#22D3EE")),
        intArrayOf(Color.parseColor("#F59E0B"), Color.parseColor("#EF4444")),
        intArrayOf(Color.parseColor("#10B981"), Color.parseColor("#3B82F6")),
        intArrayOf(Color.parseColor("#1A1A2E"), Color.parseColor("#7C3AED"))
    )

    fun create(context: Context, quote: String, author: String, gradientIndex: Int = 0): Bitmap {
        val width = 1080
        val height = 1080
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val colors = gradients[gradientIndex % gradients.size]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors[0], colors[1],
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            typeface = CanvasFontHelper.memeTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        drawWrappedText(canvas, "\"$quote\"", width / 2f, height * 0.4f, width - 120, quotePaint)

        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = 36f
            typeface = CanvasFontHelper.labelTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.author_prefix, author), width / 2f, height * 0.72f, authorPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 150
            textSize = 24f
            typeface = CanvasFontHelper.labelTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.brand_pixijoy), width / 2f, height * 0.9f, brandPaint)
        return output
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Int, paint: Paint) {
        val words = text.split(" ")
        var line = ""
        var y = startY
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(line, x, y, paint)
                line = word
                y += paint.textSize * 1.3f
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, y, paint)
    }
}

@Singleton
class ProfilePicComposer @Inject constructor() {

    fun create(source: Bitmap, frameColor: Int = Color.parseColor("#7C3AED")): Bitmap {
        val size = 1024
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.parseColor("#0D0D0F"))

        val scaled = scaleCenterCrop(source, size - 80, size - 80)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val saveCount = canvas.saveLayer(0f, 0f, size.toFloat(), size.toFloat(), null)

        val center = size / 2f
        val radius = (size - 80) / 2f
        canvas.drawCircle(center, center, radius, Paint().apply { color = Color.WHITE })
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 40f, 40f, circlePaint)
        canvas.restoreToCount(saveCount)
        scaled.recycle()

        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frameColor
            style = Paint.Style.STROKE
            strokeWidth = 12f
        }
        canvas.drawCircle(center, center, radius + 6f, framePaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = frameColor
        }
        canvas.drawCircle(size - 80f, size - 80f, 48f, badgePaint)
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✓", size - 80f, size - 68f, checkPaint)
        return output
    }

    fun createWhatsAppSticker(source: Bitmap): Bitmap {
        val size = 512
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val scaled = scaleCenterCrop(source, size, size)
        canvas.drawBitmap(scaled, 0f, 0f, null)
        scaled.recycle()
        return output
    }

    private fun scaleCenterCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = maxOf(
            targetWidth.toFloat() / source.width,
            targetHeight.toFloat() / source.height
        )
        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val x = (scaledWidth - targetWidth) / 2
        val y = (scaledHeight - targetHeight) / 2
        val cropped = Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
        if (scaled != source) scaled.recycle()
        return cropped
    }
}
