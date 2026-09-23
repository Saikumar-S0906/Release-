package com.pixijoy.movie

import android.graphics.Matrix
import androidx.media3.effect.MatrixTransformation
import kotlin.math.abs

/**
 * One transform sample on a clip's local 0..1 timeline (D3).
 * [tx]/[ty] are NDC offsets (~-0.25..0.25); [scale] is multiplicative.
 */
data class TransformKeyframe(
    val t: Float,
    val scale: Float = 1f,
    val tx: Float = 0f,
    val ty: Float = 0f,
    val rotationDeg: Float = 0f
) {
    fun normalized(): TransformKeyframe = copy(
        t = t.coerceIn(0f, 1f),
        scale = scale.coerceIn(0.5f, 2.5f),
        tx = tx.coerceIn(-0.35f, 0.35f),
        ty = ty.coerceIn(-0.35f, 0.35f),
        rotationDeg = rotationDeg.coerceIn(-45f, 45f)
    )

    val isIdentity: Boolean
        get() = abs(scale - 1f) < 0.01f &&
            abs(tx) < 0.005f &&
            abs(ty) < 0.005f &&
            abs(rotationDeg) < 0.5f
}

/**
 * Keyframed scale / pan / rotate for a clip. Optional [windowStart]/[windowEnd] remap
 * local 0..1 into a slice of the curve (used when D2 expands speed-ramp steps).
 */
data class ClipMotion(
    val keyframes: List<TransformKeyframe> = emptyList(),
    val windowStart: Float = 0f,
    val windowEnd: Float = 1f
) {
    val isIdentity: Boolean
        get() = keyframes.isEmpty() || keyframes.all { it.isIdentity }

    fun withWindow(t0: Float, t1: Float): ClipMotion =
        copy(windowStart = t0.coerceIn(0f, 1f), windowEnd = t1.coerceIn(0f, 1f))

    fun sample(localT: Float): TransformKeyframe {
        val sorted = keyframes.map { it.normalized() }.sortedBy { it.t }
        if (sorted.isEmpty()) return TransformKeyframe(0f)
        val u = windowStart + (windowEnd - windowStart) * localT.coerceIn(0f, 1f)
        if (u <= sorted.first().t) return sorted.first()
        if (u >= sorted.last().t) return sorted.last()
        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (u in a.t..b.t) {
                val span = (b.t - a.t).coerceAtLeast(1e-4f)
                val w = (u - a.t) / span
                return TransformKeyframe(
                    t = u,
                    scale = a.scale + (b.scale - a.scale) * w,
                    tx = a.tx + (b.tx - a.tx) * w,
                    ty = a.ty + (b.ty - a.ty) * w,
                    rotationDeg = a.rotationDeg + (b.rotationDeg - a.rotationDeg) * w
                )
            }
        }
        return sorted.last()
    }

    /** Media3 effect; [durationUs] is the EditedMediaItem presentation length. */
    fun toMatrixTransformation(durationUs: Long): MatrixTransformation =
        MatrixTransformation { presentationTimeUs ->
            val local = if (durationUs <= 0L) {
                0f
            } else {
                (presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
            }
            val s = sample(local)
            Matrix().apply {
                postScale(s.scale, s.scale)
                postRotate(s.rotationDeg)
                postTranslate(s.tx, s.ty)
            }
        }

    /** Persistable form (no commas). Windows are runtime-only and omitted. */
    fun encode(): String {
        if (isIdentity) return "none"
        return keyframes.joinToString("+") { k ->
            val n = k.normalized()
            listOf(n.t, n.scale, n.tx, n.ty, n.rotationDeg).joinToString("_") { fmt(it) }
        }
    }

    fun matchesPreset(preset: ClipMotionPreset): Boolean =
        encode() == preset.toMotion().encode()

    companion object {
        val None = ClipMotion()

        fun decode(raw: String?): ClipMotion {
            if (raw.isNullOrBlank() || raw == "none") return None
            val keys = raw.split('+').mapNotNull { part ->
                val p = part.split('_')
                if (p.size < 2) return@mapNotNull null
                runCatching {
                    TransformKeyframe(
                        t = p[0].toFloat(),
                        scale = p.getOrNull(1)?.toFloatOrNull() ?: 1f,
                        tx = p.getOrNull(2)?.toFloatOrNull() ?: 0f,
                        ty = p.getOrNull(3)?.toFloatOrNull() ?: 0f,
                        rotationDeg = p.getOrNull(4)?.toFloatOrNull() ?: 0f
                    ).normalized()
                }.getOrNull()
            }
            return if (keys.isEmpty()) None else ClipMotion(keys)
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

enum class ClipMotionPreset {
    None,
    ZoomIn,
    ZoomOut,
    PanLeft,
    PanRight,
    Drift;

    fun toMotion(): ClipMotion = when (this) {
        None -> ClipMotion.None
        ZoomIn -> ClipMotion(
            listOf(
                TransformKeyframe(0f, scale = 1f),
                TransformKeyframe(1f, scale = 1.22f)
            )
        )
        ZoomOut -> ClipMotion(
            listOf(
                TransformKeyframe(0f, scale = 1.22f),
                TransformKeyframe(1f, scale = 1f)
            )
        )
        PanLeft -> ClipMotion(
            listOf(
                TransformKeyframe(0f, scale = 1.12f, tx = 0.12f),
                TransformKeyframe(1f, scale = 1.12f, tx = -0.12f)
            )
        )
        PanRight -> ClipMotion(
            listOf(
                TransformKeyframe(0f, scale = 1.12f, tx = -0.12f),
                TransformKeyframe(1f, scale = 1.12f, tx = 0.12f)
            )
        )
        Drift -> ClipMotion(
            listOf(
                TransformKeyframe(0f, scale = 1.05f, tx = -0.06f, ty = 0.04f),
                TransformKeyframe(1f, scale = 1.18f, tx = 0.08f, ty = -0.05f)
            )
        )
    }
}
