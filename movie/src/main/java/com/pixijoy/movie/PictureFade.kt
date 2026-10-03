package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Color
import androidx.media3.common.Effect
import androidx.media3.common.OverlaySettings
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import kotlin.math.max

/**
 * Picture half of a Soft cut. Audio fades already exist; this hides the hard cut
 * by fading the outgoing picture to black and the incoming picture up from black
 * across the same window. It is not an overlap of the two shots.
 */
object PictureFade {
    /** Per clip: fade-in ms, fade-out ms. Zero means that side stays fully visible. */
    fun windows(clips: List<TimelineClip>): List<Pair<Long, Long>> {
        val fadeIn = LongArray(clips.size)
        val fadeOut = LongArray(clips.size)
        for (i in 0 until clips.lastIndex) {
            val current = clips[i]
            if (current.transition != ClipTransition.Soft || current.transitionMs <= 0L) continue
            val next = clips[i + 1]
            val ms = current.transitionMs.coerceIn(
                40L,
                minOf(current.durationMs, next.durationMs) / 2
            )
            fadeOut[i] = ms
            fadeIn[i + 1] = ms
        }
        return List(clips.size) { fadeIn[it] to fadeOut[it] }
    }

    /** 0 = the picture, 1 = black. */
    fun blackAmount(timeUs: Long, durationUs: Long, fadeInUs: Long, fadeOutUs: Long): Float {
        if (durationUs <= 0L) return 0f
        val t = timeUs.coerceIn(0L, durationUs)
        var alpha = 0f
        if (fadeInUs > 0L && t < fadeInUs) {
            alpha = max(alpha, 1f - t.toFloat() / fadeInUs.toFloat())
        }
        if (fadeOutUs > 0L) {
            val start = durationUs - fadeOutUs
            if (t > start) {
                alpha = max(alpha, (t - start).toFloat() / fadeOutUs.toFloat())
            }
        }
        return alpha.coerceIn(0f, 1f)
    }

    fun effect(durationMs: Long, fadeInMs: Long, fadeOutMs: Long): Effect? {
        if (fadeInMs <= 0L && fadeOutMs <= 0L) return null
        return OverlayEffect(
            listOf(
                BlackFadeOverlay(
                    durationMs.coerceAtLeast(1),
                    fadeInMs.coerceAtLeast(0),
                    fadeOutMs.coerceAtLeast(0)
                )
            )
        )
    }
}

private class BlackFadeOverlay(
    private val durationMs: Long,
    private val fadeInMs: Long,
    private val fadeOutMs: Long
) : BitmapOverlay() {
    private var frame: Bitmap? = null

    override fun configure(videoSize: androidx.media3.common.util.Size) {
        super.configure(videoSize)
        frame?.recycle()
        frame = black(videoSize.width, videoSize.height)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap =
        frame ?: black(2, 2).also { frame = it }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val alpha = PictureFade.blackAmount(
            presentationTimeUs,
            durationMs * 1000L,
            fadeInMs * 1000L,
            fadeOutMs * 1000L
        )
        return StaticOverlaySettings.Builder().setAlphaScale(alpha).build()
    }

    private fun black(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width.coerceAtLeast(2), height.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.BLACK) }
}
