package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.media3.common.Effect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay

/**
 * R7 / coding P1+P5 — CG compositor lane on the timeline.
 *
 * Heavy Filament / glTF lives in on-demand `:feature_cg` later. Until that
 * module binds a [CgSceneRenderer], [ClassicalCgRenderer] draws lit props
 * so preview and export share one overlay path (not a sticker emoji).
 *
 * P5 classical: Z-order + track-plane insert, matte modes, camera keyframes.
 *
 * Encode (v1): `CG,<id>,<preset>,<startMs>,<endMs>,<x>,<y>,<scale>,<yaw>,<zOrder>,<followTrack>`
 * Encode (v2+): `...,<matte>,<pitch>,<endYaw>,<endScale>`
 */
enum class CgPreset {
    None,
    /** Soft-lit cube prop — classical stand-in for a glTF mesh. */
    PropCube,
    /** Ground plane + shadow catcher sketch for set extension previz. */
    GroundPlane,
    /** Sky dome gradient plate (classical; HDRI later). */
    SkyDome,
    /** Soft dust / atmosphere particles (classical compute stand-in). */
    DustTrail;

    companion object {
        fun parse(raw: String?): CgPreset =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: None
    }
}

/** Plate matte until neural matting pack binds (P5.5 / H6 classical). */
enum class CgMatte {
    None,
    /** Soft elliptical window — stand-in for selfie / subject matte. */
    SoftOval,
    /** Chroma-ish green fringe fade at edges (plate isolation). */
    ChromaEdge,
    /** Hard rectangular crop window. */
    RectWindow;

    companion object {
        fun parse(raw: String?): CgMatte =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: None
    }
}

fun interface CgSceneRenderer {
    fun render(
        preset: CgPreset,
        width: Int,
        height: Int,
        scale: Float,
        yaw: Float
    ): Bitmap
}

object CgHost {
    @Volatile
    var renderer: CgSceneRenderer? = null

    fun active(): CgSceneRenderer = renderer ?: ClassicalCgRenderer

    fun filamentAvailable(): Boolean = renderer != null && renderer !is ClassicalCgRenderer
}

/** Base-APK CG plate — materials via gradient/lighting cue, not Filament JNI. */
object ClassicalCgRenderer : CgSceneRenderer {
    override fun render(preset: CgPreset, width: Int, height: Int, scale: Float, yaw: Float): Bitmap =
        renderFull(preset, width, height, scale, yaw, pitch = 0f, ox = 0f, oy = 0f)

    fun renderFull(
        preset: CgPreset,
        width: Int,
        height: Int,
        scale: Float,
        yaw: Float,
        pitch: Float,
        ox: Float,
        oy: Float
    ): Bitmap {
        val w = width.coerceAtLeast(2)
        val h = height.coerceAtLeast(2)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val dx = ox * w * 0.45f
        val dy = -oy * h * 0.45f
        canvas.translate(dx, dy)
        when (preset) {
            CgPreset.None -> Unit
            CgPreset.PropCube -> drawCube(canvas, w, h, scale, yaw, pitch)
            CgPreset.GroundPlane -> drawGround(canvas, w, h, scale)
            CgPreset.SkyDome -> drawSky(canvas, w, h)
            CgPreset.DustTrail -> drawDust(canvas, w, h, scale, yaw)
        }
        return bmp
    }

    private fun drawSky(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.rgb(40, 70, 120), Color.rgb(180, 200, 230), Color.rgb(240, 200, 140)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawGround(canvas: Canvas, w: Int, h: Int, scale: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val horizon = h * (0.55f - scale * 0.05f)
        paint.shader = LinearGradient(
            0f, horizon, 0f, h.toFloat(),
            Color.rgb(55, 60, 50),
            Color.rgb(25, 28, 22),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, horizon, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
        // Shadow catcher
        paint.color = Color.argb(90, 0, 0, 0)
        canvas.drawOval(
            w * 0.25f,
            h * 0.72f,
            w * 0.75f,
            h * 0.88f,
            paint
        )
    }

    private fun drawDust(canvas: Canvas, w: Int, h: Int, scale: Float, yaw: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = (36 + scale * 40).toInt().coerceIn(24, 96)
        val yawRad = Math.toRadians(yaw.toDouble())
        val drift = kotlin.math.cos(yawRad).toFloat()
        for (i in 0 until count) {
            val t = i / count.toFloat()
            val x = w * (0.15f + t * 0.7f + drift * 0.05f)
            val y = h * (0.2f + ((i * 37) % 100) / 100f * 0.55f)
            val r = (2f + (i % 5)) * scale
            paint.color = Color.argb(40 + (i % 50), 220, 230, 240)
            canvas.drawCircle(x, y, r, paint)
        }
    }

    private fun drawCube(canvas: Canvas, w: Int, h: Int, scale: Float, yaw: Float, pitch: Float) {
        val cx = w * 0.5f
        val cy = h * 0.48f
        val s = (minOf(w, h) * 0.18f * scale).coerceIn(24f, minOf(w, h) * 0.4f)
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
        val cos = kotlin.math.cos(yawRad)
        val sin = kotlin.math.sin(yawRad)
        val cosp = kotlin.math.cos(pitchRad)
        val sinp = kotlin.math.sin(pitchRad)

        fun pt(lx: Float, ly: Float, lz: Float): Pair<Float, Float> {
            val ry = ly * cosp - lz * sinp
            val rz0 = ly * sinp + lz * cosp
            val rx = lx * cos - rz0 * sin
            val rz = lx * sin + rz0 * cos
            val depth = 1f / (2.2f + rz * 0.015f)
            return (cx + rx * s * depth) to (cy + ry * s * depth - rz * s * 0.35f * depth)
        }

        val faces = listOf(
            listOf(pt(-1f, -1f, -1f), pt(1f, -1f, -1f), pt(1f, -1f, 1f), pt(-1f, -1f, 1f)) to Color.rgb(210, 220, 235),
            listOf(pt(-1f, -1f, 1f), pt(1f, -1f, 1f), pt(1f, 1f, 1f), pt(-1f, 1f, 1f)) to Color.rgb(120, 150, 190),
            listOf(pt(1f, -1f, -1f), pt(1f, -1f, 1f), pt(1f, 1f, 1f), pt(1f, 1f, -1f)) to Color.rgb(70, 95, 130)
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        paint.color = Color.argb(70, 0, 0, 0)
        canvas.drawOval(cx - s * 0.9f, cy + s * 0.85f, cx + s * 0.9f, cy + s * 1.15f, paint)
        for ((pts, color) in faces) {
            val path = Path()
            path.moveTo(pts[0].first, pts[0].second)
            for (i in 1 until pts.size) path.lineTo(pts[i].first, pts[i].second)
            path.close()
            paint.color = color
            canvas.drawPath(path, paint)
        }
    }

    fun applyMatte(src: Bitmap, matte: CgMatte): Bitmap {
        if (matte == CgMatte.None) return src
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        when (matte) {
            CgMatte.None -> Unit
            CgMatte.SoftOval -> {
                paint.shader = RadialGradient(
                    w * 0.5f, h * 0.48f,
                    minOf(w, h) * 0.42f,
                    intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            CgMatte.ChromaEdge -> {
                paint.shader = RadialGradient(
                    w * 0.5f, h * 0.5f,
                    minOf(w, h) * 0.55f,
                    intArrayOf(Color.WHITE, Color.argb(200, 255, 255, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.7f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            CgMatte.RectWindow -> {
                paint.shader = null
                paint.color = Color.WHITE
                val m = minOf(w, h) * 0.12f
                canvas.drawRect(m, m, w - m, h - m, paint)
            }
        }
        paint.xfermode = null
        if (src !== out) src.recycle()
        return out
    }
}

data class TimelineCgLayer(
    val id: String,
    val preset: CgPreset = CgPreset.PropCube,
    val startMs: Long = 0,
    val endMs: Long = -1,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val yaw: Float = 25f,
    /** Higher draws later (over video / other CG). Negative = behind props / under titles. */
    val zOrder: Int = 0,
    /** When true, x/y follow active clip track bake (P5.4 track-plane). */
    val followTrack: Boolean = false,
    val matte: CgMatte = CgMatte.None,
    /** Camera pitch degrees (P5.2). */
    val pitch: Float = 0f,
    /** End-of-span yaw for simple camera keyframe (interpolated). */
    val endYaw: Float = Float.NaN,
    /** End-of-span scale for dolly keyframe. */
    val endScale: Float = Float.NaN
) {
    val isActive: Boolean get() = preset != CgPreset.None

    fun resolvedEnd(totalMs: Long): Long {
        val end = if (endMs < 0) totalMs else endMs
        return end.coerceAtLeast(startMs + 200L).coerceAtMost(totalMs.coerceAtLeast(startMs + 200L))
    }

    fun progress(ms: Long, totalMs: Long): Float {
        val end = resolvedEnd(totalMs)
        val span = (end - startMs).coerceAtLeast(1L).toFloat()
        return ((ms - startMs).toFloat() / span).coerceIn(0f, 1f)
    }

    fun yawAt(ms: Long, totalMs: Long): Float {
        val target = if (endYaw.isNaN()) yaw else endYaw
        return yaw + (target - yaw) * progress(ms, totalMs)
    }

    fun scaleAt(ms: Long, totalMs: Long): Float {
        val target = if (endScale.isNaN()) scale else endScale
        return (scale + (target - scale) * progress(ms, totalMs)).coerceIn(0.35f, 2.5f)
    }

    fun normalized(): TimelineCgLayer = copy(
        scale = scale.coerceIn(0.35f, 2.5f),
        x = x.coerceIn(-0.95f, 0.95f),
        y = y.coerceIn(-0.95f, 0.95f),
        yaw = yaw.coerceIn(-180f, 180f),
        pitch = pitch.coerceIn(-60f, 60f),
        endYaw = if (endYaw.isNaN()) endYaw else endYaw.coerceIn(-180f, 180f),
        endScale = if (endScale.isNaN()) endScale else endScale.coerceIn(0.35f, 2.5f),
        zOrder = zOrder.coerceIn(-8, 8)
    )

    fun encodeLine(): String {
        val n = normalized()
        val ey = if (n.endYaw.isNaN()) "nan" else fmt(n.endYaw)
        val es = if (n.endScale.isNaN()) "nan" else fmt(n.endScale)
        return "CG,${n.id},${n.preset.name},${n.startMs},${n.endMs}," +
            "${fmt(n.x)},${fmt(n.y)},${fmt(n.scale)},${fmt(n.yaw)},${n.zOrder}," +
            "${if (n.followTrack) 1 else 0},${n.matte.name},${fmt(n.pitch)},$ey,$es"
    }

    companion object {
        fun decodeLine(parts: List<String>): TimelineCgLayer? {
            if (parts.getOrNull(0) != "CG" || parts.size < 11) return null
            return runCatching {
                TimelineCgLayer(
                    id = parts[1].ifBlank { "cg0" },
                    preset = CgPreset.parse(parts[2]),
                    startMs = parts[3].toLong().coerceAtLeast(0),
                    endMs = parts[4].toLong(),
                    x = parts[5].toFloat(),
                    y = parts[6].toFloat(),
                    scale = parts[7].toFloat(),
                    yaw = parts[8].toFloat(),
                    zOrder = parts[9].toInt(),
                    followTrack = parts[10] == "1",
                    matte = CgMatte.parse(parts.getOrNull(11)),
                    pitch = parts.getOrNull(12)?.toFloatOrNull() ?: 0f,
                    endYaw = parts.getOrNull(13)?.let { if (it == "nan") Float.NaN else it.toFloat() }
                        ?: Float.NaN,
                    endScale = parts.getOrNull(14)?.let { if (it == "nan") Float.NaN else it.toFloat() }
                        ?: Float.NaN
                ).normalized()
            }.getOrNull()
        }

        fun propDefault(totalMs: Long, id: String = "cg0"): TimelineCgLayer =
            TimelineCgLayer(
                id = id,
                preset = CgPreset.PropCube,
                endMs = totalMs.coerceAtLeast(1000),
                scale = 1.1f,
                yaw = 28f
            )

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

object CgOverlays {
    fun effect(
        layers: List<TimelineCgLayer>,
        totalMs: Long,
        trackAnchor: Pair<Float, Float>? = null
    ): Effect? {
        val active = layers.filter { it.isActive }.sortedBy { it.zOrder }
        if (active.isEmpty()) return null
        val overlays = ArrayList<TextureOverlay>(active.size)
        for (layer in active) {
            overlays += TimedCgOverlay(layer, totalMs.coerceAtLeast(1), trackAnchor)
        }
        return OverlayEffect(overlays)
    }
}

private class TimedCgOverlay(
    private val layer: TimelineCgLayer,
    private val totalMs: Long,
    private val trackAnchor: Pair<Float, Float>?
) : BitmapOverlay() {
    private val endMs = layer.resolvedEnd(totalMs)
    private var frameW = 1280
    private var frameH = 720
    private var visible: Bitmap? = null
    private var clear: Bitmap? = null
    private var cacheBucket = Long.MIN_VALUE

    override fun configure(videoSize: androidx.media3.common.util.Size) {
        super.configure(videoSize)
        frameW = videoSize.width.coerceAtLeast(2)
        frameH = videoSize.height.coerceAtLeast(2)
        visible?.recycle()
        clear?.recycle()
        visible = null
        clear = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        cacheBucket = Long.MIN_VALUE
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val ms = presentationTimeUs / 1000
        val show = ms >= layer.startMs && ms < endMs
        if (!show) {
            return clear ?: Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888).also { clear = it }
        }
        val bucket = ms / 40L
        if (bucket != cacheBucket || visible == null) {
            visible?.recycle()
            visible = renderAt(ms)
            cacheBucket = bucket
        }
        return visible!!
    }

    private fun renderAt(ms: Long): Bitmap {
        val (ox, oy) = if (layer.followTrack && trackAnchor != null) {
            val nx = (trackAnchor.first * 2f - 1f).coerceIn(-0.95f, 0.95f)
            val ny = (1f - trackAnchor.second * 2f).coerceIn(-0.95f, 0.95f)
            (layer.x + nx * 0.35f) to (layer.y + ny * 0.35f)
        } else {
            layer.x to layer.y
        }
        val yaw = layer.yawAt(ms, totalMs)
        val scale = layer.scaleAt(ms, totalMs)
        val host = CgHost.active()
        val raw = if (host is ClassicalCgRenderer) {
            host.renderFull(layer.preset, frameW, frameH, scale, yaw, layer.pitch, ox, oy)
        } else {
            val base = host.render(layer.preset, frameW, frameH, scale, yaw)
            // Bake NDC offset for non-classical renderers
            if (ox == 0f && oy == 0f) base else offsetBitmap(base, ox, oy)
        }
        return ClassicalCgRenderer.applyMatte(raw, layer.matte)
    }

    private fun offsetBitmap(src: Bitmap, ox: Float, oy: Float): Bitmap {
        val out = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.translate(ox * frameW * 0.45f, -oy * frameH * 0.45f)
        canvas.drawBitmap(src, 0f, 0f, null)
        if (src !== out) src.recycle()
        return out
    }
}
