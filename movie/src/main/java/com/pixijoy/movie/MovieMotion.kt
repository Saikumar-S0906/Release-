package com.pixijoy.movie

import kotlin.math.cos

object MovieMotion {
    fun scale(t: Float, talking: Boolean): Float {
        val u = t.coerceIn(0f, 1f)
        return if (talking) {
            1f + 0.08f * (0.5f - 0.5f * cos((u * 2f * Math.PI).toFloat()))
        } else {
            1f + 0.18f * u
        }
    }

    fun pan(t: Float, talking: Boolean, width: Float): Float {
        if (talking) return 0f
        return (t.coerceIn(0f, 1f) - 0.5f) * width * 0.12f
    }
}

object MovieSpeed {
    fun normalize(speed: Float): Float = speed.coerceIn(0.5f, 2f)

    fun outputDurationMs(sourceMs: Long, speed: Float): Long =
        (sourceMs / normalize(speed)).toLong().coerceAtLeast(250)
}
