package com.pixijoy.movie

object MovieFrame {
    const val DEFAULT_FPS = 24f

    fun fps(raw: Float?): Float {
        val value = raw ?: return DEFAULT_FPS
        if (value.isNaN() || value <= 0f) return DEFAULT_FPS
        return value.coerceIn(8f, 120f)
    }

    fun durationMs(fps: Float): Long =
        (1000.0 / fps(fps).toDouble()).toLong().coerceAtLeast(1)

    fun snap(ms: Long, fps: Float, totalMs: Long): Long {
        val end = totalMs.coerceAtLeast(0)
        if (end <= 0L) return 0L
        val step = durationMs(fps)
        val snapped = ((ms + step / 2) / step) * step
        return snapped.coerceIn(0L, end)
    }

    fun step(ms: Long, deltaFrames: Int, fps: Float, totalMs: Long): Long {
        val step = durationMs(fps)
        return snap(ms + deltaFrames.toLong() * step, fps, totalMs)
    }
}
