package com.pixijoy.studio.quality

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityScorecardTest {
    @Test
    fun matrixRows_coverStudioShellMovieExportAndPacks() {
        val rows = parseMatrixCapabilities(matrixText())
        val sections = rows.map { it.section }.toSet()
        assertTrue(sections.contains("Shell"))
        assertTrue(sections.contains("Photo Studio"))
        assertTrue(sections.contains("Movie Studio"))
        assertTrue(sections.contains("Export"))
        assertTrue(sections.contains("Hard truth gates"))
        assertTrue(rows.any { it.name == "Heal" })
        assertTrue(rows.any { it.name == "Fill" })
        assertTrue(rows.any { it.name.startsWith("CG compositor") })
        assertTrue(rows.size >= 40)
    }

    @Test
    fun passFileGreensOneRow_andDoesNotGreenTheProject() {
        val row = MatrixCapability("Photo Studio", "Heal", "BAKED")
        val missing = scoreRow(row, null)
        assertEquals("NOT_GREEN", missing.quality)
        val dir = File(System.getProperty("java.io.tmpdir"), "pixijoy-quality-score")
        dir.mkdirs()
        val proof = File(dir, "heal.txt")
        proof.writeText("note\nresult: PASS\n")
        assertEquals("GREEN", scoreRow(row, proof).quality)
        val project = projectLine(
            listOf(
                ScoreLine(row.id, "GREEN", ""),
                ScoreLine("other", "NOT_GREEN", "")
            ),
            File(dir, "PROJECT.txt")
        )
        assertEquals("PROJECT NOT_GREEN", project)
        proof.delete()
    }

    @Test
    fun greenRequiresPassEvidence_andProjectStaysNotGreen() {
        val root = repoRoot()
        val evidence = File(root, "quality/evidence")
        val existing = File(root, "quality/SCORECARD.txt")
        if (existing.isFile) {
            for (line in existing.readLines()) {
                if (!line.contains("\tGREEN\t")) continue
                val id = line.substringBefore('\t')
                val proof = File(evidence, "$id.txt")
                assertTrue("$id was marked GREEN without evidence", proof.isFile &&
                    proof.readLines().any { it.trim() == "result: PASS" })
            }
        }
        val rows = parseMatrixCapabilities(matrixText()) + supplementalRows()
        val card = renderScorecard(parseMatrixCapabilities(matrixText()), evidence, File(evidence, "PROJECT.txt"))
        File(root, "quality").mkdirs()
        File(root, "quality/SCORECARD.txt").writeText(card)

        assertTrue(card.lineSequence().first() == "PROJECT NOT_GREEN")
        val body = card.lineSequence().filter { it.isNotEmpty() && !it.startsWith("#") }.drop(1).toList()
        assertEquals(rows.size, body.size)
        for (line in body) {
            val id = line.substringBefore('\t')
            val quality = line.substringAfter('\t').substringBefore('\t')
            if (quality == "GREEN") {
                val proof = File(evidence, "$id.txt")
                assertTrue("$id is GREEN without evidence", proof.isFile)
                assertTrue(
                    proof.readLines().any { it.trim() == "result: PASS" }
                )
            } else {
                assertEquals("NOT_GREEN", quality)
            }
        }
    }

    private fun repoRoot(): File {
        val cwd = System.getProperty("user.dir") ?: error("user.dir missing")
        return repoRootFrom(File(cwd))
    }

    private fun matrixText(): String =
        File(repoRoot(), "docs/FUNCTIONALITY_MATRIX.md").readText()
}
