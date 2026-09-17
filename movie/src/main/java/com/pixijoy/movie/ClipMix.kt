package com.pixijoy.movie

object ClipMix {
    fun volume(v: Float): Float = v.coerceIn(0f, 1f)

    fun fadeMs(v: Long, durationMs: Long): Long = v.coerceIn(0L, durationMs.coerceAtLeast(0))

    fun gain(
        tMs: Long,
        durationMs: Long,
        volume: Float,
        fadeInMs: Long,
        fadeOutMs: Long
    ): Float {
        if (durationMs <= 0L) return 0f
        val vol = volume(volume)
        val t = tMs.coerceIn(0L, durationMs)
        var fadeIn = fadeMs(fadeInMs, durationMs)
        var fadeOut = fadeMs(fadeOutMs, durationMs)
        if (fadeIn + fadeOut > durationMs) {
            val scale = durationMs.toFloat() / (fadeIn + fadeOut)
            fadeIn = (fadeIn * scale).toLong()
            fadeOut = durationMs - fadeIn
        }
        val inG = if (fadeIn <= 0L || t >= fadeIn) 1f else t / fadeIn.toFloat()
        val outStart = durationMs - fadeOut
        val outG = if (fadeOut <= 0L || t <= outStart) 1f else (durationMs - t) / fadeOut.toFloat()
        return (vol * inG * outG).coerceIn(0f, 1f)
    }
}
