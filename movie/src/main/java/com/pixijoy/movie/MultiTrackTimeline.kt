package com.pixijoy.movie

/**
 * R4 D1 foundation: multi-track timeline model (V + A lanes).
 * Existing single-list [Timeline] remains the R2/R3 encode path; this type is additive
 * until preview/export consume tracks.
 */
enum class TrackKind { Video, Audio }

data class TimelineTrack(
    val id: String,
    val kind: TrackKind,
    val name: String,
    val muted: Boolean = false,
    val clips: List<TimelineClip> = emptyList()
)

data class MultiTrackTimeline(
    val version: Int = 1,
    val tracks: List<TimelineTrack> = emptyList(),
    val bed: AudioBed? = null,
    val voice: AudioBed? = null,
    val extraStems: List<AudioStem> = emptyList(),
    val masterFx: AudioMasterFx = AudioMasterFx.Neutral,
    val outputHeight: Int = 1080,
    /** Timed title / caption layers (D4). */
    val titles: List<TimelineTitle> = emptyList(),
    /** Timeline grade + look (D5). */
    val grade: TimelineGrade = TimelineGrade.Neutral,
    /** Multi-video frame layout (D7). */
    val layout: VideoLayout = VideoLayout.Single
) {
    fun videoTracks(): List<TimelineTrack> = tracks.filter { it.kind == TrackKind.Video }
    fun audioTracks(): List<TimelineTrack> = tracks.filter { it.kind == TrackKind.Audio }

    /** Clips from unmuted video tracks, track order then clip order — composition source of truth. */
    fun composedVideoClips(): List<TimelineClip> =
        videoTracks().filter { !it.muted }.flatMap { it.clips }

    fun unmutedAudioTracksWithClips(): List<TimelineTrack> =
        audioTracks().filter { !it.muted && it.clips.isNotEmpty() }

    fun totalMs(): Long =
        composedVideoClips()
            .sumOf { it.durationMs }
            .coerceAtLeast(1)

    /** Flatten primary video lane (+ beds/fx) for legacy single-list UI helpers. */
    fun toFlatTimeline(): Timeline {
        val videoClips = composedVideoClips().ifEmpty {
            videoTracks().firstOrNull()?.clips.orEmpty()
        }
        return Timeline(
            clips = videoClips,
            bed = bed,
            voice = voice,
            outputHeight = outputHeight,
            extraStems = extraStems,
            masterFx = masterFx,
            titles = titles,
            grade = grade,
            layout = layout
        )
    }

    /** Replace primary video clips + global audio beds from a flat [Timeline]; keep extra tracks. */
    fun withPrimaryFlat(flat: Timeline): MultiTrackTimeline {
        val ensured = ensureDefaultTracks()
        val primaryId = ensured.videoTracks().first().id
        val nextTracks = ensured.tracks.map { track ->
            if (track.id == primaryId) track.copy(clips = flat.clips) else track
        }
        return ensured.copy(
            tracks = nextTracks,
            bed = flat.bed,
            voice = flat.voice,
            extraStems = flat.extraStems,
            masterFx = flat.masterFx,
            outputHeight = flat.outputHeight,
            titles = flat.titles,
            grade = flat.grade,
            layout = flat.layout
        )
    }

    fun ensureDefaultTracks(): MultiTrackTimeline {
        val hasVideo = tracks.any { it.kind == TrackKind.Video }
        val hasAudio = tracks.any { it.kind == TrackKind.Audio }
        if (hasVideo && hasAudio) return this
        val next = tracks.toMutableList()
        if (!hasVideo) {
            next.add(0, TimelineTrack(id = "V1", kind = TrackKind.Video, name = "Video 1"))
        }
        if (!hasAudio) {
            next.add(TimelineTrack(id = "A1", kind = TrackKind.Audio, name = "Audio 1"))
        }
        return copy(tracks = next)
    }

    fun withTrackMuted(trackId: String, muted: Boolean): MultiTrackTimeline =
        copy(tracks = tracks.map { if (it.id == trackId) it.copy(muted = muted) else it })

    /**
     * Plan for Media3 Composition sequences (testable without Transformer).
     * Video = [layout.videoInputs] parallel sequences when multi; else 1.
     * Each unmuted audio track with clips = 1 sequence; beds/stems separate.
     */
    fun compositionSequencePlan(hasBedFile: Boolean, hasVoiceFile: Boolean, stemCount: Int): Int {
        var n = layout.videoInputs.coerceAtLeast(1)
        n += unmutedAudioTracksWithClips().size
        if (hasBedFile) n++
        if (hasVoiceFile) n++
        n += stemCount.coerceAtLeast(0)
        return n
    }

    fun encode(): String = buildString {
        appendLine("MT,$version,$outputHeight")
        tracks.forEach { track ->
            appendLine(
                "TK,${track.id},${track.kind.name},${track.name.encodeTrack()},${if (track.muted) 1 else 0}"
            )
            track.clips.forEach { clip ->
                appendLine(
                    "TC,${track.id},${clip.id},${clip.mediaIndex},${clip.inMs},${clip.outMs}," +
                        "${clip.speed},${if (clip.muted) 1 else 0},${clip.volume}," +
                        "${clip.fadeInMs},${clip.fadeOutMs},${clip.transition.name}," +
                        "${clip.transitionMs},${clip.speedEnd},${clip.motion.encode()}"
                )
            }
        }
        bed?.let { appendLine("B,${it.fileName},${it.volume},${it.fadeInMs},${it.fadeOutMs},${if (it.muted) 1 else 0},${it.duck},${it.attackMs},${it.releaseMs}") }
        voice?.let { appendLine("V,${it.fileName},${it.volume},${it.fadeInMs},${it.fadeOutMs},${if (it.muted) 1 else 0},${it.duck},${it.attackMs},${it.releaseMs}") }
        extraStems.forEach { stem ->
            appendLine(
                "S,${stem.id},${stem.fileName},${stem.volume},${stem.fadeInMs},${stem.fadeOutMs}," +
                    "${if (stem.muted) 1 else 0},${stem.role.encodeTrack()}"
            )
        }
        if (!masterFx.isNeutral()) appendLine(masterFx.encode())
        titles.filter { it.text.isNotBlank() }.forEach { appendLine(it.encodeLine()) }
        if (!grade.isNeutral()) appendLine(grade.encodeLine())
        if (layout != VideoLayout.Single) appendLine("L,${layout.encode()}")
    }

    companion object {
        fun fromFlatTimeline(timeline: Timeline): MultiTrackTimeline {
            val tracks = buildList {
                add(
                    TimelineTrack(
                        id = "V1",
                        kind = TrackKind.Video,
                        name = "Video 1",
                        clips = timeline.clips
                    )
                )
                // Reserved audio lane for future clip-based audio; beds stay on B/V/S lines.
                add(
                    TimelineTrack(
                        id = "A1",
                        kind = TrackKind.Audio,
                        name = "Audio 1",
                        clips = emptyList()
                    )
                )
            }
            return MultiTrackTimeline(
                tracks = tracks,
                bed = timeline.bed,
                voice = timeline.voice,
                extraStems = timeline.extraStems,
                masterFx = timeline.masterFx,
                outputHeight = timeline.outputHeight,
                titles = timeline.titles,
                grade = timeline.grade,
                layout = timeline.layout
            )
        }

        fun fromSingleVideo(clips: List<TimelineClip>): MultiTrackTimeline =
            fromFlatTimeline(Timeline(clips = clips))

        fun decode(raw: String): MultiTrackTimeline? {
            if (!raw.lineSequence().any { it.startsWith("MT,") }) return null
            var version = 1
            var height = 1080
            val trackMap = linkedMapOf<String, TimelineTrack>()
            var bed: AudioBed? = null
            var voice: AudioBed? = null
            val stems = ArrayList<AudioStem>()
            var fx = AudioMasterFx.Neutral
            val titles = ArrayList<TimelineTitle>()
            var grade = TimelineGrade.Neutral
            var layout = VideoLayout.Single
            raw.lineSequence().forEach { line ->
                val parts = line.split(',')
                when (parts.firstOrNull()) {
                    "MT" -> {
                        version = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        height = parts.getOrNull(2)?.toIntOrNull() ?: 1080
                    }
                    "TK" -> if (parts.size >= 5) {
                        val id = parts[1]
                        val kind = runCatching { TrackKind.valueOf(parts[2]) }.getOrDefault(TrackKind.Video)
                        trackMap[id] = TimelineTrack(
                            id = id,
                            kind = kind,
                            name = parts[3].decodeTrack(),
                            muted = parts[4] == "1"
                        )
                    }
                    "TC" -> if (parts.size >= 11) {
                        val trackId = parts[1]
                        val speed = parts[6].toFloat()
                        val clip = TimelineClip(
                            id = parts[2],
                            mediaIndex = parts[3].toInt(),
                            inMs = parts[4].toLong(),
                            outMs = parts[5].toLong(),
                            speed = speed,
                            muted = parts[7] == "1",
                            volume = parts[8].toFloat(),
                            fadeInMs = parts[9].toLong(),
                            fadeOutMs = parts[10].toLong(),
                            transition = ClipTransition.parse(parts.getOrNull(11)),
                            transitionMs = parts.getOrNull(12)?.toLongOrNull() ?: 0L,
                            speedEnd = parts.getOrNull(13)?.toFloatOrNull() ?: speed,
                            motion = ClipMotion.decode(parts.getOrNull(14))
                        )
                        val track = trackMap[trackId] ?: return@forEach
                        trackMap[trackId] = track.copy(clips = track.clips + clip)
                    }
                    "B" -> if (parts.size >= 8) {
                        bed = AudioBed(
                            fileName = parts[1],
                            volume = parts[2].toFloat(),
                            fadeInMs = parts[3].toLong(),
                            fadeOutMs = parts[4].toLong(),
                            muted = parts[5] == "1",
                            duck = parts[6].toFloat(),
                            attackMs = parts[7].toLong(),
                            releaseMs = parts.getOrNull(8)?.toLongOrNull() ?: 250L
                        )
                    }
                    "V" -> if (parts.size >= 8) {
                        voice = AudioBed(
                            fileName = parts[1],
                            volume = parts[2].toFloat(),
                            fadeInMs = parts[3].toLong(),
                            fadeOutMs = parts[4].toLong(),
                            muted = parts[5] == "1",
                            duck = parts[6].toFloat(),
                            attackMs = parts[7].toLong(),
                            releaseMs = parts.getOrNull(8)?.toLongOrNull() ?: 250L
                        )
                    }
                    "S" -> if (parts.size >= 8) {
                        stems += AudioStem(
                            id = parts[1],
                            fileName = parts[2],
                            volume = parts[3].toFloat(),
                            fadeInMs = parts[4].toLong(),
                            fadeOutMs = parts[5].toLong(),
                            muted = parts[6] == "1",
                            role = parts[7].decodeTrack()
                        )
                    }
                    "X" -> AudioMasterFx.decode(parts)?.let { fx = it }
                    "TT" -> TimelineTitle.decodeLine(parts)?.let { titles += it }
                    "G" -> TimelineGrade.decodeLine(parts)?.let { grade = it }
                    "L" -> if (parts.size >= 2) layout = VideoLayout.parse(parts[1])
                }
            }
            return MultiTrackTimeline(
                version = version,
                tracks = trackMap.values.toList(),
                bed = bed,
                voice = voice,
                extraStems = stems,
                masterFx = fx,
                outputHeight = height,
                titles = titles,
                grade = grade,
                layout = layout
            )
        }
    }
}

private fun String.encodeTrack(): String = replace(",", "%2C")
private fun String.decodeTrack(): String = replace("%2C", ",")
