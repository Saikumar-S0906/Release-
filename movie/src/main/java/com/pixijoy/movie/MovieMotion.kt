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

    /**
     * Same pan as [pan], in Media3 clip space (frame width is 2).
     * Pixel shift / width * 2 = (t-0.5)*0.24.
     */
    fun panNdc(t: Float, talking: Boolean): Float {
        if (talking) return 0f
        return (t.coerceIn(0f, 1f) - 0.5f) * 0.24f
    }

    /**
     * Default still move. Preview samples [scale] and [pan]; export samples this
     * curve. Non-talking zoom and pan are linear (two keys). Talking is a cosine
     * pulse, so it is stored on eight steps.
     */
    fun kenBurns(talking: Boolean): ClipMotion {
        val steps = if (talking) 8 else 1
        val keys = ArrayList<TransformKeyframe>(steps + 1)
        for (i in 0..steps) {
            val u = i / steps.toFloat()
            keys += TransformKeyframe(
                t = u,
                scale = scale(u, talking),
                tx = panNdc(u, talking)
            )
        }
        return ClipMotion(keys)
    }
}

/**
 * A still with a grade, a title, a layout, or a user move must export through the
 * composition (so the file contains that picture). Identity motion on a still is
 * filled with [MovieMotion.kenBurns] there. A plain still stays on [KenBurnsEncoder].
 */
object StillPicture {
    fun needsComposition(timeline: MultiTrackTimeline): Boolean =
        timeline.titles.any { it.text.isNotBlank() } ||
            !timeline.grade.isNeutral() ||
            timeline.layout.isMulti() ||
            timeline.composedVideoClips().any { it.hasMotion }

    /**
     * Timeline to hand to the composition exporter, or null when the still encoder
     * already matches the preview (no grade, no title, no user move, single layout).
     */
    fun forExport(
        timeline: MultiTrackTimeline,
        talking: Boolean,
        stillAt: (mediaIndex: Int) -> Boolean
    ): MultiTrackTimeline? {
        if (!needsComposition(timeline)) return null
        if (timeline.layout.isMulti()) return timeline
        return timeline.withDefaultStillMotion(talking, stillAt)
    }
}

fun MultiTrackTimeline.withDefaultStillMotion(
    talking: Boolean,
    stillAt: (mediaIndex: Int) -> Boolean
): MultiTrackTimeline {
    val move = MovieMotion.kenBurns(talking)
    return copy(
        tracks = tracks.map { track ->
            if (track.kind != TrackKind.Video) {
                track
            } else {
                track.copy(
                    clips = track.clips.map { clip ->
                        if (clip.hasMotion || !stillAt(clip.mediaIndex)) clip
                        else clip.copy(motion = move)
                    }
                )
            }
        }
    )
}

object MovieSpeed {
    const val MIN = 0.5f
    const val MAX = 2f
    const val MIN_SLOW_MO = 0.125f

    fun normalize(speed: Float): Float = speed.coerceIn(MIN, MAX)

    /** D10 slow-mo rates (¼ / ⅛) — wider floor than edit-speed. */
    fun normalizeSlowMo(rate: Float): Float = rate.coerceIn(MIN_SLOW_MO, 1f)

    /**
     * Effective Media3 playback rate for a clip. Slow-mo contract wins over D2 speed
     * when active (constant ultra-slow; ramps deferred while slow-mo is on).
     */
    fun playbackRate(clip: TimelineClip): Float =
        if (clip.hasSlowMo) normalizeSlowMo(clip.slowMo.rate) else normalize(clip.speed)

    fun outputDurationMs(sourceMs: Long, speed: Float): Long {
        val rate = speed.coerceIn(MIN_SLOW_MO, MAX).coerceAtLeast(0.05f)
        return (sourceMs / rate).toLong().coerceAtLeast(250)
    }

    /**
     * D2 speed ramp: expand one clip into constant-speed steps Media3 can encode.
     * Returns the original clip alone when there is no ramp or D10 slow-mo is active.
     */
    fun expandRamp(clip: TimelineClip, steps: Int = 6): List<TimelineClip> {
        if (clip.hasSlowMo) {
            val rate = normalizeSlowMo(clip.slowMo.rate)
            return listOf(clip.copy(speed = rate, speedEnd = rate))
        }
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
                transitionMs = if (i == n - 1) clip.transitionMs else 0L,
                motion = clip.motion.withWindow(t0, t1)
            )
        }
        return out.ifEmpty { listOf(clip.copy(speedEnd = clip.speed)) }
    }

    fun expandTimeline(clips: List<TimelineClip>): List<TimelineClip> =
        clips.flatMap { expandRamp(it) }
}
