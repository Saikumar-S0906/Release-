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

enum class TitleRole {
    Title,
    Caption;

    companion object {
        fun parse(raw: String?): TitleRole =
            when (raw?.lowercase()) {
                "caption" -> Caption
                else -> Title
            }
    }
}

/**
 * Timed text layer on the composition (D4). Times are composition milliseconds.
 * [endMs] `< 0` means “through end of timeline” resolved at effect build.
 */
data class TimelineTitle(
    val id: String,
    val text: String,
    val role: TitleRole = TitleRole.Title,
    val startMs: Long = 0,
    val endMs: Long = -1,
    val anchorX: Float = 0f,
    val anchorY: Float = Float.NaN
) {
    fun resolvedEnd(totalMs: Long): Long {
        val end = if (endMs < 0) totalMs else endMs
        return end.coerceAtLeast(startMs + 200L).coerceAtMost(totalMs.coerceAtLeast(startMs + 200L))
    }

    /** NDC Y for layout helpers / future StaticOverlaySettings wiring. */
    fun resolvedAnchorY(): Float =
        if (!anchorY.isNaN()) {
            anchorY.coerceIn(-0.9f, 0.9f)
        } else {
            when (role) {
                TitleRole.Title -> 0.72f
                TitleRole.Caption -> -0.72f
            }
        }

    fun encodeLine(): String =
        "TT,$id,${role.name},$startMs,$endMs,${fmt(anchorX)},${fmt(anchorY)},${text.encodeField()}"

    companion object {
        fun decodeLine(parts: List<String>): TimelineTitle? {
            if (parts.getOrNull(0) != "TT" || parts.size < 8) return null
            return runCatching {
                TimelineTitle(
                    id = parts[1].ifBlank { "t0" },
                    role = TitleRole.parse(parts[2]),
                    startMs = parts[3].toLong().coerceAtLeast(0),
                    endMs = parts[4].toLong(),
                    anchorX = parts[5].toFloat(),
                    anchorY = parts[6].let { raw ->
                        if (raw.equals("nan", ignoreCase = true)) Float.NaN else raw.toFloat()
                    },
                    text = parts.drop(7).joinToString(",").decodeField()
                )
            }.getOrNull()
        }

        fun titleDefault(totalMs: Long, id: String = "title"): TimelineTitle {
            val hold = (totalMs * 0.45f).toLong().coerceIn(1500L, 5000L)
            return TimelineTitle(
                id = id,
                text = "Title",
                role = TitleRole.Title,
                startMs = 0,
                endMs = hold
            )
        }

        fun captionDefault(totalMs: Long, id: String = "caption"): TimelineTitle {
            val hold = (totalMs * 0.45f).toLong().coerceIn(1500L, 5000L)
            return TimelineTitle(
                id = id,
                text = "Caption",
                role = TitleRole.Caption,
                startMs = 0,
                endMs = hold
            )
        }

        private fun fmt(v: Float): String =
            if (v.isNaN()) "nan"
            else String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

object TitleOverlays {
    fun effect(titles: List<TimelineTitle>, totalMs: Long): Effect? {
        val active = titles.filter { it.text.isNotBlank() }
        if (active.isEmpty()) return null
        val overlays = ArrayList<TextureOverlay>(active.size)
        for (title in active) {
            overlays += TimedBitmapTitleOverlay(title, totalMs.coerceAtLeast(1))
        }
        return OverlayEffect(overlays)
    }

    fun spannablePreview(title: TimelineTitle): String = title.text.trim()

    internal fun paintFor(role: TitleRole): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = when (role) {
                TitleRole.Title -> 72f
                TitleRole.Caption -> 54f
            }
            setShadowLayer(8f, 0f, 3f, Color.BLACK)
        }
}

/**
 * Draws title/caption bitmaps. Positioning is baked into a full-frame transparent bitmap
 * so we avoid OverlaySettings APIs that differ across Media3 minors.
 */
private class TimedBitmapTitleOverlay(
    private val title: TimelineTitle,
    totalMs: Long
) : BitmapOverlay() {
    private val endMs = title.resolvedEnd(totalMs)
    private val paint = TitleOverlays.paintFor(title.role)
    private var frameW = 1280
    private var frameH = 720
    private var visible: Bitmap? = null
    private var clear: Bitmap? = null

    override fun configure(videoSize: androidx.media3.common.util.Size) {
        super.configure(videoSize)
        frameW = videoSize.width.coerceAtLeast(2)
        frameH = videoSize.height.coerceAtLeast(2)
        visible?.recycle()
        clear?.recycle()
        visible = render(frameW, frameH, showText = true)
        clear = render(frameW, frameH, showText = false)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val ms = presentationTimeUs / 1000
        val show = ms >= title.startMs && ms < endMs
        return if (show) {
            visible ?: render(frameW, frameH, showText = true).also { visible = it }
        } else {
            clear ?: render(frameW, frameH, showText = false).also { clear = it }
        }
    }

    private fun render(w: Int, h: Int, showText: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        if (!showText) return bmp
        val canvas = Canvas(bmp)
        val y = ((1f - (title.resolvedAnchorY() + 1f) * 0.5f) * h).coerceIn(48f, h - 48f)
        val x = w * 0.5f + title.anchorX.coerceIn(-1f, 1f) * w * 0.45f
        canvas.drawText(title.text.trim().ifBlank { " " }, x, y, paint)
        return bmp
    }
}

private fun String.encodeField(): String =
    replace("%", "%25").replace(",", "%2C").replace("\n", "%0A")

private fun String.decodeField(): String =
    replace("%0A", "\n").replace("%2C", ",").replace("%25", "%")
