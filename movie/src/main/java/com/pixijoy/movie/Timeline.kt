package com.pixijoy.movie

data class TimelineClip(
    val id: String,
    val mediaIndex: Int,
    val inMs: Long,
    val outMs: Long,
    val speed: Float = 1f,
    val muted: Boolean = false
) {
    val sourceDurationMs: Long get() = (outMs - inMs).coerceAtLeast(1)
    val durationMs: Long get() = MovieSpeed.outputDurationMs(sourceDurationMs, speed)
}

data class Timeline(val clips: List<TimelineClip> = emptyList()) {
    fun totalMs(): Long = clips.sumOf { it.durationMs }

    fun clipAt(playheadMs: Long): Pair<TimelineClip, Long>? {
        var t = 0L
        for (clip in clips) {
            val end = t + clip.durationMs
            if (playheadMs in t until end) return clip to t
            t = end
        }
        return clips.lastOrNull()?.let { it to (totalMs() - it.durationMs).coerceAtLeast(0) }
    }

    fun split(playheadMs: Long): Timeline {
        var t = 0L
        val out = ArrayList<TimelineClip>(clips.size + 1)
        var split = false
        for (clip in clips) {
            val end = t + clip.durationMs
            if (!split && playheadMs > t + 120 && playheadMs < end - 120) {
                val local = ((playheadMs - t) * clip.speed).toLong()
                val mid = (clip.inMs + local).coerceIn(clip.inMs + 1, clip.outMs - 1)
                out += clip.copy(id = clip.id + "a", outMs = mid)
                out += clip.copy(id = clip.id + "b", inMs = mid)
                split = true
            } else {
                out += clip
            }
            t = end
        }
        return if (split) Timeline(out) else this
    }

    fun update(id: String, transform: (TimelineClip) -> TimelineClip): Timeline =
        Timeline(clips.map { if (it.id == id) transform(it) else it })

    fun encode(): String = clips.joinToString("\n") { c ->
        "C,${c.mediaIndex},${c.inMs},${c.outMs},${c.speed},${if (c.muted) 1 else 0},${c.id}"
    }

    companion object {
        fun fromMedia(count: Int, durationMs: (Int) -> Long): Timeline {
            if (count <= 0) return Timeline()
            return Timeline(
                (0 until count).map { i ->
                    val d = durationMs(i).coerceAtLeast(250)
                    TimelineClip(id = "c$i", mediaIndex = i, inMs = 0, outMs = d)
                }
            )
        }

        fun decode(raw: String, count: Int, durationMs: (Int) -> Long): Timeline {
            val parsed = raw.lineSequence().mapNotNull { line ->
                val p = line.trim().split(',')
                if (p.getOrNull(0) != "C" || p.size < 7) return@mapNotNull null
                runCatching {
                    TimelineClip(
                        id = p[6],
                        mediaIndex = p[1].toInt().coerceIn(0, (count - 1).coerceAtLeast(0)),
                        inMs = p[2].toLong(),
                        outMs = p[3].toLong(),
                        speed = p[4].toFloat(),
                        muted = p[5] == "1"
                    )
                }.getOrNull()
            }.toList()
            return if (parsed.isEmpty()) fromMedia(count, durationMs) else Timeline(parsed)
        }
    }
}
