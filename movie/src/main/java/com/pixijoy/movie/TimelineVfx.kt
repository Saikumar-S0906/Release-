package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.media3.common.Effect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * F2 classical particle VFX on the composition. Not neural. Optional [followTrack]
 * steers the emitter toward the active clip's F1 track anchors along the shot
 * (seed-only when Track ML is unbound).
 */
enum class VfxPreset {
    None,
    Sparkle,
    Ember,
    Confetti,
    Snow,
    Bokeh;

    companion object {
        fun parse(raw: String?): VfxPreset =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: None
    }
}

data class TimelineVfx(
    val id: String,
    val preset: VfxPreset = VfxPreset.Sparkle,
    val startMs: Long = 0,
    val endMs: Long = -1,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
    val intensity: Float = 0.65f,
    val followTrack: Boolean = false
) {
    val isActive: Boolean get() = preset != VfxPreset.None

    fun resolvedEnd(totalMs: Long): Long {
        val end = if (endMs < 0) totalMs else endMs
        return end.coerceAtLeast(startMs + 200L).coerceAtMost(totalMs.coerceAtLeast(startMs + 200L))
    }

    fun normalized(): TimelineVfx = copy(
        intensity = intensity.coerceIn(0.1f, 1f),
        anchorX = anchorX.coerceIn(-0.95f, 0.95f),
        anchorY = anchorY.coerceIn(-0.95f, 0.95f)
    )

    fun encodeLine(): String {
        val n = normalized()
        return "VX,${n.id},${n.preset.name},${n.startMs},${n.endMs}," +
            "${fmt(n.anchorX)},${fmt(n.anchorY)},${fmt(n.intensity)},${if (n.followTrack) 1 else 0}"
    }

    companion object {
        fun decodeLine(parts: List<String>): TimelineVfx? {
            if (parts.getOrNull(0) != "VX" || parts.size < 9) return null
            return runCatching {
                TimelineVfx(
                    id = parts[1].ifBlank { "vx0" },
                    preset = VfxPreset.parse(parts[2]),
                    startMs = parts[3].toLong().coerceAtLeast(0),
                    endMs = parts[4].toLong(),
                    anchorX = parts[5].toFloat(),
                    anchorY = parts[6].toFloat(),
                    intensity = parts[7].toFloat(),
                    followTrack = parts[8] == "1"
                ).normalized()
            }.getOrNull()
        }

        fun sparkleDefault(totalMs: Long, id: String = "vx0"): TimelineVfx =
            TimelineVfx(
                id = id,
                preset = VfxPreset.Sparkle,
                startMs = 0,
                endMs = totalMs.coerceAtLeast(1000),
                intensity = 0.7f
            )

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

object VfxOverlays {
    /**
     * @param trackAnchors optional F1 image-space anchors over composition time.
     * Converted to NDC-ish overlay coords when [TimelineVfx.followTrack] is set.
     */
    fun effect(
        layers: List<TimelineVfx>,
        totalMs: Long,
        trackAnchors: TrackAnchorResolver = TrackAnchorResolver.None
    ): Effect? {
        val active = layers.filter { it.isActive }
        if (active.isEmpty()) return null
        val overlays = ArrayList<TextureOverlay>(active.size)
        for (layer in active) {
            overlays += ParticleVfxOverlay(layer, totalMs.coerceAtLeast(1), trackAnchors)
        }
        return OverlayEffect(overlays)
    }
}

private class ParticleVfxOverlay(
    private val layer: TimelineVfx,
    totalMs: Long,
    private val trackAnchors: TrackAnchorResolver
) : BitmapOverlay() {
    private val endMs = layer.resolvedEnd(totalMs)
    private var frameW = 1280
    private var frameH = 720
    private var clear: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = ArrayList<Particle>()
    private var seeded = false
    private var lastEx = 0f
    private var lastEy = 0f

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var size: Float,
        var color: Int
    )

    override fun configure(videoSize: androidx.media3.common.util.Size) {
        super.configure(videoSize)
        frameW = videoSize.width.coerceAtLeast(2)
        frameH = videoSize.height.coerceAtLeast(2)
        clear?.recycle()
        clear = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        seeded = false
        particles.clear()
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val ms = presentationTimeUs / 1000
        if (ms < layer.startMs || ms >= endMs) {
            return clear ?: Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
                .also { clear = it }
        }
        ensureSeeded(ms)
        val localT = ((ms - layer.startMs).toFloat() / (endMs - layer.startMs).coerceAtLeast(1))
            .coerceIn(0f, 1f)
        step(localT, ms)
        val out = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        for (p in particles) {
            if (p.life <= 0f) continue
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            paint.color = Color.argb(
                (Color.alpha(p.color) * a).toInt().coerceIn(0, 255),
                Color.red(p.color),
                Color.green(p.color),
                Color.blue(p.color)
            )
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
        return out
    }

    private fun ensureSeeded(compositionMs: Long) {
        if (!seeded) {
            seeded = true
            val count = (48 + layer.intensity * 80).toInt().coerceIn(24, 160)
            val rng = Random(layer.id.hashCode() xor layer.preset.hashCode())
            val (cx, cy) = emitterPx(compositionMs)
            lastEx = cx
            lastEy = cy
            repeat(count) {
                particles += spawn(rng, cx, cy)
            }
            return
        }
        if (!layer.followTrack) return
        val (cx, cy) = emitterPx(compositionMs)
        val dx = cx - lastEx
        val dy = cy - lastEy
        if (dx == 0f && dy == 0f) return
        lastEx = cx
        lastEy = cy
        for (p in particles) {
            p.x += dx
            p.y += dy
        }
    }

    private fun emitterPx(compositionMs: Long = 0L): Pair<Float, Float> {
        val nx: Float
        val ny: Float
        if (layer.followTrack) {
            val anchor = trackAnchors.at(compositionMs)
            if (anchor != null) {
                val ndc = TrackAttach.vfxNdc(anchor.first, anchor.second)
                nx = ndc.first
                ny = ndc.second
            } else {
                nx = layer.anchorX
                ny = layer.anchorY
            }
        } else {
            nx = layer.anchorX
            ny = layer.anchorY
        }
        val x = (frameW * 0.5f) + nx * (frameW * 0.45f)
        val y = (frameH * 0.5f) - ny * (frameH * 0.45f)
        return x to y
    }

    private fun spawn(rng: Random, cx: Float, cy: Float): Particle {
        val life = rng.nextFloat() * 0.8f + 0.4f
        return when (layer.preset) {
            VfxPreset.Sparkle -> Particle(
                x = cx + rng.nextFloat() * 40f - 20f,
                y = cy + rng.nextFloat() * 40f - 20f,
                vx = rng.nextFloat() * 4f - 2f,
                vy = rng.nextFloat() * -3f - 0.5f,
                life = life,
                maxLife = life,
                size = 2f + rng.nextFloat() * 4f * layer.intensity,
                color = Color.argb(220, 255, 240, 180)
            )
            VfxPreset.Ember -> Particle(
                x = cx + rng.nextFloat() * 30f - 15f,
                y = cy + rng.nextFloat() * 20f,
                vx = rng.nextFloat() * 2f - 1f,
                vy = rng.nextFloat() * -4f - 1f,
                life = life,
                maxLife = life,
                size = 3f + rng.nextFloat() * 5f * layer.intensity,
                color = Color.argb(230, 255, 90 + rng.nextInt(80), 20)
            )
            VfxPreset.Confetti -> {
                val hue = rng.nextInt(6)
                val col = when (hue) {
                    0 -> Color.rgb(255, 80, 80)
                    1 -> Color.rgb(80, 200, 255)
                    2 -> Color.rgb(255, 210, 60)
                    3 -> Color.rgb(120, 255, 120)
                    4 -> Color.rgb(255, 120, 220)
                    else -> Color.rgb(255, 255, 255)
                }
                Particle(
                    x = cx + rng.nextFloat() * 60f - 30f,
                    y = cy - 40f,
                    vx = rng.nextFloat() * 6f - 3f,
                    vy = rng.nextFloat() * 2f + 1f,
                    life = life * 1.4f,
                    maxLife = life * 1.4f,
                    size = 3f + rng.nextFloat() * 6f,
                    color = Color.argb(230, Color.red(col), Color.green(col), Color.blue(col))
                )
            }
            VfxPreset.Snow -> Particle(
                x = rng.nextFloat() * frameW,
                y = rng.nextFloat() * frameH * 0.3f,
                vx = rng.nextFloat() * 1.2f - 0.6f,
                vy = 1.2f + rng.nextFloat() * 2.5f * layer.intensity,
                life = life * 2f,
                maxLife = life * 2f,
                size = 2f + rng.nextFloat() * 3f,
                color = Color.argb(200, 240, 245, 255)
            )
            VfxPreset.Bokeh -> {
                val ang = rng.nextFloat() * Math.PI.toFloat() * 2f
                val r = 20f + rng.nextFloat() * 120f * layer.intensity
                Particle(
                    x = cx + cos(ang) * r,
                    y = cy + sin(ang) * r,
                    vx = cos(ang) * 0.4f,
                    vy = sin(ang) * 0.4f,
                    life = life * 1.6f,
                    maxLife = life * 1.6f,
                    size = 8f + rng.nextFloat() * 18f * layer.intensity,
                    color = Color.argb(90, 255, 200, 120)
                )
            }
            VfxPreset.None -> Particle(cx, cy, 0f, 0f, 0f, 1f, 1f, Color.TRANSPARENT)
        }
    }

    private fun step(t: Float, compositionMs: Long) {
        val (cx, cy) = emitterPx(compositionMs)
        val rng = Random((t * 10_000).toInt() xor layer.id.hashCode())
        val gravity = when (layer.preset) {
            VfxPreset.Ember -> -0.08f
            VfxPreset.Confetti, VfxPreset.Snow -> 0.12f
            else -> 0.02f
        }
        for (i in particles.indices) {
            val p = particles[i]
            p.x += p.vx
            p.y += p.vy
            p.vy += gravity
            p.life -= 0.016f
            if (p.life <= 0f || p.y > frameH + 40f || p.x < -40f || p.x > frameW + 40f) {
                particles[i] = spawn(rng, cx, cy)
            }
        }
    }
}
