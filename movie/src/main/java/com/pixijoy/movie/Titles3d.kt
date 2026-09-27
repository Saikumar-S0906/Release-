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

/**
 * F3 3D-style titles. [Titles3dRenderer] may be provided by the on-demand
 * `:feature_titles3d` Filament module. Until that split is installed, [ExtrudedTitleRenderer]
 * draws a bevelled extruded bitmap (still 3D-looking, no Filament in base APK).
 */
fun interface Titles3dRenderer {
    fun render(text: String, width: Int, height: Int, depth: Float): Bitmap
}

object Titles3dHost {
    @Volatile
    var renderer: Titles3dRenderer? = null

    fun active(): Titles3dRenderer = renderer ?: ExtrudedTitleRenderer

    fun filamentAvailable(): Boolean = renderer != null && renderer !is ExtrudedTitleRenderer
}

/** Canvas extruded/bevelled title — base APK fallback (no Filament JNI). */
object ExtrudedTitleRenderer : Titles3dRenderer {
    override fun render(text: String, width: Int, height: Int, depth: Float): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(2), height.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = height * 0.14f
        }
        val cx = width / 2f
        val cy = height * 0.42f
        val layers = (6 + depth * 10).toInt().coerceIn(4, 18)
        for (i in layers downTo 1) {
            paint.color = Color.argb(220, 30, 30, 40)
            canvas.drawText(text, cx + i * 1.6f, cy + i * 1.6f, paint)
        }
        paint.color = Color.WHITE
        paint.setShadowLayer(12f, 0f, 4f, Color.BLACK)
        canvas.drawText(text, cx, cy, paint)
        // Specular edge
        paint.clearShadowLayer()
        paint.color = Color.argb(180, 200, 220, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawText(text, cx - 1f, cy - 1f, paint)
        return bmp
    }
}

data class TimelineTitle3d(
    val id: String,
    val text: String,
    val startMs: Long = 0,
    val endMs: Long = -1,
    val depth: Float = 0.55f,
    val useFilament: Boolean = true
) {
    fun resolvedEnd(totalMs: Long): Long {
        val end = if (endMs < 0) totalMs else endMs
        return end.coerceAtLeast(startMs + 200L).coerceAtMost(totalMs.coerceAtLeast(startMs + 200L))
    }

    fun encodeLine(): String =
        "T3,$id,$startMs,$endMs,${fmt(depth)},${if (useFilament) 1 else 0},${text.encodeField()}"

    companion object {
        fun decodeLine(parts: List<String>): TimelineTitle3d? {
            if (parts.getOrNull(0) != "T3" || parts.size < 7) return null
            return runCatching {
                TimelineTitle3d(
                    id = parts[1].ifBlank { "t3" },
                    startMs = parts[2].toLong().coerceAtLeast(0),
                    endMs = parts[3].toLong(),
                    depth = parts[4].toFloat().coerceIn(0.1f, 1f),
                    useFilament = parts[5] == "1",
                    text = parts.drop(6).joinToString(",").decodeField()
                )
            }.getOrNull()
        }

        fun default(totalMs: Long, id: String = "t3_0"): TimelineTitle3d =
            TimelineTitle3d(id = id, text = "3D Title", endMs = (totalMs * 0.45f).toLong().coerceIn(1500L, 5000L))

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

object Title3dOverlays {
    fun effect(layers: List<TimelineTitle3d>, totalMs: Long): Effect? {
        val active = layers.filter { it.text.isNotBlank() }
        if (active.isEmpty()) return null
        val overlays = ArrayList<TextureOverlay>(active.size)
        for (layer in active) {
            overlays += TimedTitle3dOverlay(layer, totalMs.coerceAtLeast(1))
        }
        return OverlayEffect(overlays)
    }
}

private class TimedTitle3dOverlay(
    private val layer: TimelineTitle3d,
    totalMs: Long
) : BitmapOverlay() {
    private val endMs = layer.resolvedEnd(totalMs)
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
        visible = Titles3dHost.active().render(layer.text, frameW, frameH, layer.depth)
        clear = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val ms = presentationTimeUs / 1000
        val show = ms >= layer.startMs && ms < endMs
        return if (show) {
            visible ?: Titles3dHost.active().render(layer.text, frameW, frameH, layer.depth).also { visible = it }
        } else {
            clear ?: Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888).also { clear = it }
        }
    }
}
