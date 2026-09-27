package com.pixijoy.movie

/**
 * R8 / coding P4 — sync helpers (slip + clap heuristic).
 * Clap detect is a simple amplitude transient finder — documented as best-effort.
 */
object AudioSync {
    const val SlipMinMs = -8000L
    const val SlipMaxMs = 8000L

    fun clampSlip(ms: Long): Long = ms.coerceIn(SlipMinMs, SlipMaxMs)

    /**
     * First sample above [threshold] fraction of full-scale after a quiet run-in.
     * Returns offset in ms from buffer start (0 if none found).
     */
    fun clapOffsetMs(
        pcm16: ShortArray,
        sampleRateHz: Int,
        threshold: Float = 0.4f
    ): Long {
        if (pcm16.isEmpty()) return 0L
        val rate = sampleRateHz.coerceAtLeast(1)
        val thr = (Short.MAX_VALUE * threshold.coerceIn(0.05f, 0.95f)).toInt()
        val quiet = thr / 4
        var i = 0
        while (i < pcm16.size && kotlin.math.abs(pcm16[i].toInt()) < quiet) i++
        while (i < pcm16.size && kotlin.math.abs(pcm16[i].toInt()) < thr) i++
        if (i >= pcm16.size) return 0L
        return (i * 1000L / rate).coerceAtLeast(0L)
    }

    /**
     * Slip so the clap (in VO file time) lands on [pictureBeatMs].
     * Positive slip = delay VO on the timeline.
     */
    fun suggestVoiceSlip(clapInVoiceMs: Long, pictureBeatMs: Long = 0L): Long =
        clampSlip(pictureBeatMs - clapInVoiceMs)
}
