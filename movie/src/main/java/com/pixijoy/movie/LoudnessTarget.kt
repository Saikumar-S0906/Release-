package com.pixijoy.movie

/**
 * R8 / coding P4 — selectable loudness gate.
 * Best-effort: maps target LUFS to compressor + soft limiter on [AudioMasterFx].
 * Not a true integrated LUFS meter (no full-file analysis on device).
 */
enum class LoudnessTarget(val lufs: Int) {
    Off(0),
    Streaming(-14),
    Podcast(-16),
    Broadcast(-24);

    fun isOff(): Boolean = this == Off

    fun encodeLine(): String = "LU,$name"

    /**
     * Soft dynamics toward the gate. Soft limiter always on when not [Off].
     * Leaves EQ untouched; replaces dynamics only when previously neutral-ish.
     */
    fun applyMaster(fx: AudioMasterFx): AudioMasterFx = when (this) {
        Off -> fx
        Streaming -> fx.copy(
            compThresholdDb = -18f,
            compRatio = 2.5f,
            compAttackMs = 12f,
            compReleaseMs = 120f,
            compMakeupDb = 2.5f,
            limiterCeilingDb = -1f,
            limiterEnabled = true
        )
        Podcast -> fx.copy(
            compThresholdDb = -20f,
            compRatio = 3f,
            compAttackMs = 10f,
            compReleaseMs = 150f,
            compMakeupDb = 3.5f,
            limiterCeilingDb = -1.5f,
            limiterEnabled = true
        )
        Broadcast -> fx.copy(
            compThresholdDb = -24f,
            compRatio = 4f,
            compAttackMs = 8f,
            compReleaseMs = 200f,
            compMakeupDb = 4.5f,
            limiterCeilingDb = -2f,
            limiterEnabled = true
        )
    }

    /** UI meter stub: shows selected target, not measured integrated loudness. */
    fun meterLabel(): String = when (this) {
        Off -> "Off"
        else -> "−${-lufs} LUFS target"
    }

    companion object {
        fun decodeLine(parts: List<String>): LoudnessTarget? {
            if (parts.getOrNull(0) != "LU" || parts.size < 2) return null
            return runCatching { valueOf(parts[1]) }.getOrNull()
        }

        fun matching(fx: AudioMasterFx): LoudnessTarget? =
            entries.filter { !it.isOff() }.firstOrNull { target ->
                val applied = target.applyMaster(AudioMasterFx.Neutral)
                fx.limiterEnabled == applied.limiterEnabled &&
                    kotlin.math.abs(fx.compThresholdDb - applied.compThresholdDb) < 0.5f &&
                    kotlin.math.abs(fx.limiterCeilingDb - applied.limiterCeilingDb) < 0.5f
            }
    }
}
