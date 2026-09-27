package com.pixijoy.movie

/**
 * R8 / coding P2 — director assembly primitives on the timeline graph.
 *
 * Markers, bins, and named cut versions encode with the project graph.
 * [EditSnap] magnetizes playhead/trim to clip edges (I4).
 */

enum class MarkerKind {
    Beat,
    Soft,
    Hard,
    Vfx,
    Music,
    Note;

    companion object {
        fun parse(raw: String?): MarkerKind =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Note
    }
}

data class TimelineMarker(
    val id: String,
    val ms: Long,
    val kind: MarkerKind = MarkerKind.Beat,
    val note: String = ""
) {
    fun normalized(totalMs: Long = Long.MAX_VALUE): TimelineMarker = copy(
        ms = ms.coerceIn(0L, totalMs.coerceAtLeast(0L)),
        note = note.take(200)
    )

    fun encodeLine(): String {
        val n = normalized()
        return "MK,${n.id},${n.ms},${n.kind.name},${n.note.encodeField()}"
    }

    companion object {
        fun decodeLine(parts: List<String>): TimelineMarker? {
            if (parts.getOrNull(0) != "MK" || parts.size < 4) return null
            return runCatching {
                TimelineMarker(
                    id = parts[1].ifBlank { "mk0" },
                    ms = parts[2].toLong().coerceAtLeast(0),
                    kind = MarkerKind.parse(parts[3]),
                    note = parts.drop(4).joinToString(",").decodeField()
                )
            }.getOrNull()
        }
    }
}

data class ProjectBin(
    val id: String,
    val name: String,
    /** Media indices from the movie project media list. */
    val mediaIndices: List<Int> = emptyList()
) {
    fun encodeLine(): String =
        "BN,$id,${name.encodeField()},${mediaIndices.joinToString("|")}"

    companion object {
        fun decodeLine(parts: List<String>): ProjectBin? {
            if (parts.getOrNull(0) != "BN" || parts.size < 3) return null
            return runCatching {
                val indices = parts.getOrNull(3)
                    ?.split('|')
                    ?.mapNotNull { it.toIntOrNull()?.takeIf { i -> i >= 0 } }
                    .orEmpty()
                ProjectBin(
                    id = parts[1].ifBlank { "bin0" },
                    name = parts[2].decodeField().ifBlank { "Bin" },
                    mediaIndices = indices.distinct()
                )
            }.getOrNull()
        }

        fun defaults(): List<ProjectBin> = listOf(
            ProjectBin("bin_a", "A-cam"),
            ProjectBin("bin_b", "B-cam"),
            ProjectBin("bin_sel", "Selects"),
            ProjectBin("bin_vfx", "VFX plates")
        )
    }
}

/**
 * Named alternate cut. [cutEncode] is a full [MultiTrackTimeline.encode] without
 * nested assembly lines (assemblies stripped before snapshot).
 */
data class AssemblyVersion(
    val id: String,
    val name: String,
    val cutEncode: String
) {
    fun encodeLine(): String =
        "AV,$id,${name.encodeField()},${cutEncode.encodeField()}"

    companion object {
        fun decodeLine(parts: List<String>): AssemblyVersion? {
            if (parts.getOrNull(0) != "AV" || parts.size < 4) return null
            return runCatching {
                AssemblyVersion(
                    id = parts[1].ifBlank { "av0" },
                    name = parts[2].decodeField().ifBlank { "Cut" },
                    cutEncode = parts.drop(3).joinToString(",").decodeField()
                )
            }.getOrNull()
        }
    }
}

/** Magnetic snap of a time to nearby clip edges / markers. */
object EditSnap {
    const val DEFAULT_THRESHOLD_MS = 120L

    fun clipEdges(clips: List<TimelineClip>): List<Long> {
        val edges = ArrayList<Long>()
        var t = 0L
        edges += 0L
        for (clip in clips) {
            t += clip.durationMs
            edges += t
        }
        return edges.distinct().sorted()
    }

    fun snapMs(
        ms: Long,
        edges: List<Long>,
        thresholdMs: Long = DEFAULT_THRESHOLD_MS,
        totalMs: Long = Long.MAX_VALUE
    ): Long {
        val clamped = ms.coerceIn(0L, totalMs.coerceAtLeast(0L))
        if (edges.isEmpty() || thresholdMs <= 0L) return clamped
        var best = clamped
        var bestDist = thresholdMs + 1
        for (e in edges) {
            val d = kotlin.math.abs(e - clamped)
            if (d <= thresholdMs && d < bestDist) {
                bestDist = d
                best = e
            }
        }
        return best.coerceIn(0L, totalMs.coerceAtLeast(0L))
    }

    fun snapPlayhead(ms: Long, timeline: Timeline, thresholdMs: Long = DEFAULT_THRESHOLD_MS): Long =
        snapMs(ms, clipEdges(timeline.clips), thresholdMs, timeline.totalMs())
}

internal fun String.encodeField(): String =
    replace("%", "%25").replace(",", "%2C").replace("\n", "%0A")

internal fun String.decodeField(): String =
    replace("%0A", "\n").replace("%2C", ",").replace("%25", "%")
