package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.media3.common.Effect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay

/** Optional burn-in timecode for review packages (P6 / L1). */
object TimecodeOverlays {
    fun effect(enabled: Boolean, totalMs: Long, fps: Int = DeliveryTc.Fps): Effect? {
        if (!enabled) return null
        return OverlayEffect(listOf(TimedTcOverlay(totalMs.coerceAtLeast(1), fps) as TextureOverlay))
    }
}

private class TimedTcOverlay(
    private val totalMs: Long,
    private val fps: Int
) : BitmapOverlay() {
    private var frameW = 1280
    private var frameH = 720
    private var clear: Bitmap? = null
    private var bucket = Long.MIN_VALUE
    private var cached: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        typeface = Typeface.MONOSPACE
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    override fun configure(videoSize: androidx.media3.common.util.Size) {
        super.configure(videoSize)
        frameW = videoSize.width.coerceAtLeast(2)
        frameH = videoSize.height.coerceAtLeast(2)
        paint.textSize = (frameH * 0.045f).coerceIn(28f, 64f)
        clear?.recycle()
        cached?.recycle()
        clear = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        cached = null
        bucket = Long.MIN_VALUE
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val ms = (presentationTimeUs / 1000).coerceIn(0L, totalMs)
        val b = ms / 40L
        if (b != bucket || cached == null) {
            cached?.recycle()
            val bmp = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val tc = DeliveryTc.format(ms, fps)
            canvas.drawText(tc, frameW * 0.04f, frameH * 0.92f, paint)
            cached = bmp
            bucket = b
        }
        return cached ?: clear!!
    }
}
