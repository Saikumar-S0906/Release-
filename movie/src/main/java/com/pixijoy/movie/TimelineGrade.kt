package com.pixijoy.movie

import androidx.media3.common.Effect
import androidx.media3.effect.SingleColorLut
import kotlin.math.pow

/**
 * Timeline-level grade + bundled look (D5). Applied on composition preview/export.
 */
data class TimelineGrade(
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val lookKey: String = MovieLooks.ORIGINAL
) {
    fun normalized(): TimelineGrade = copy(
        exposure = exposure.coerceIn(-2f, 2f),
        contrast = contrast.coerceIn(0.5f, 1.8f),
        saturation = saturation.coerceIn(0f, 2f),
        lookKey = lookKey.ifBlank { MovieLooks.ORIGINAL }
    )

    fun isNeutral(): Boolean {
        val n = normalized()
        return kotlin.math.abs(n.exposure) < 0.01f &&
            kotlin.math.abs(n.contrast - 1f) < 0.01f &&
            kotlin.math.abs(n.saturation - 1f) < 0.01f &&
            MovieLooks.isIdentity(n.lookKey)
    }

    fun encodeLine(): String {
        val n = normalized()
        return "G,${fmt(n.exposure)},${fmt(n.contrast)},${fmt(n.saturation)},${n.lookKey}"
    }

    companion object {
        val Neutral = TimelineGrade()

        fun decodeLine(parts: List<String>): TimelineGrade? {
            if (parts.getOrNull(0) != "G" || parts.size < 5) return null
            return runCatching {
                TimelineGrade(
                    exposure = parts[1].toFloat(),
                    contrast = parts[2].toFloat(),
                    saturation = parts[3].toFloat(),
                    lookKey = parts[4].ifBlank { MovieLooks.ORIGINAL }
                ).normalized()
            }.getOrNull()
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

/** Bundled look keys mirrored from photo Looks (movie-local to avoid :photo cycle). */
object MovieLooks {
    const val ORIGINAL = "original"
    const val CLEAN = "clean"
    const val WARM = "warm"
    const val COOL = "cool"
    const val FILM = "film"
    const val NIGHT = "night"

    val keys: List<String> = listOf(ORIGINAL, CLEAN, WARM, COOL, FILM, NIGHT)

    fun isIdentity(key: String): Boolean =
        key.isBlank() || key.equals(ORIGINAL, ignoreCase = true)

    fun colorMatrix(key: String): FloatArray = when (key.lowercase()) {
        CLEAN -> floatArrayOf(
            1.08f, -0.02f, -0.02f, 0f, 6f,
            -0.02f, 1.08f, -0.02f, 0f, 6f,
            -0.02f, -0.02f, 1.05f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        )
        WARM -> floatArrayOf(
            1.12f, 0.04f, 0f, 0f, 8f,
            0.02f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.92f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f
        )
        COOL -> floatArrayOf(
            0.92f, 0f, 0.04f, 0f, -4f,
            0f, 1.02f, 0.04f, 0f, 2f,
            0.04f, 0.04f, 1.14f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
        FILM -> floatArrayOf(
            1.05f, 0.06f, 0.02f, 0f, 4f,
            0.02f, 1.00f, 0.04f, 0f, 2f,
            0.04f, 0.08f, 0.95f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f
        )
        NIGHT -> floatArrayOf(
            0.78f, 0.02f, 0.08f, 0f, -12f,
            0f, 0.82f, 0.10f, 0f, -8f,
            0.06f, 0.04f, 1.10f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )
        else -> identityMatrix()
    }

    fun identityMatrix(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

object MovieGrade {
    fun colorMatrix(grade: TimelineGrade): FloatArray {
        val n = grade.normalized()
        val adjust = adjustMatrix(n.exposure, n.contrast, n.saturation)
        val look = MovieLooks.colorMatrix(n.lookKey)
        // look applied after adjust (look × adjust)
        return concat(look, adjust)
    }

    fun effect(grade: TimelineGrade): Effect? {
        if (grade.isNeutral()) return null
        val matrix = colorMatrix(grade)
        val cube = matrixToCube(matrix, size = 17)
        return SingleColorLut.createFromCube(cube)
    }

    /**
     * Scope meters 0..1 for R/G/B/Luma from sampling mid-gray + primaries through the grade.
     * Lightweight monitoring UI — not a frame parade.
     */
    fun scopeMeters(grade: TimelineGrade): ScopeMeters {
        val m = colorMatrix(grade)
        val mid = apply(m, 0.5f, 0.5f, 0.5f)
        val red = apply(m, 1f, 0f, 0f)
        val green = apply(m, 0f, 1f, 0f)
        val blue = apply(m, 0f, 0f, 1f)
        val luma = 0.2126f * mid[0] + 0.7152f * mid[1] + 0.0722f * mid[2]
        return ScopeMeters(
            red = red[0].coerceIn(0f, 1f),
            green = green[1].coerceIn(0f, 1f),
            blue = blue[2].coerceIn(0f, 1f),
            luma = luma.coerceIn(0f, 1f)
        )
    }

    private fun adjustMatrix(exposure: Float, contrast: Float, saturation: Float): FloatArray {
        val gain = 2.0.pow(exposure.toDouble()).toFloat()
        val translate = (1f - contrast) * 0.18f * 255f
        val s = contrast * gain
        val grade = floatArrayOf(
            s, 0f, 0f, 0f, translate,
            0f, s, 0f, 0f, translate,
            0f, 0f, s, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        return concat(saturationMatrix(saturation), grade)
    }

    private fun saturationMatrix(saturation: Float): FloatArray {
        val s = saturation.coerceAtLeast(0f)
        val r = 0.213f * (1f - s)
        val g = 0.715f * (1f - s)
        val b = 0.072f * (1f - s)
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Result = a × b, so b is applied first. */
    internal fun concat(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (i in 0..3) {
            val a0 = a[i * 5]
            val a1 = a[i * 5 + 1]
            val a2 = a[i * 5 + 2]
            val a3 = a[i * 5 + 3]
            val a4 = a[i * 5 + 4]
            for (j in 0..3) {
                out[i * 5 + j] = a0 * b[j] + a1 * b[5 + j] + a2 * b[10 + j] + a3 * b[15 + j]
            }
            out[i * 5 + 4] = a0 * b[4] + a1 * b[9] + a2 * b[14] + a3 * b[19] + a4
        }
        return out
    }

    internal fun apply(m: FloatArray, r: Float, g: Float, b: Float): FloatArray {
        val R = r * 255f
        val G = g * 255f
        val B = b * 255f
        val outR = (m[0] * R + m[1] * G + m[2] * B + m[4]) / 255f
        val outG = (m[5] * R + m[6] * G + m[7] * B + m[9]) / 255f
        val outB = (m[10] * R + m[11] * G + m[12] * B + m[14]) / 255f
        return floatArrayOf(outR.coerceIn(0f, 1.5f), outG.coerceIn(0f, 1.5f), outB.coerceIn(0f, 1.5f))
    }

    private fun matrixToCube(matrix: FloatArray, size: Int): Array<Array<IntArray>> {
        val n = size.coerceIn(2, 33)
        val cube = Array(n) { Array(n) { IntArray(n) } }
        val denom = (n - 1).toFloat().coerceAtLeast(1f)
        for (ri in 0 until n) {
            for (gi in 0 until n) {
                for (bi in 0 until n) {
                    val rgb = apply(matrix, ri / denom, gi / denom, bi / denom)
                    val r = (rgb[0].coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
                    val g = (rgb[1].coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
                    val b = (rgb[2].coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
                    cube[ri][gi][bi] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
        return cube
    }
}

data class ScopeMeters(
    val red: Float,
    val green: Float,
    val blue: Float,
    val luma: Float
)
