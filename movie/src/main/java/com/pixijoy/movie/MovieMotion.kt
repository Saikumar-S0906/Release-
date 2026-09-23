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

    /**
     * D2 speed ramp: expand one clip into constant-speed steps Media3 can encode.
     * Returns the original clip alone when there is no ramp.
     */
    fun expandRamp(clip: TimelineClip, steps: Int = 6): List<TimelineClip> {
        if (!clip.hasSpeedRamp) return listOf(clip)
        val n = steps.coerceIn(2, 12)
        val span = clip.sourceDurationMs
        if (span < n * 40L) return listOf(clip.copy(speedEnd = clip.speed))
        val start = normalize(clip.speed)
        val end = normalize(clip.speedEnd)
        val out = ArrayList<TimelineClip>(n)
        for (i in 0 until n) {
            val t0 = i / n.toFloat()
            val t1 = (i + 1) / n.toFloat()
            val src0 = clip.inMs + (span * t0).toLong()
            val src1 = if (i == n - 1) clip.outMs else clip.inMs + (span * t1).toLong()
            if (src1 - src0 < 20L) continue
            val mid = (t0 + t1) * 0.5f
            val rate = start + (end - start) * mid
            out += clip.copy(
                id = "${clip.id}_r$i",
                inMs = src0,
                outMs = src1,
                speed = rate,
                speedEnd = rate,
                fadeInMs = if (i == 0) clip.fadeInMs else 0L,
                fadeOutMs = if (i == n - 1) clip.fadeOutMs else 0L,
                transition = if (i == n - 1) clip.transition else ClipTransition.None,
                transitionMs = if (i == n - 1) clip.transitionMs else 0L
            )
        }
        return out.ifEmpty { listOf(clip.copy(speedEnd = clip.speed)) }
    }

    fun expandTimeline(clips: List<TimelineClip>): List<TimelineClip> =
        clips.flatMap { expandRamp(it) }
}
