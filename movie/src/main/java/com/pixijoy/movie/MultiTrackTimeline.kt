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
    val outputHeight: Int = 1080
) {
    fun videoTracks(): List<TimelineTrack> = tracks.filter { it.kind == TrackKind.Video }
    fun audioTracks(): List<TimelineTrack> = tracks.filter { it.kind == TrackKind.Audio }

    fun totalMs(): Long =
        videoTracks()
            .flatMap { it.clips }
            .maxOfOrNull { it.durationMs }
            ?.coerceAtLeast(1)
            ?: 1

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
                        "${clip.fadeInMs},${clip.fadeOutMs}"
                )
            }
        }
        bed?.let { appendLine("B,${it.fileName},${it.volume},${it.fadeInMs},${it.fadeOutMs},${if (it.muted) 1 else 0},${it.duck},${it.attackMs}") }
        voice?.let { appendLine("V,${it.fileName},${it.volume},${it.fadeInMs},${it.fadeOutMs},${if (it.muted) 1 else 0},${it.duck},${it.attackMs}") }
        if (!masterFx.isNeutral()) appendLine(masterFx.encode())
    }

    companion object {
        fun fromSingleVideo(clips: List<TimelineClip>): MultiTrackTimeline =
            MultiTrackTimeline(
                tracks = listOf(
                    TimelineTrack(id = "V1", kind = TrackKind.Video, name = "Video 1", clips = clips)
                )
            )

        fun decode(raw: String): MultiTrackTimeline? {
            if (!raw.lineSequence().any { it.startsWith("MT,") }) return null
            var version = 1
            var height = 1080
            val trackMap = linkedMapOf<String, TimelineTrack>()
            var bed: AudioBed? = null
            var voice: AudioBed? = null
            var fx = AudioMasterFx.Neutral
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
                        val clip = TimelineClip(
                            id = parts[2],
                            mediaIndex = parts[3].toInt(),
                            inMs = parts[4].toLong(),
                            outMs = parts[5].toLong(),
                            speed = parts[6].toFloat(),
                            muted = parts[7] == "1",
                            volume = parts[8].toFloat(),
                            fadeInMs = parts[9].toLong(),
                            fadeOutMs = parts[10].toLong()
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
                            attackMs = parts[7].toLong()
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
                            attackMs = parts[7].toLong()
                        )
                    }
                    "X" -> AudioMasterFx.decode(parts)?.let { fx = it }
                }
            }
            return MultiTrackTimeline(
                version = version,
                tracks = trackMap.values.toList(),
                bed = bed,
                voice = voice,
                masterFx = fx,
                outputHeight = height
            )
        }
    }
}

private fun String.encodeTrack(): String = replace(",", "%2C")
private fun String.decodeTrack(): String = replace("%2C", ",")
