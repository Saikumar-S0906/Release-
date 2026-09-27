package com.pixijoy.movie

/**
 * R9 / coding P6 — delivery & review handoff.
 *
 * Watermark is always off (product rule). Burn-in TC is optional.
 * Presets nudge height + HDR intent; EDL/FCPXML/notes are sidecar files.
 *
 * Encode: `DO,<burnInTc>,0,<preset>` then `RN,<id>,<ms>,<text>`
 */
enum class DeliverablePreset {
    /** 720p SDR — social / stories. */
    Social,
    /** 1080p SDR — review / proxy-class. */
    Review,
    /** 1080/4K + Keep HDR when timeline already tall. */
    Master;

    fun apply(mt: MultiTrackTimeline): MultiTrackTimeline {
        val next = when (this) {
            Social -> mt.copy(outputHeight = 720, hdrMode = MovieHdrMode.Sdr)
            Review -> mt.copy(outputHeight = 1080, hdrMode = MovieHdrMode.Sdr)
            Master -> mt.copy(
                outputHeight = if (mt.outputHeight >= 1800) 2160 else 1080,
                hdrMode = MovieHdrMode.KeepHdr
            )
        }
        return next.copy(delivery = next.delivery.copy(preset = this))
    }

    companion object {
        fun parse(raw: String?): DeliverablePreset =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Review
    }
}

data class ReviewNote(
    val id: String,
    val ms: Long,
    val text: String
) {
    fun normalized(totalMs: Long = Long.MAX_VALUE): ReviewNote = copy(
        ms = ms.coerceIn(0L, totalMs.coerceAtLeast(0L)),
        text = text.trim().take(400)
    )

    fun encodeLine(): String {
        val n = normalized()
        return "RN,${n.id},${n.ms},${n.text.encodeField()}"
    }

    companion object {
        fun decodeLine(parts: List<String>): ReviewNote? {
            if (parts.getOrNull(0) != "RN" || parts.size < 4) return null
            return runCatching {
                ReviewNote(
                    id = parts[1].ifBlank { "rn0" },
                    ms = parts[2].toLong().coerceAtLeast(0),
                    text = parts.drop(3).joinToString(",").decodeField()
                )
            }.getOrNull()
        }
    }
}

data class DeliveryOptions(
    val burnInTc: Boolean = false,
    /** Always false — PixiJoy never watermarks. Kept for honesty in encode/UI. */
    val watermark: Boolean = false,
    val preset: DeliverablePreset = DeliverablePreset.Review,
    val notes: List<ReviewNote> = emptyList()
) {
    fun normalized(): DeliveryOptions = copy(
        watermark = false,
        notes = notes.map { it.normalized() }.filter { it.text.isNotBlank() }
    )

    fun isDefault(): Boolean =
        !burnInTc && !watermark && preset == DeliverablePreset.Review && notes.isEmpty()

    fun encodeLines(): List<String> {
        val n = normalized()
        val out = ArrayList<String>()
        if (!n.isDefault() || n.preset != DeliverablePreset.Review || n.burnInTc) {
            out += "DO,${if (n.burnInTc) 1 else 0},0,${n.preset.name}"
        }
        n.notes.forEach { out += it.encodeLine() }
        return out
    }

    companion object {
        val Default = DeliveryOptions()

        fun decodeDo(parts: List<String>): DeliveryOptions? {
            if (parts.getOrNull(0) != "DO" || parts.size < 4) return null
            return DeliveryOptions(
                burnInTc = parts[1] == "1",
                watermark = false,
                preset = DeliverablePreset.parse(parts[3])
            )
        }
    }
}

/** Timecode helpers for burn-in + EDL (non-drop, 24 fps nominal for handoff). */
object DeliveryTc {
    const val Fps = 24

    fun format(ms: Long, fps: Int = Fps): String {
        val totalFrames = ((ms.coerceAtLeast(0) * fps) / 1000L)
        val ff = (totalFrames % fps).toInt()
        val totalSec = totalFrames / fps
        val ss = (totalSec % 60).toInt()
        val totalMin = totalSec / 60
        val mm = (totalMin % 60).toInt()
        val hh = (totalMin / 60).toInt().coerceAtMost(99)
        return "%02d:%02d:%02d:%02d".format(hh, mm, ss, ff)
    }

    fun parseToMs(tc: String, fps: Int = Fps): Long {
        val p = tc.trim().split(':')
        if (p.size != 4) return 0L
        return runCatching {
            val hh = p[0].toLong()
            val mm = p[1].toLong()
            val ss = p[2].toLong()
            val ff = p[3].toLong()
            (((hh * 60 + mm) * 60 + ss) * fps + ff) * 1000L / fps
        }.getOrDefault(0L)
    }
}
