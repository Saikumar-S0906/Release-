package com.pixijoy.movie

/**
 * D6 duck finish presets — applied to the music bed under dialogue / VO.
 */
enum class DuckPreset {
    Soft,
    Medium,
    Hard;

    val duck: Float
        get() = when (this) {
            Soft -> 0.40f
            Medium -> 0.25f
            Hard -> 0.10f
        }

    val attackMs: Long
        get() = when (this) {
            Soft -> 220L
            Medium -> 120L
            Hard -> 80L
        }

    val releaseMs: Long
        get() = when (this) {
            Soft -> 380L
            Medium -> 250L
            Hard -> 150L
        }

    fun apply(bed: AudioBed): AudioBed =
        bed.copy(duck = duck, attackMs = attackMs, releaseMs = releaseMs)

    fun matches(bed: AudioBed): Boolean =
        kotlin.math.abs(bed.duck - duck) < 0.02f &&
            kotlin.math.abs(bed.attackMs - attackMs) < 15L &&
            kotlin.math.abs(bed.releaseMs - releaseMs) < 15L

    companion object {
        fun matching(bed: AudioBed?): DuckPreset? =
            bed?.let { entries.firstOrNull { p -> p.matches(it) } }
    }
}

object AudioFinish {
    /** Expand duck windows slightly so attack/release have room inside composition time. */
    fun polishWindows(
        windows: List<ClipMix.Window>,
        attackMs: Long,
        releaseMs: Long,
        totalMs: Long
    ): List<ClipMix.Window> {
        if (windows.isEmpty()) return windows
        val a = attackMs.coerceAtLeast(0)
        val r = releaseMs.coerceAtLeast(0)
        return windows.map { w ->
            ClipMix.Window(
                startMs = (w.startMs - a / 4).coerceAtLeast(0),
                endMs = (w.endMs + r / 4).coerceAtMost(totalMs.coerceAtLeast(w.endMs))
            )
        }
    }
}
