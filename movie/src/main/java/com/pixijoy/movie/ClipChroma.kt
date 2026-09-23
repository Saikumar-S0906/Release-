package com.pixijoy.movie

import kotlin.math.abs

/**
 * Clip-level chroma key (D9).
 *
 * [similarity] 0..1 — how close a pixel must be to the key color to punch out.
 * [softness] 0..1 — edge feather width. Keyed pixels become transparent (alpha) and RGB
 * is mixed toward black so single-track export still shows a clear punch-out.
 */
data class ClipChroma(
    val key: ChromaKey = ChromaKey.Off,
    val similarity: Float = 0.4f,
    val softness: Float = 0.12f
) {
    fun normalized(): ClipChroma = copy(
        similarity = similarity.coerceIn(0.05f, 0.95f),
        softness = softness.coerceIn(0f, 0.5f)
    )

    val isIdentity: Boolean get() = key == ChromaKey.Off

    fun encode(): String {
        if (isIdentity) return "none"
        val n = normalized()
        return "${key.encode()}_${fmt(n.similarity)}_${fmt(n.softness)}"
    }

    fun matchesKey(preset: ChromaKey): Boolean = key == preset

    fun withKey(preset: ChromaKey): ClipChroma =
        if (preset == ChromaKey.Off) None else copy(key = preset).normalized()

    fun withSimilarity(value: Float): ClipChroma = copy(similarity = value).normalized()

    fun withSoftness(value: Float): ClipChroma = copy(softness = value).normalized()

    /** RGB key color in linear-ish 0..1 for the shader (null when off). */
    fun keyRgb(): FloatArray? = when (key) {
        ChromaKey.Off -> null
        ChromaKey.Green -> floatArrayOf(0.05f, 0.78f, 0.12f)
        ChromaKey.Blue -> floatArrayOf(0.08f, 0.18f, 0.85f)
    }

    companion object {
        val None = ClipChroma()

        fun decode(raw: String?): ClipChroma {
            if (raw.isNullOrBlank() || raw == "none") return None
            val p = raw.split('_')
            if (p.isEmpty()) return None
            val key = ChromaKey.parse(p[0])
            if (key == ChromaKey.Off) return None
            return ClipChroma(
                key = key,
                similarity = p.getOrNull(1)?.toFloatOrNull() ?: 0.4f,
                softness = p.getOrNull(2)?.toFloatOrNull() ?: 0.12f
            ).normalized()
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

enum class ChromaKey {
    Off,
    Green,
    Blue;

    fun encode(): String = name.lowercase()

    companion object {
        fun parse(raw: String?): ChromaKey =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Off
    }
}

enum class ChromaSimilarityPreset(val value: Float) {
    Tight(0.25f),
    Medium(0.4f),
    Wide(0.6f);

    companion object {
        fun matching(chroma: ClipChroma?): ChromaSimilarityPreset {
            val s = chroma?.normalized()?.similarity ?: 0.4f
            return entries.minBy { abs(it.value - s) }
        }
    }
}
