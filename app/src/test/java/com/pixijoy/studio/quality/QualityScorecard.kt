package com.pixijoy.studio.quality

import java.io.File

/**
 * Phase 0 ledger. Product status in the functionality matrix is not quality.
 * A row is GREEN only when quality/evidence/<id>.txt contains a line `result: PASS`.
 * The project line stays NOT_GREEN until every row is GREEN and
 * quality/evidence/PROJECT.txt contains `accepted: phase5-out-of-scope`.
 * Neither of those is true in phase 0.
 */
data class MatrixCapability(
    val section: String,
    val name: String,
    val productStatus: String
) {
    val id: String
        get() = slug("$section $name")
}

data class ScoreLine(
    val id: String,
    val quality: String,
    val detail: String
)

private val productStatus = Regex("""\*\*(LIVE|BAKED|FALLBACK|UNAVAILABLE|PACK)\*\*""")

internal fun slug(raw: String): String =
    raw.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

internal fun parseMatrixCapabilities(markdown: String): List<MatrixCapability> {
    var section = ""
    val rows = ArrayList<MatrixCapability>()
    for (raw in markdown.lineSequence()) {
        val line = raw.trim()
        if (line.startsWith("## ")) {
            section = line.removePrefix("## ").trim()
            continue
        }
        if (!line.startsWith("|") || section.isEmpty()) continue
        val cells = line.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        if (cells.size < 2) continue
        val status = productStatus.find(cells[1])?.groupValues?.get(1) ?: continue
        val name = cells[0].trim()
        if (name.equals("Capability", ignoreCase = true) ||
            name.equals("Surface", ignoreCase = true) ||
            name.equals("Output", ignoreCase = true) ||
            name.equals("Status", ignoreCase = true)
        ) {
            continue
        }
        rows += MatrixCapability(section, name, status)
    }
    return rows
}

internal fun scoreRow(row: MatrixCapability, evidence: File?): ScoreLine {
    val text = evidence?.takeIf { it.isFile }?.readText().orEmpty()
    val pass = text.lineSequence().any { it.trim() == "result: PASS" }
    return if (pass) {
        ScoreLine(row.id, "GREEN", "evidence result: PASS; product status ${row.productStatus} is not this result")
    } else {
        val recorded = text.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("result:") }
        val why = if (recorded != null) {
            "evidence $recorded; product status ${row.productStatus} is not a quality result"
        } else {
            "no passing evidence file; product status ${row.productStatus} is not a quality result"
        }
        ScoreLine(row.id, "NOT_GREEN", why)
    }
}

internal fun projectLine(rows: List<ScoreLine>, projectEvidence: File?): String {
    val allGreen = rows.isNotEmpty() && rows.all { it.quality == "GREEN" }
    val accepted = projectEvidence?.takeIf { it.isFile }?.readText().orEmpty()
        .lineSequence()
        .any { it.trim() == "accepted: phase5-out-of-scope" }
    return if (allGreen && accepted) "PROJECT GREEN" else "PROJECT NOT_GREEN"
}

/**
 * Finer than a matrix line that bundles several tools.
 * A pass here does not turn the bundled matrix row green.
 */
internal fun supplementalRows(): List<MatrixCapability> = listOf(
    MatrixCapability("Photo Studio", "Sky heuristic", "BAKED"),
    // Plate response only. The matrix CG row also covers Filament, glTF, and soft Z.
    MatrixCapability("Movie Studio", "Classical cube plate", "LIVE"),
    MatrixCapability("Movie Studio", "Classical ground plate", "LIVE"),
    MatrixCapability("Movie Studio", "Classical sky plate", "LIVE"),
    MatrixCapability("Movie Studio", "Classical dust plate", "LIVE"),
    MatrixCapability("Hard truth gates", "Inpaint contract smoke", "PACK"),
    MatrixCapability("Hard truth gates", "Depth contract smoke", "PACK"),
    MatrixCapability("Hard truth gates", "Inpaint contract quality", "PACK"),
    MatrixCapability("Hard truth gates", "Depth contract quality", "PACK")
)

internal fun renderScorecard(
    rows: List<MatrixCapability>,
    evidenceDir: File,
    projectEvidence: File
): String {
    val scored = rows + supplementalRows()
    val lines = scored.map { scoreRow(it, File(evidenceDir, "${it.id}.txt")) }
    val body = StringBuilder()
    body.append(projectLine(lines, projectEvidence))
    body.append('\n')
    body.append("# Generated from docs/FUNCTIONALITY_MATRIX.md. GREEN requires quality/evidence/<id>.txt with result: PASS.\n")
    body.append("# A Maestro tap is not evidence. Product LIVE/BAKED/PACK is not evidence.\n")
    for ((row, line) in scored.zip(lines)) {
        body.append(line.id)
        body.append('\t')
        body.append(line.quality)
        body.append('\t')
        body.append(row.section)
        body.append(" / ")
        body.append(row.name)
        body.append(" — ")
        body.append(line.detail)
        body.append('\n')
    }
    return body.toString()
}

internal fun repoRootFrom(start: File): File {
    var dir: File? = start
    repeat(8) {
        val current = dir ?: return@repeat
        if (File(current, "docs/FUNCTIONALITY_MATRIX.md").isFile &&
            File(current, "gradlew.bat").isFile
        ) {
            return current
        }
        dir = current.parentFile
    }
    error("repo root not found from ${start.absolutePath}")
}
