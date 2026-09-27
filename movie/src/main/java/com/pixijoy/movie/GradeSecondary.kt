package com.pixijoy.movie

import kotlin.math.abs

/**
 * R8 / coding P3 — secondary qualify + shot-match reference on top of [TimelineGrade].
 *
 * Pipeline order (documented for directors):
 *   1. Show / primary adjust (exposure · contrast · saturation)
 *   2. Look LUT (bundled [MovieLooks])
 *   3. Secondary / power-window qualify (hue + soft window weight in the cube)
 *   4. Technical (identity reserved — import .cube stays photo C1 / future)
 *
 * Scopes meters are **display-referred** approximations from the grade matrix,
 * not waveform parade from decoded frames.
 */
data class GradeSecondary(
    val enabled: Boolean = false,
    val exposure: Float = 0f,
    val saturation: Float = 1f,
    /** Degrees 0..360. */
    val hueCenter: Float = 30f,
    /** Degrees width of qualify. */
    val hueWidth: Float = 60f,
    /** Soft power window in NDC-ish −1..1 (center + half extents). */
    val cx: Float = 0f,
    val cy: Float = 0f,
    val halfW: Float = 0.55f,
    val halfH: Float = 0.55f,
    val soft: Float = 0.25f
) {
    fun normalized(): GradeSecondary = copy(
        exposure = exposure.coerceIn(-1.5f, 1.5f),
        saturation = saturation.coerceIn(0.2f, 2f),
        hueCenter = ((hueCenter % 360f) + 360f) % 360f,
        hueWidth = hueWidth.coerceIn(10f, 180f),
        cx = cx.coerceIn(-1f, 1f),
        cy = cy.coerceIn(-1f, 1f),
        halfW = halfW.coerceIn(0.1f, 1f),
        halfH = halfH.coerceIn(0.1f, 1f),
        soft = soft.coerceIn(0.05f, 0.8f)
    )

    fun isIdentity(): Boolean {
        val n = normalized()
        return !n.enabled || (
            abs(n.exposure) < 0.01f && abs(n.saturation - 1f) < 0.01f
            )
    }

    fun encodeLine(): String {
        val n = normalized()
        return "GS,${if (n.enabled) 1 else 0},${fmt(n.exposure)},${fmt(n.saturation)}," +
            "${fmt(n.hueCenter)},${fmt(n.hueWidth)},${fmt(n.cx)},${fmt(n.cy)}," +
            "${fmt(n.halfW)},${fmt(n.halfH)},${fmt(n.soft)}"
    }

    companion object {
        val Off = GradeSecondary()

        fun decodeLine(parts: List<String>): GradeSecondary? {
            if (parts.getOrNull(0) != "GS" || parts.size < 11) return null
            return runCatching {
                GradeSecondary(
                    enabled = parts[1] == "1",
                    exposure = parts[2].toFloat(),
                    saturation = parts[3].toFloat(),
                    hueCenter = parts[4].toFloat(),
                    hueWidth = parts[5].toFloat(),
                    cx = parts[6].toFloat(),
                    cy = parts[7].toFloat(),
                    halfW = parts[8].toFloat(),
                    halfH = parts[9].toFloat(),
                    soft = parts[10].toFloat()
                ).normalized()
            }.getOrNull()
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

/** Still-reference mean RGB/luma for shot match (−1 = unset). */
data class GradeMatchRef(
    val r: Float = -1f,
    val g: Float = -1f,
    val b: Float = -1f,
    val luma: Float = -1f
) {
    fun isSet(): Boolean = r >= 0f && g >= 0f && b >= 0f

    fun encodeLine(): String = "GM,${fmt(r)},${fmt(g)},${fmt(b)},${fmt(luma)}"

    companion object {
        val Unset = GradeMatchRef()

        fun decodeLine(parts: List<String>): GradeMatchRef? {
            if (parts.getOrNull(0) != "GM" || parts.size < 5) return null
            return runCatching {
                GradeMatchRef(
                    r = parts[1].toFloat(),
                    g = parts[2].toFloat(),
                    b = parts[3].toFloat(),
                    luma = parts[4].toFloat()
                )
            }.getOrNull()
        }

        fun fromMeters(m: ScopeMeters): GradeMatchRef =
            GradeMatchRef(m.red, m.green, m.blue, m.luma)

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

object ShotMatch {
    /**
     * Suggest primary exposure/sat nudge so current meters approach [ref].
     * Heuristic — not neural.
     */
    fun suggest(current: TimelineGrade, ref: GradeMatchRef): TimelineGrade {
        if (!ref.isSet()) return current
        val meters = MovieGrade.scopeMeters(current)
        val lumaDelta = (ref.luma - meters.luma).coerceIn(-0.35f, 0.35f)
        val satTarget = ((ref.r + ref.g + ref.b) / 3f)
        val satNow = ((meters.red + meters.green + meters.blue) / 3f).coerceAtLeast(0.05f)
        val satScale = (satTarget / satNow).coerceIn(0.7f, 1.4f)
        return current.copy(
            exposure = (current.exposure + lumaDelta * 1.2f).coerceIn(-2f, 2f),
            saturation = (current.saturation * satScale).coerceIn(0f, 2f)
        ).normalized()
    }
}

object GradePipeline {
    /** Human-readable order for UI / docs. */
    const val ORDER_LABEL = "Show → Look → Secondary → Technical"

    fun scopesTruthLabel(hdr: Boolean): String =
        if (hdr) "Scopes: display-referred (HDR path)" else "Scopes: display-referred (SDR)"
}

/** Hue of RGB in degrees 0..360. */
internal fun rgbHue(r: Float, g: Float, b: Float): Float {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    if (d < 1e-5f) return 0f
    val h = when (max) {
        r -> ((g - b) / d) % 6f
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    }
    var deg = h * 60f
    if (deg < 0f) deg += 360f
    return deg
}

internal fun hueWeight(hue: Float, center: Float, width: Float): Float {
    var d = abs(hue - center)
    if (d > 180f) d = 360f - d
    val half = width * 0.5f
    if (d >= half) return 0f
    return (1f - d / half).coerceIn(0f, 1f)
}

/** Soft rectangular window weight for a sample UV in −1..1. */
internal fun windowWeight(u: Float, v: Float, sec: GradeSecondary): Float {
    val n = sec.normalized()
    val dx = abs(u - n.cx) / n.halfW
    val dy = abs(v - n.cy) / n.halfH
    val edge = maxOf(dx, dy)
    if (edge <= 1f - n.soft) return 1f
    if (edge >= 1f) return 0f
    return (1f - (edge - (1f - n.soft)) / n.soft).coerceIn(0f, 1f)
}
