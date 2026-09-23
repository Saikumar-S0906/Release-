package com.pixijoy.movie

import android.graphics.Matrix
import androidx.media3.common.Effect
import androidx.media3.effect.Crop
import androidx.media3.effect.MatrixTransformation
import kotlin.math.abs

/**
 * Clip-level stabilize + reframe (D8).
 *
 * [stabilize] 0..1 applies a constant digital zoom (crop-stabilize) so edge wobble is
 * hidden — not gyro / optical-flow ML (that stays an E-pack). [crop*] are NDC edges for
 * Media3 [Crop] (full frame = -1..1).
 */
data class ClipFrame(
    val stabilize: Float = 0f,
    val cropLeft: Float = -1f,
    val cropRight: Float = 1f,
    val cropBottom: Float = -1f,
    val cropTop: Float = 1f
) {
    fun normalized(): ClipFrame {
        val l = cropLeft.coerceIn(-1.5f, 1.4f)
        val r = cropRight.coerceIn(l + 0.05f, 1.5f)
        val b = cropBottom.coerceIn(-1.5f, 1.4f)
        val t = cropTop.coerceIn(b + 0.05f, 1.5f)
        return copy(
            stabilize = stabilize.coerceIn(0f, 1f),
            cropLeft = l,
            cropRight = r,
            cropBottom = b,
            cropTop = t
        )
    }

    val isIdentity: Boolean
        get() {
            val n = normalized()
            return n.stabilize < 0.01f &&
                abs(n.cropLeft + 1f) < 0.01f &&
                abs(n.cropRight - 1f) < 0.01f &&
                abs(n.cropBottom + 1f) < 0.01f &&
                abs(n.cropTop - 1f) < 0.01f
        }

    val hasCrop: Boolean
        get() {
            val n = normalized()
            return abs(n.cropLeft + 1f) >= 0.01f ||
                abs(n.cropRight - 1f) >= 0.01f ||
                abs(n.cropBottom + 1f) >= 0.01f ||
                abs(n.cropTop - 1f) >= 0.01f
        }

    /** Media3 video effects — Crop then stabilize zoom. */
    fun toEffects(): List<Effect> {
        if (isIdentity) return emptyList()
        val n = normalized()
        val out = ArrayList<Effect>(2)
        if (n.hasCrop) {
            out += Crop(n.cropLeft, n.cropRight, n.cropBottom, n.cropTop)
        }
        if (n.stabilize >= 0.01f) {
            val scale = 1f + n.stabilize * 0.2f
            out += MatrixTransformation {
                Matrix().apply { postScale(scale, scale) }
            }
        }
        return out
    }

    fun encode(): String {
        if (isIdentity) return "none"
        val n = normalized()
        return listOf(n.stabilize, n.cropLeft, n.cropRight, n.cropBottom, n.cropTop)
            .joinToString("_") { fmt(it) }
    }

    fun matchesStabilize(preset: StabilizePreset): Boolean =
        abs(normalized().stabilize - preset.strength) < 0.05f

    fun matchesReframe(preset: ReframePreset): Boolean {
        val p = preset.toFrame().normalized()
        val n = normalized()
        return abs(n.cropLeft - p.cropLeft) < 0.03f &&
            abs(n.cropRight - p.cropRight) < 0.03f &&
            abs(n.cropBottom - p.cropBottom) < 0.03f &&
            abs(n.cropTop - p.cropTop) < 0.03f
    }

    fun withStabilize(preset: StabilizePreset): ClipFrame =
        copy(stabilize = preset.strength).normalized()

    fun withReframe(preset: ReframePreset): ClipFrame {
        val p = preset.toFrame()
        return copy(
            cropLeft = p.cropLeft,
            cropRight = p.cropRight,
            cropBottom = p.cropBottom,
            cropTop = p.cropTop
        ).normalized()
    }

    companion object {
        val None = ClipFrame()

        fun decode(raw: String?): ClipFrame {
            if (raw.isNullOrBlank() || raw == "none") return None
            val p = raw.split('_')
            if (p.size < 5) return None
            return runCatching {
                ClipFrame(
                    stabilize = p[0].toFloat(),
                    cropLeft = p[1].toFloat(),
                    cropRight = p[2].toFloat(),
                    cropBottom = p[3].toFloat(),
                    cropTop = p[4].toFloat()
                ).normalized()
            }.getOrDefault(None)
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

enum class StabilizePreset(val strength: Float) {
    Off(0f),
    Light(0.35f),
    Medium(0.65f),
    Strong(1f);

    companion object {
        fun matching(frame: ClipFrame?): StabilizePreset {
            val s = frame?.normalized()?.stabilize ?: 0f
            return entries.minBy { abs(it.strength - s) }
        }
    }
}

enum class ReframePreset {
    Original,
    Close,
    Left,
    Right,
    Vertical;

    fun toFrame(): ClipFrame = when (this) {
        Original -> ClipFrame.None
        Close -> ClipFrame(cropLeft = -0.55f, cropRight = 0.55f, cropBottom = -0.55f, cropTop = 0.55f)
        Left -> ClipFrame(cropLeft = -1f, cropRight = 0.15f, cropBottom = -1f, cropTop = 1f)
        Right -> ClipFrame(cropLeft = -0.15f, cropRight = 1f, cropBottom = -1f, cropTop = 1f)
        Vertical -> ClipFrame(cropLeft = -0.4f, cropRight = 0.4f, cropBottom = -1f, cropTop = 1f)
    }
}
