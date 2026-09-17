package com.pixijoy.movie

data class TimelineClip(
    val id: String,
    val mediaIndex: Int,
    val inMs: Long,
    val outMs: Long,
    val speed: Float = 1f,
    val muted: Boolean = false,
    val volume: Float = 1f,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0
) {
    val sourceDurationMs: Long get() = (outMs - inMs).coerceAtLeast(1)
    val durationMs: Long get() = MovieSpeed.outputDurationMs(sourceDurationMs, speed)
}

data class AudioBed(
    val fileName: String,
    val volume: Float = 0.8f,
    val fadeInMs: Long = 400,
    val fadeOutMs: Long = 400,
    val muted: Boolean = false,
    val duck: Float = 0.25f,
    val attackMs: Long = 120
)

data class Timeline(
    val clips: List<TimelineClip> = emptyList(),
    val bed: AudioBed? = null,
    val voice: AudioBed? = null
) {
    fun totalMs(): Long = clips.sumOf { it.durationMs }

    fun dialogueWindows(): List<ClipMix.Window> {
        var t = 0L
        val out = ArrayList<ClipMix.Window>()
        for (clip in clips) {
            if (!clip.muted && clip.volume > 0.001f) {
                out += ClipMix.Window(t, t + clip.durationMs)
            }
            t += clip.durationMs
        }
        return out
    }

    fun duckWindows(voiceEndMs: Long = 0): List<ClipMix.Window> {
        val out = dialogueWindows().toMutableList()
        val v = voice
        if (v != null && !v.muted && voiceEndMs > 0) {
            out += ClipMix.Window(0, voiceEndMs)
        }
        return out
    }

    fun clipAt(playheadMs: Long): Pair<TimelineClip, Long>? {
        var t = 0L
        for (clip in clips) {
            val end = t + clip.durationMs
            if (playheadMs in t until end) return clip to t
            t = end
        }
        return clips.lastOrNull()?.let { it to (totalMs() - it.durationMs).coerceAtLeast(0) }
    }

    fun startOf(id: String): Long {
        var t = 0L
        for (clip in clips) {
            if (clip.id == id) return t
            t += clip.durationMs
        }
        return 0L
    }

    fun windowSeek(playheadMs: Long): Pair<Int, Long>? {
        val (clip, start) = clipAt(playheadMs) ?: return null
        val i = clips.indexOfFirst { it.id == clip.id }
        if (i < 0) return null
        return i to (playheadMs - start).coerceAtLeast(0)
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
                out += clip.copy(id = clip.id + "a", outMs = mid, fadeOutMs = 0)
                out += clip.copy(id = clip.id + "b", inMs = mid, fadeInMs = 0)
                split = true
            } else {
                out += clip
            }
            t = end
        }
        return if (split) copy(clips = out) else this
    }

    fun update(id: String, transform: (TimelineClip) -> TimelineClip): Timeline =
        copy(clips = clips.map { if (it.id == id) transform(it) else it })

    fun remove(id: String): Timeline = copy(clips = clips.filterNot { it.id == id })

    fun trimIn(id: String, newInMs: Long, minHoldMs: Long = 250): Timeline = update(id) { clip ->
        val inn = newInMs.coerceIn(0, (clip.outMs - minHoldMs).coerceAtLeast(0))
        clip.copy(inMs = inn)
    }

    fun trimOut(id: String, newOutMs: Long, sourceEndMs: Long, minHoldMs: Long = 250): Timeline = update(id) { clip ->
        val out = newOutMs.coerceIn(clip.inMs + minHoldMs, sourceEndMs.coerceAtLeast(clip.inMs + minHoldMs))
        clip.copy(outMs = out)
    }

    fun rippleTrimIn(id: String, newInMs: Long, minHoldMs: Long = 250): Timeline {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return this
        val clip = clips[i]
        val inn = newInMs.coerceIn(0, (clip.outMs - minHoldMs).coerceAtLeast(0))
        val copy = clips.toMutableList()
        if (i > 0) {
            val prev = copy[i - 1]
            if (prev.mediaIndex == clip.mediaIndex && prev.outMs == clip.inMs) {
                val cut = inn.coerceIn(prev.inMs + minHoldMs, (clip.outMs - minHoldMs).coerceAtLeast(prev.inMs + minHoldMs))
                copy[i - 1] = prev.copy(outMs = cut)
                copy[i] = clip.copy(inMs = cut)
                return copy(clips = copy)
            }
        }
        copy[i] = clip.copy(inMs = inn)
        return copy(clips = copy)
    }

    fun rippleTrimOut(id: String, newOutMs: Long, sourceEndMs: Long, minHoldMs: Long = 250): Timeline {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return this
        val clip = clips[i]
        val out = newOutMs.coerceIn(clip.inMs + minHoldMs, sourceEndMs.coerceAtLeast(clip.inMs + minHoldMs))
        val copy = clips.toMutableList()
        if (i < copy.lastIndex) {
            val next = copy[i + 1]
            if (next.mediaIndex == clip.mediaIndex && next.inMs == clip.outMs) {
                val cut = out.coerceIn(
                    clip.inMs + minHoldMs,
                    (next.outMs - minHoldMs).coerceAtLeast(clip.inMs + minHoldMs)
                ).coerceAtMost(sourceEndMs.coerceAtLeast(clip.inMs + minHoldMs))
                copy[i] = clip.copy(outMs = cut)
                copy[i + 1] = next.copy(inMs = cut)
                return copy(clips = copy)
            }
        }
        copy[i] = clip.copy(outMs = out)
        return copy(clips = copy)
    }

    fun move(id: String, delta: Int): Timeline {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0) return this
        val j = (i + delta).coerceIn(0, clips.lastIndex)
        if (i == j) return this
        val copy = clips.toMutableList()
        val clip = copy.removeAt(i)
        copy.add(j, clip)
        return copy(clips = copy)
    }

    fun encode(): String = buildString {
        append(clips.joinToString("\n") { c ->
            "C,${c.mediaIndex},${c.inMs},${c.outMs},${c.speed},${if (c.muted) 1 else 0},${c.id},${ClipMix.volume(c.volume)},${c.fadeInMs.coerceAtLeast(0)},${c.fadeOutMs.coerceAtLeast(0)}"
        })
        val music = bed
        if (music != null) {
            if (isNotEmpty()) append('\n')
            append("A,${music.fileName},${ClipMix.volume(music.volume)},${music.fadeInMs.coerceAtLeast(0)},${music.fadeOutMs.coerceAtLeast(0)},${if (music.muted) 1 else 0},${ClipMix.volume(music.duck)},${music.attackMs.coerceAtLeast(0)}")
        }
        val vo = voice
        if (vo != null) {
            if (isNotEmpty()) append('\n')
            append("V,${vo.fileName},${ClipMix.volume(vo.volume)},${vo.fadeInMs.coerceAtLeast(0)},${vo.fadeOutMs.coerceAtLeast(0)},${if (vo.muted) 1 else 0}")
        }
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
                        muted = p[5] == "1",
                        volume = p.getOrNull(7)?.toFloatOrNull()?.let(ClipMix::volume) ?: 1f,
                        fadeInMs = p.getOrNull(8)?.toLongOrNull()?.coerceAtLeast(0) ?: 0L,
                        fadeOutMs = p.getOrNull(9)?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
                    )
                }.getOrNull()
            }.toList()
            val bed = raw.lineSequence().mapNotNull { line ->
                val p = line.trim().split(',')
                if (p.getOrNull(0) != "A" || p.size < 6) return@mapNotNull null
                runCatching {
                    AudioBed(
                        fileName = p[1],
                        volume = p[2].toFloat().let(ClipMix::volume),
                        fadeInMs = p[3].toLong().coerceAtLeast(0),
                        fadeOutMs = p[4].toLong().coerceAtLeast(0),
                        muted = p[5] == "1",
                        duck = p.getOrNull(6)?.toFloatOrNull()?.let(ClipMix::volume) ?: 0.25f,
                        attackMs = p.getOrNull(7)?.toLongOrNull()?.coerceAtLeast(0) ?: 120L
                    )
                }.getOrNull()
            }.firstOrNull()
            val voice = raw.lineSequence().mapNotNull { line ->
                val p = line.trim().split(',')
                if (p.getOrNull(0) != "V" || p.size < 6) return@mapNotNull null
                runCatching {
                    AudioBed(
                        fileName = p[1],
                        volume = p[2].toFloat().let(ClipMix::volume),
                        fadeInMs = p[3].toLong().coerceAtLeast(0),
                        fadeOutMs = p[4].toLong().coerceAtLeast(0),
                        muted = p[5] == "1",
                        duck = 1f
                    )
                }.getOrNull()
            }.firstOrNull()
            val clips = if (parsed.isEmpty()) fromMedia(count, durationMs).clips else parsed
            return Timeline(clips, bed, voice)
        }
    }
}
