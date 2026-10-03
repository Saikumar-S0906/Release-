package com.pixijoy.movie

import com.pixijoy.core.common.StudioDiagnostics

/**
 * Multi-track timeline (V + A lanes plus assembly, grade, sound, and delivery lines).
 * Preview and export read this model. A flat [Timeline] is projected where one
 * composition is required. Encode is versioned (MT,<version>,<height>).
 * A bad number on a TC, B, V, or S line is skipped so the rest of the project opens.
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
    val layout: VideoLayout = VideoLayout.Single,
    /** HDR export intent (D11). */
    val hdrMode: MovieHdrMode = MovieHdrMode.Sdr,
    /** Classical particle VFX layers (F2). */
    val vfx: List<TimelineVfx> = emptyList(),
    /** Extruded / Filament 3D titles (F3). */
    val titles3d: List<TimelineTitle3d> = emptyList(),
    /** CG compositor layers (R7 / coding P1). */
    val cg: List<TimelineCgLayer> = emptyList(),
    /** Director markers (R8 / P2). */
    val markers: List<TimelineMarker> = emptyList(),
    /** Media bins / selects (R8 / P2). */
    val bins: List<ProjectBin> = emptyList(),
    /** Active named cut id (empty = working cut only). */
    val activeAssemblyId: String = "",
    /** Saved alternate cuts (R8 / P2). */
    val assemblies: List<AssemblyVersion> = emptyList(),
    /** Stem bus mute/solo (P4 / K1). */
    val stemBuses: StemBuses = StemBuses.Default,
    /** Loudness target gate (P4 / K4). */
    val loudness: LoudnessTarget = LoudnessTarget.Off,
    /** Delivery / review options (P6 / R9). */
    val delivery: DeliveryOptions = DeliveryOptions.Default
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
            layout = layout,
            hdrMode = hdrMode,
            vfx = vfx,
            titles3d = titles3d,
            cg = cg,
            markers = markers,
            bins = bins,
            stemBuses = stemBuses,
            loudness = loudness
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
            layout = flat.layout,
            hdrMode = flat.hdrMode,
            vfx = flat.vfx,
            titles3d = flat.titles3d,
            cg = flat.cg,
            markers = flat.markers,
            bins = if (flat.bins.isNotEmpty()) flat.bins else bins,
            stemBuses = flat.stemBuses,
            loudness = flat.loudness
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

    /** Snapshot of the working cut for [AssemblyVersion] (no nested assemblies). */
    fun encodeWorkingCut(): String = copy(assemblies = emptyList(), activeAssemblyId = "").encode()

    fun saveAssembly(name: String, id: String = "av${assemblies.size}"): MultiTrackTimeline {
        val slot = AssemblyVersion(id = id, name = name.ifBlank { "Cut ${assemblies.size + 1}" }, cutEncode = encodeWorkingCut())
        val next = assemblies.filterNot { it.id == id } + slot
        return copy(assemblies = next, activeAssemblyId = id)
    }

    fun switchAssembly(id: String): MultiTrackTimeline {
        val slot = assemblies.firstOrNull { it.id == id } ?: return this
        val restored = decode(slot.cutEncode) ?: return this
        return restored.copy(
            assemblies = assemblies,
            activeAssemblyId = id,
            bins = if (restored.bins.isEmpty()) bins else restored.bins,
            markers = restored.markers.ifEmpty { markers }
        )
    }

    fun ensureDefaultBins(): MultiTrackTimeline =
        if (bins.isNotEmpty()) this else copy(bins = ProjectBin.defaults())

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
                        "${clip.transitionMs},${clip.speedEnd},${clip.motion.encode()},${clip.frame.encode()},${clip.chroma.encode()},${clip.slowMo.encode()},${clip.track.encode()}"
                )
            }
        }
        bed?.let {
            appendLine(
                "B,${it.fileName},${it.volume},${it.fadeInMs},${it.fadeOutMs},${if (it.muted) 1 else 0}," +
                    "${it.duck},${it.attackMs},${it.releaseMs},${AudioSync.clampSlip(it.slipMs)}"
            )
        }
        voice?.let {
            appendLine(
                "V,${it.fileName},${it.volume},${it.fadeInMs},${it.fadeOutMs},${if (it.muted) 1 else 0}," +
                    "${it.duck},${it.attackMs},${it.releaseMs},${AudioSync.clampSlip(it.slipMs)}"
            )
        }
        extraStems.forEach { stem ->
            appendLine(
                "S,${stem.id},${stem.fileName},${stem.volume},${stem.fadeInMs},${stem.fadeOutMs}," +
                    "${if (stem.muted) 1 else 0},${stem.role.encodeTrack()},${AudioSync.clampSlip(stem.slipMs)}"
            )
        }
        if (!stemBuses.isDefault()) appendLine(stemBuses.encodeLine())
        if (!loudness.isOff()) appendLine(loudness.encodeLine())
        if (!masterFx.isNeutral()) appendLine(masterFx.encode())
        delivery.encodeLines().forEach { appendLine(it) }
        titles.filter { it.text.isNotBlank() }.forEach { appendLine(it.encodeLine()) }
        vfx.filter { it.isActive }.forEach { appendLine(it.encodeLine()) }
        titles3d.filter { it.text.isNotBlank() }.forEach { appendLine(it.encodeLine()) }
        cg.filter { it.isActive }.forEach { appendLine(it.encodeLine()) }
        markers.forEach { appendLine(it.normalized().encodeLine()) }
        bins.forEach { appendLine(it.encodeLine()) }
        if (activeAssemblyId.isNotBlank()) appendLine("AA,$activeAssemblyId")
        assemblies.forEach { appendLine(it.encodeLine()) }
        val g = grade.normalized()
        if (!g.isNeutral() || g.matchRef.isSet() || g.secondary.enabled) {
            appendLine(g.encodeLine())
            g.encodeExtraLines().forEach { appendLine(it) }
        }
        if (layout != VideoLayout.Single) appendLine("L,${layout.encode()}")
        if (!hdrMode.isDefault()) appendLine("HDR,${hdrMode.encode()}")
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
                layout = timeline.layout,
                hdrMode = timeline.hdrMode,
                vfx = timeline.vfx,
                titles3d = timeline.titles3d,
                cg = timeline.cg,
                markers = timeline.markers,
                bins = timeline.bins,
                stemBuses = timeline.stemBuses,
                loudness = timeline.loudness
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
            var stemBuses = StemBuses.Default
            var loudness = LoudnessTarget.Off
            var delivery = DeliveryOptions.Default
            val reviewNotes = ArrayList<ReviewNote>()
            val titles = ArrayList<TimelineTitle>()
            val vfx = ArrayList<TimelineVfx>()
            val titles3d = ArrayList<TimelineTitle3d>()
            val cg = ArrayList<TimelineCgLayer>()
            val markers = ArrayList<TimelineMarker>()
            val bins = ArrayList<ProjectBin>()
            val assemblies = ArrayList<AssemblyVersion>()
            var activeAssemblyId = ""
            var grade = TimelineGrade.Neutral
            var secondary = GradeSecondary.Off
            var matchRef = GradeMatchRef.Unset
            var layout = VideoLayout.Single
            var hdrMode = MovieHdrMode.Sdr
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
                        val clip = parseClip(parts)
                        if (clip == null) {
                            StudioDiagnostics.note("timeline", "rejected TC line")
                            return@forEach
                        }
                        val track = trackMap[parts[1]] ?: return@forEach
                        trackMap[parts[1]] = track.copy(clips = track.clips + clip)
                    }
                    "B" -> if (parts.size >= 8) {
                        val parsed = parseBed(parts)
                        if (parsed == null) {
                            StudioDiagnostics.note("timeline", "rejected B line")
                            return@forEach
                        }
                        bed = parsed
                    }
                    "V" -> if (parts.size >= 8) {
                        val parsed = parseBed(parts)
                        if (parsed == null) {
                            StudioDiagnostics.note("timeline", "rejected V line")
                            return@forEach
                        }
                        voice = parsed
                    }
                    "S" -> if (parts.size >= 8) {
                        val parsed = parseStem(parts)
                        if (parsed == null) {
                            StudioDiagnostics.note("timeline", "rejected S line")
                            return@forEach
                        }
                        stems += parsed
                    }
                    "SB" -> StemBuses.decodeLine(parts)?.let { stemBuses = it }
                    "LU" -> LoudnessTarget.decodeLine(parts)?.let { loudness = it }
                    "DO" -> DeliveryOptions.decodeDo(parts)?.let { delivery = it }
                    "RN" -> ReviewNote.decodeLine(parts)?.let { reviewNotes += it }
                    "X" -> AudioMasterFx.decode(parts)?.let { fx = it }
                    "TT" -> TimelineTitle.decodeLine(parts)?.let { titles += it }
                    "VX" -> TimelineVfx.decodeLine(parts)?.let { vfx += it }
                    "T3" -> TimelineTitle3d.decodeLine(parts)?.let { titles3d += it }
                    "CG" -> TimelineCgLayer.decodeLine(parts)?.let { cg += it }
                    "MK" -> TimelineMarker.decodeLine(parts)?.let { markers += it }
                    "BN" -> ProjectBin.decodeLine(parts)?.let { bins += it }
                    "AA" -> if (parts.size >= 2) activeAssemblyId = parts[1]
                    "AV" -> AssemblyVersion.decodeLine(parts)?.let { assemblies += it }
                    "G" -> TimelineGrade.decodeLine(parts)?.let { grade = it }
                    "GS" -> GradeSecondary.decodeLine(parts)?.let { secondary = it }
                    "GM" -> GradeMatchRef.decodeLine(parts)?.let { matchRef = it }
                    "L" -> if (parts.size >= 2) layout = VideoLayout.parse(parts[1])
                    "HDR" -> if (parts.size >= 2) hdrMode = MovieHdrMode.parse(parts[1])
                }
            }
            grade = grade.copy(secondary = secondary, matchRef = matchRef)
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
                layout = layout,
                hdrMode = hdrMode,
                vfx = vfx,
                titles3d = titles3d,
                cg = cg,
                markers = markers,
                bins = bins,
                activeAssemblyId = activeAssemblyId,
                assemblies = assemblies,
                stemBuses = stemBuses,
                loudness = loudness,
                delivery = delivery.copy(notes = reviewNotes)
            )
        }

        private fun parseClip(parts: List<String>): TimelineClip? {
            val speed = parts.getOrNull(6)?.toFloatOrNull() ?: return null
            val mediaIndex = parts.getOrNull(3)?.toIntOrNull() ?: return null
            val inMs = parts.getOrNull(4)?.toLongOrNull() ?: return null
            val outMs = parts.getOrNull(5)?.toLongOrNull() ?: return null
            val volume = parts.getOrNull(8)?.toFloatOrNull() ?: return null
            val fadeInMs = parts.getOrNull(9)?.toLongOrNull() ?: return null
            val fadeOutMs = parts.getOrNull(10)?.toLongOrNull() ?: return null
            return TimelineClip(
                id = parts[2],
                mediaIndex = mediaIndex,
                inMs = inMs,
                outMs = outMs,
                speed = speed,
                muted = parts[7] == "1",
                volume = volume,
                fadeInMs = fadeInMs,
                fadeOutMs = fadeOutMs,
                transition = ClipTransition.parse(parts.getOrNull(11)),
                transitionMs = parts.getOrNull(12)?.toLongOrNull() ?: 0L,
                speedEnd = parts.getOrNull(13)?.toFloatOrNull() ?: speed,
                motion = ClipMotion.decode(parts.getOrNull(14)),
                frame = ClipFrame.decode(parts.getOrNull(15)),
                chroma = ClipChroma.decode(parts.getOrNull(16)),
                slowMo = ClipSlowMo.decode(parts.getOrNull(17)),
                track = ClipTrack.decode(parts.getOrNull(18))
            )
        }

        private fun parseBed(parts: List<String>): AudioBed? {
            val volume = parts.getOrNull(2)?.toFloatOrNull() ?: return null
            val fadeInMs = parts.getOrNull(3)?.toLongOrNull() ?: return null
            val fadeOutMs = parts.getOrNull(4)?.toLongOrNull() ?: return null
            val duck = parts.getOrNull(6)?.toFloatOrNull() ?: return null
            val attackMs = parts.getOrNull(7)?.toLongOrNull() ?: return null
            return AudioBed(
                fileName = parts[1],
                volume = volume,
                fadeInMs = fadeInMs,
                fadeOutMs = fadeOutMs,
                muted = parts[5] == "1",
                duck = duck,
                attackMs = attackMs,
                releaseMs = parts.getOrNull(8)?.toLongOrNull() ?: 250L,
                slipMs = parts.getOrNull(9)?.toLongOrNull()?.let(AudioSync::clampSlip) ?: 0L
            )
        }

        private fun parseStem(parts: List<String>): AudioStem? {
            val volume = parts.getOrNull(3)?.toFloatOrNull() ?: return null
            val fadeInMs = parts.getOrNull(4)?.toLongOrNull() ?: return null
            val fadeOutMs = parts.getOrNull(5)?.toLongOrNull() ?: return null
            return AudioStem(
                id = parts[1],
                fileName = parts[2],
                volume = volume,
                fadeInMs = fadeInMs,
                fadeOutMs = fadeOutMs,
                muted = parts[6] == "1",
                role = parts[7].decodeTrack(),
                slipMs = parts.getOrNull(8)?.toLongOrNull()?.let(AudioSync::clampSlip) ?: 0L
            )
        }
    }
}

private fun String.encodeTrack(): String = replace(",", "%2C")
private fun String.decodeTrack(): String = replace("%2C", ",")
