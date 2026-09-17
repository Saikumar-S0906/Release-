package com.pixijoy.movie

object MovieTrim {
    fun normalize(startMs: Long, endMs: Long, durationMs: Long): Pair<Long, Long> {
        val duration = durationMs.coerceAtLeast(250)
        var start = startMs.coerceIn(0, duration - 1)
        var end = endMs.coerceIn(start + 1, duration)
        if (end - start < 250) {
            end = (start + 250).coerceAtMost(duration)
            if (end - start < 250) start = (end - 250).coerceAtLeast(0)
        }
        return start to end
    }

    fun stillDurationMs(talking: Boolean): Long = if (talking) 3000L else 4000L
}
