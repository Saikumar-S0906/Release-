package com.pixijoy.movie

import java.io.File

/**
 * R9 / P6 — write review notes, EDL-lite, FCPXML-lite, and export metadata
 * next to the project (not into MediaStore).
 *
 * **Gaps (documented):**
 * - EDL is CMX3600-ish SOURCE→RECORD; no transitions / AX reel names from real tape.
 * - FCPXML is a minimal cut list Resolve/Premiere may need repair on exotic rates.
 * - No ProRes proxy; Review preset is H.264 1080p SDR.
 * - Metadata is a JSON sidecar (Media3 does not embed freeform title/LUT in MP4 here).
 */
object DeliveryHandoff {
    fun handoffDir(projectDir: File): File =
        File(projectDir, "handoff").also { it.mkdirs() }

    fun writeAll(
        projectDir: File,
        projectTitle: String,
        multi: MultiTrackTimeline,
        mediaNames: List<String>
    ): DeliveryArtifacts {
        val dir = handoffDir(projectDir)
        val notesFile = File(dir, "review-notes.json")
        val edlFile = File(dir, "cut.edl")
        val xmlFile = File(dir, "cut.fcpxml")
        val metaFile = File(dir, "export-meta.json")
        notesFile.writeText(ReviewNotesJson.encode(projectTitle, multi))
        edlFile.writeText(EdlLite.encode(projectTitle, multi, mediaNames))
        xmlFile.writeText(FcpxmlLite.encode(projectTitle, multi, mediaNames))
        metaFile.writeText(ExportMetadataJson.encode(projectTitle, multi))
        return DeliveryArtifacts(notesFile, edlFile, xmlFile, metaFile)
    }
}

data class DeliveryArtifacts(
    val notesJson: File,
    val edl: File,
    val fcpxml: File,
    val metadataJson: File
)

object ReviewNotesJson {
    fun encode(projectTitle: String, multi: MultiTrackTimeline): String = buildString {
        append("{\n")
        append("  \"title\": ").append(jsonStr(projectTitle)).append(",\n")
        append("  \"watermark\": false,\n")
        append("  \"burnInTc\": ").append(multi.delivery.burnInTc).append(",\n")
        append("  \"preset\": ").append(jsonStr(multi.delivery.preset.name)).append(",\n")
        append("  \"notes\": [\n")
        val items = ArrayList<String>()
        multi.delivery.notes.forEach { note ->
            items += "    {\"id\": ${jsonStr(note.id)}, \"ms\": ${note.ms}, " +
                "\"tc\": ${jsonStr(DeliveryTc.format(note.ms))}, \"text\": ${jsonStr(note.text)}}"
        }
        multi.markers.forEach { mk ->
            items += "    {\"id\": ${jsonStr(mk.id)}, \"ms\": ${mk.ms}, " +
                "\"tc\": ${jsonStr(DeliveryTc.format(mk.ms))}, \"kind\": ${jsonStr(mk.kind.name)}, " +
                "\"text\": ${jsonStr(mk.note.ifBlank { mk.kind.name })}, \"fromMarker\": true}"
        }
        append(items.joinToString(",\n"))
        if (items.isNotEmpty()) append('\n')
        append("  ]\n")
        append("}\n")
    }
}

object ExportMetadataJson {
    fun encode(projectTitle: String, multi: MultiTrackTimeline): String {
        val versionName = multi.assemblies.firstOrNull { it.id == multi.activeAssemblyId }?.name
            ?: multi.assemblies.lastOrNull()?.name
            ?: "Working cut"
        val look = multi.grade.lookKey.ifBlank { "none" }
        return buildString {
            append("{\n")
            append("  \"title\": ").append(jsonStr(projectTitle)).append(",\n")
            append("  \"version\": ").append(jsonStr(versionName)).append(",\n")
            append("  \"activeAssemblyId\": ").append(jsonStr(multi.activeAssemblyId)).append(",\n")
            append("  \"look\": ").append(jsonStr(look)).append(",\n")
            append("  \"lut\": ").append(jsonStr(look)).append(",\n")
            append("  \"secondary\": ").append(multi.grade.secondary.enabled).append(",\n")
            append("  \"preset\": ").append(jsonStr(multi.delivery.preset.name)).append(",\n")
            append("  \"height\": ").append(MovieExportSize.normalize(multi.outputHeight)).append(",\n")
            append("  \"hdr\": ").append(jsonStr(multi.hdrMode.name)).append(",\n")
            append("  \"watermark\": false,\n")
            append("  \"burnInTc\": ").append(multi.delivery.burnInTc).append("\n")
            append("}\n")
        }
    }
}

object EdlLite {
    /**
     * CMX3600-ish event list. Gaps: no dissolve events; reel = AX; fps fixed 24.
     */
    fun encode(projectTitle: String, multi: MultiTrackTimeline, mediaNames: List<String>): String {
        val clips = multi.toFlatTimeline().clips
        val sb = StringBuilder()
        sb.appendLine("TITLE: ${projectTitle.take(80)}")
        sb.appendLine("FCM: NON-DROP FRAME")
        sb.appendLine()
        var rec = 0L
        clips.forEachIndexed { i, clip ->
            val name = mediaNames.getOrElse(clip.mediaIndex) { "CLIP${clip.mediaIndex}" }
            val srcIn = DeliveryTc.format(clip.inMs)
            val srcOut = DeliveryTc.format(clip.outMs)
            val recIn = DeliveryTc.format(rec)
            val recOut = DeliveryTc.format(rec + clip.durationMs)
            val evt = (i + 1).toString().padStart(3, '0')
            sb.appendLine("$evt  AX       V     C        $srcIn $srcOut $recIn $recOut")
            sb.appendLine("* FROM CLIP NAME: $name")
            sb.appendLine("* CLIP ID: ${clip.id}")
            rec += clip.durationMs
        }
        multi.markers.sortedBy { it.ms }.forEach { mk ->
            sb.appendLine("* MARKER ${mk.kind.name} ${DeliveryTc.format(mk.ms)} ${mk.note}")
        }
        return sb.toString()
    }
}

object FcpxmlLite {
    /**
     * Minimal FCPXML 1.9 cut list. Gaps: no compound clips, no color, audio beds omitted.
     */
    fun encode(projectTitle: String, multi: MultiTrackTimeline, mediaNames: List<String>): String {
        val clips = multi.toFlatTimeline().clips
        val totalFrames = ((multi.totalMs() * DeliveryTc.Fps) / 1000L).coerceAtLeast(1)
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<!DOCTYPE fcpxml>""")
        sb.appendLine("""<fcpxml version="1.9">""")
        sb.appendLine("""  <resources>""")
        mediaNames.forEachIndexed { i, name ->
            sb.appendLine(
                """    <asset id="r$i" name="${xml(name)}" start="0s" duration="3600s" """ +
                    """hasVideo="1" hasAudio="1" format="rfmt"/>"""
            )
        }
        sb.appendLine(
            """    <format id="rfmt" name="FFVideoFormat1080p24" frameDuration="100/2400s" """ +
                """width="1920" height="1080"/>"""
        )
        sb.appendLine("""  </resources>""")
        sb.appendLine("""  <library>""")
        sb.appendLine("""    <event name="${xml(projectTitle)}">""")
        sb.appendLine(
            """      <project name="${xml(projectTitle)}" uid="pixijoy-cut">"""
        )
        sb.appendLine(
            """        <sequence format="rfmt" duration="${totalFrames}/${DeliveryTc.Fps * 100}s" tcStart="0s">"""
        )
        sb.appendLine("""          <spine>""")
        clips.forEach { clip ->
            val frames = ((clip.durationMs * DeliveryTc.Fps) / 1000L).coerceAtLeast(1)
            val offset = ((clip.inMs * DeliveryTc.Fps) / 1000L)
            val ref = "r${clip.mediaIndex}"
            sb.appendLine(
                """            <asset-clip ref="$ref" offset="${offset}s" """ +
                    """duration="${frames}/${DeliveryTc.Fps}s" name="${xml(clip.id)}"/>"""
            )
        }
        multi.markers.forEach { mk ->
            val t = DeliveryTc.format(mk.ms)
            sb.appendLine(
                """            <!-- marker ${xml(mk.kind.name)} @ $t ${xml(mk.note)} -->"""
            )
        }
        sb.appendLine("""          </spine>""")
        sb.appendLine("""        </sequence>""")
        sb.appendLine("""      </project>""")
        sb.appendLine("""    </event>""")
        sb.appendLine("""  </library>""")
        sb.appendLine("""</fcpxml>""")
        return sb.toString()
    }

    private fun xml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
}

private fun jsonStr(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r") + "\""
