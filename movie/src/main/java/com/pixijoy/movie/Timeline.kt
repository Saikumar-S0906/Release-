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
    val fadeOutMs: Long = 0,
    /** Transition into the following clip (D2). */
    val transition: ClipTransition = ClipTransition.None,
    val transitionMs: Long = 0,
    /**
     * End speed for a linear ramp across the clip (D2). When equal to [speed], constant.
     * Engine expands into stepped segments for Media3.
     */
    val speedEnd: Float = speed,
    /** Keyframed scale/pan/rotate (D3). */
    val motion: ClipMotion = ClipMotion.None
) {
    val sourceDurationMs: Long get() = (outMs - inMs).coerceAtLeast(1)
    val durationMs: Long
        get() = if (!hasSpeedRamp) {
            MovieSpeed.outputDurationMs(sourceDurationMs, speed)
        } else {
            MovieSpeed.expandRamp(this).sumOf { it.durationMs }
        }
    val hasSpeedRamp: Boolean
        get() = kotlin.math.abs(MovieSpeed.normalize(speedEnd) - MovieSpeed.normalize(speed)) > 0.01f
    val hasTransition: Boolean
        get() = transition != ClipTransition.None && transitionMs > 0L
    val hasMotion: Boolean get() = !motion.isIdentity
}

data class AudioBed(
    val fileName: String,
    val volume: Float = 0.8f,
    val fadeInMs: Long = 400,
    val fadeOutMs: Long = 400,
    val muted: Boolean = false,
    /** Music floor while dialogue / VO is active (0 = mute under, 1 = no duck). */
    val duck: Float = 0.25f,
    /** Duck ramp into dialogue (ms). */
    val attackMs: Long = 120,
    /** Duck ramp out of dialogue (ms). D6 polish — was tied to attack. */
    val releaseMs: Long = 250
)

data class AudioStem(
    val id: String,
    val fileName: String,
    val volume: Float = 1f,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val muted: Boolean = false,
    val role: String = "sfx"
)

data class AudioMasterFx(
    val lowGainDb: Float = 0f,
    val midGainDb: Float = 0f,
    val highGainDb: Float = 0f,
    val compThresholdDb: Float = 0f,
    val compRatio: Float = 1f,
    val compAttackMs: Float = 10f,
    val compReleaseMs: Float = 100f,
    val compMakeupDb: Float = 0f,
    val limiterCeilingDb: Float = -1f,
    val limiterEnabled: Boolean = false
) {
    fun isNeutral(): Boolean =
        kotlin.math.abs(lowGainDb) < 0.01f &&
            kotlin.math.abs(midGainDb) < 0.01f &&
            kotlin.math.abs(highGainDb) < 0.01f &&
            kotlin.math.abs(compThresholdDb) < 0.01f &&
            kotlin.math.abs(compRatio - 1f) < 0.01f &&
            kotlin.math.abs(compMakeupDb) < 0.01f &&
            !limiterEnabled

    fun encode(): String =
        "X," + listOf(
            lowGainDb, midGainDb, highGainDb,
            compThresholdDb, compRatio, compAttackMs, compReleaseMs,
            compMakeupDb, limiterCeilingDb
        ).joinToString(",") { fmt(it) } + ",${if (limiterEnabled) 1 else 0}"

    companion object {
        val Neutral = AudioMasterFx()

        fun decode(parts: List<String>): AudioMasterFx? {
            if (parts.size < 11) return null
            return runCatching {
                AudioMasterFx(
                    lowGainDb = parts[1].toFloat(),
                    midGainDb = parts[2].toFloat(),
                    highGainDb = parts[3].toFloat(),
                    compThresholdDb = parts[4].toFloat(),
                    compRatio = parts[5].toFloat().coerceAtLeast(1f),
                    compAttackMs = parts[6].toFloat().coerceAtLeast(0f),
                    compReleaseMs = parts[7].toFloat().coerceAtLeast(0f),
                    compMakeupDb = parts[8].toFloat(),
                    limiterCeilingDb = parts[9].toFloat(),
                    limiterEnabled = parts[10] == "1"
                )
            }.getOrNull()
        }

        private fun fmt(v: Float): String =
            if (v == v.toLong().toFloat()) v.toLong().toString() else v.toString()
    }
}

data class Timeline(
    val clips: List<TimelineClip> = emptyList(),
    val bed: AudioBed? = null,
    val voice: AudioBed? = null,
    val outputHeight: Int = 720,
    val extraStems: List<AudioStem> = emptyList(),
    val masterFx: AudioMasterFx = AudioMasterFx(),
    /** Timed title / caption layers (D4). */
    val titles: List<TimelineTitle> = emptyList(),
    /** Timeline grade + look (D5). */
    val grade: TimelineGrade = TimelineGrade.Neutral
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

    /**
     * D2 roll: move the shared cut between [id] and the next clip by [deltaSourceMs]
     * (positive expands left / trims right). Requires adjacent same-media clips.
     */
    fun rollCut(id: String, deltaSourceMs: Long, minHoldMs: Long = 250): Timeline {
        val i = clips.indexOfFirst { it.id == id }
        if (i < 0 || i >= clips.lastIndex) return this
        val left = clips[i]
        val right = clips[i + 1]
        if (left.mediaIndex != right.mediaIndex || left.outMs != right.inMs) return this
        val cut = (left.outMs + deltaSourceMs).coerceIn(
            left.inMs + minHoldMs,
            right.outMs - minHoldMs
        )
        if (cut == left.outMs) return this
        val copy = clips.toMutableList()
        copy[i] = left.copy(outMs = cut)
        copy[i + 1] = right.copy(inMs = cut)
        return copy(clips = copy)
    }

    /** Apply soft-dissolve pair fades for adjacent clips that declare a transition. */
    fun withTransitionFadesApplied(): Timeline {
        if (clips.size < 2) return this
        val copy = clips.toMutableList()
        for (i in 0 until copy.lastIndex) {
            val cur = copy[i]
            if (!cur.hasTransition || cur.transition != ClipTransition.Soft) continue
            val next = copy[i + 1]
            val ms = cur.transitionMs.coerceIn(40L, minOf(cur.durationMs, next.durationMs) / 2)
            copy[i] = cur.copy(fadeOutMs = maxOf(cur.fadeOutMs, ms))
            copy[i + 1] = next.copy(fadeInMs = maxOf(next.fadeInMs, ms))
        }
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
            "C,${c.mediaIndex},${c.inMs},${c.outMs},${c.speed},${if (c.muted) 1 else 0},${c.id}," +
                "${ClipMix.volume(c.volume)},${c.fadeInMs.coerceAtLeast(0)},${c.fadeOutMs.coerceAtLeast(0)}," +
                "${c.transition.name},${c.transitionMs.coerceAtLeast(0)},${c.speedEnd},${c.motion.encode()}"
        })
        val music = bed
        if (music != null) {
            if (isNotEmpty()) append('\n')
            append("A,${music.fileName},${ClipMix.volume(music.volume)},${music.fadeInMs.coerceAtLeast(0)},${music.fadeOutMs.coerceAtLeast(0)},${if (music.muted) 1 else 0},${ClipMix.volume(music.duck)},${music.attackMs.coerceAtLeast(0)},${music.releaseMs.coerceAtLeast(0)}")
        }
        val vo = voice
        if (vo != null) {
            if (isNotEmpty()) append('\n')
            append("V,${vo.fileName},${ClipMix.volume(vo.volume)},${vo.fadeInMs.coerceAtLeast(0)},${vo.fadeOutMs.coerceAtLeast(0)},${if (vo.muted) 1 else 0},${ClipMix.volume(vo.duck)},${vo.attackMs.coerceAtLeast(0)},${vo.releaseMs.coerceAtLeast(0)}")
        }
        for (stem in extraStems) {
            if (isNotEmpty()) append('\n')
            append(
                "S,${stem.id},${stem.fileName},${ClipMix.volume(stem.volume)}," +
                    "${stem.fadeInMs.coerceAtLeast(0)},${stem.fadeOutMs.coerceAtLeast(0)}," +
                    "${if (stem.muted) 1 else 0},${stem.role.ifBlank { "sfx" }}"
            )
        }
        if (MovieExportSize.normalize(outputHeight) != 720) {
            if (isNotEmpty()) append('\n')
            append("H,${MovieExportSize.normalize(outputHeight)}")
        }
        if (!masterFx.isNeutral()) {
            if (isNotEmpty()) append('\n')
            append(masterFx.encode())
        }
        for (title in titles) {
            if (title.text.isBlank()) continue
            if (isNotEmpty()) append('\n')
            append(title.encodeLine())
        }
        if (!grade.isNeutral()) {
            if (isNotEmpty()) append('\n')
            append(grade.encodeLine())
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
                    val speed = p[4].toFloat()
                    TimelineClip(
                        id = p[6],
                        mediaIndex = p[1].toInt().coerceIn(0, (count - 1).coerceAtLeast(0)),
                        inMs = p[2].toLong(),
                        outMs = p[3].toLong(),
                        speed = speed,
                        muted = p[5] == "1",
                        volume = p.getOrNull(7)?.toFloatOrNull()?.let(ClipMix::volume) ?: 1f,
                        fadeInMs = p.getOrNull(8)?.toLongOrNull()?.coerceAtLeast(0) ?: 0L,
                        fadeOutMs = p.getOrNull(9)?.toLongOrNull()?.coerceAtLeast(0) ?: 0L,
                        transition = ClipTransition.parse(p.getOrNull(10)),
                        transitionMs = p.getOrNull(11)?.toLongOrNull()?.coerceAtLeast(0) ?: 0L,
                        speedEnd = p.getOrNull(12)?.toFloatOrNull() ?: speed,
                        motion = ClipMotion.decode(p.getOrNull(13))
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
                        attackMs = p.getOrNull(7)?.toLongOrNull()?.coerceAtLeast(0) ?: 120L,
                        releaseMs = p.getOrNull(8)?.toLongOrNull()?.coerceAtLeast(0) ?: 250L
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
                        duck = p.getOrNull(6)?.toFloatOrNull()?.let(ClipMix::volume) ?: 1f,
                        attackMs = p.getOrNull(7)?.toLongOrNull()?.coerceAtLeast(0) ?: 120L,
                        releaseMs = p.getOrNull(8)?.toLongOrNull()?.coerceAtLeast(0) ?: 250L
                    )
                }.getOrNull()
            }.firstOrNull()
            val height = raw.lineSequence().mapNotNull { line ->
                val p = line.trim().split(',')
                if (p.getOrNull(0) != "H" || p.size < 2) return@mapNotNull null
                p[1].toIntOrNull()?.let(MovieExportSize::normalize)
            }.firstOrNull() ?: 720
            val extraStems = raw.lineSequence().mapNotNull { line ->
                val p = line.trim().split(',')
                if (p.getOrNull(0) != "S" || p.size < 7) return@mapNotNull null
                runCatching {
                    AudioStem(
                        id = p[1],
                        fileName = p[2],
                        volume = p[3].toFloat().let(ClipMix::volume),
                        fadeInMs = p[4].toLong().coerceAtLeast(0),
                        fadeOutMs = p[5].toLong().coerceAtLeast(0),
                        muted = p[6] == "1",
                        role = p.getOrNull(7)?.ifBlank { "sfx" } ?: "sfx"
                    )
                }.getOrNull()
            }.toList()
            val masterFx = raw.lineSequence().mapNotNull { line ->
                val p = line.trim().split(',')
                if (p.getOrNull(0) != "X") return@mapNotNull null
                AudioMasterFx.decode(p)
            }.firstOrNull() ?: AudioMasterFx()
            val titles = raw.lineSequence().mapNotNull { line ->
                TimelineTitle.decodeLine(line.trim().split(','))
            }.toList()
            val grade = raw.lineSequence().mapNotNull { line ->
                TimelineGrade.decodeLine(line.trim().split(','))
            }.firstOrNull() ?: TimelineGrade.Neutral
            val clips = if (parsed.isEmpty()) fromMedia(count, durationMs).clips else parsed
            return Timeline(clips, bed, voice, height, extraStems, masterFx, titles, grade)
        }
    }
}
