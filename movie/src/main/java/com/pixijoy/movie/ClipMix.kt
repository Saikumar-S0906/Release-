package com.pixijoy.movie

object ClipMix {
    data class Window(val startMs: Long, val endMs: Long)

    fun volume(v: Float): Float = v.coerceIn(0f, 1f)

    fun fadeMs(v: Long, durationMs: Long): Long = v.coerceIn(0L, durationMs.coerceAtLeast(0))

    fun duck(
        tMs: Long,
        windows: List<Window>,
        duck: Float,
        attackMs: Long = 120
    ): Float {
        val floor = volume(duck)
        if (windows.isEmpty() || floor >= 0.999f) return 1f
        val attack = attackMs.coerceAtLeast(0)
        var best = 1f
        for (w in windows) {
            val start = w.startMs
            val end = w.endMs.coerceAtLeast(start)
            if (tMs < start - attack || tMs > end + attack) continue
            val g = when {
                attack > 0 && tMs < start -> {
                    val u = (start - tMs).toFloat() / attack
                    floor + (1f - floor) * u.coerceIn(0f, 1f)
                }
                attack > 0 && tMs > end -> {
                    val u = (tMs - end).toFloat() / attack
                    floor + (1f - floor) * u.coerceIn(0f, 1f)
                }
                else -> floor
            }
            if (g < best) best = g
        }
        return best.coerceIn(0f, 1f)
    }

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
