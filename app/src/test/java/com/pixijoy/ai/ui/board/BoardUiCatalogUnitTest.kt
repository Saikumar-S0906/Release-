package com.pixijoy.ai.ui.board

import com.pixijoy.ai.testing.BoardFlowJourneys
import com.pixijoy.ai.ui.board.ctools.BoardCTool
import com.pixijoy.ai.ui.board.BoardRouteMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM functional tests for board catalog wireup and journey coverage. */
class BoardUiCatalogUnitTest {

    @Test
    fun catalog_isNotEmpty_andIdsUnique() {
        assertTrue(BoardUiCatalog.all.size >= 200)
        val ids = BoardUiCatalog.all.map { it.id }
        assertEquals("Duplicate catalog ids", ids.size, ids.toSet().size)
    }

    @Test
    fun catalog_allScreensMarkedDone() {
        val pending = BoardUiCatalog.all.filter { it.status != BoardScreenEntry.Status.Done }
        assertTrue(
            "Pending screens: ${pending.map { it.id }}",
            pending.isEmpty()
        )
    }

    @Test
    fun catalog_doneMeansSoftAiReleaseLabel() {
        BoardUiCatalog.all.forEach { entry ->
            assertEquals(
                "UI Done / soft AI",
                entry.status.releaseLabel
            )
        }
    }

    @Test
    fun catalog_rematchBoardFilesAreCanonical() {
        assertEquals(
            "REMATCH_05_Make_it_Beautiful.png",
            BoardUiCatalog.find("b5_choose")!!.boardFile
        )
        assertEquals(
            "REMATCH_06_Make_it_Funny.png",
            BoardUiCatalog.find("b6_choose")!!.boardFile
        )
        assertEquals(
            "REMATCH_07_Tell_PixiJoy.png",
            BoardUiCatalog.find("b7_sheet")!!.boardFile
        )
        assertEquals(
            "REMATCH_13_Sticker.png",
            BoardUiCatalog.find("b13_photo")!!.boardFile
        )
        assertEquals(
            "REMATCH_14_Move.png",
            BoardUiCatalog.find("b14_input")!!.boardFile
        )
        assertEquals(
            "REMATCH_EXTRA_Different.png",
            BoardUiCatalog.find("b_diff_choose")!!.boardFile
        )
    }

    @Test
    fun catalog_primaryBoardFiles_areNonBlank() {
        val withFile = BoardUiCatalog.all.filter { it.boardFile.isNotBlank() }
        assertTrue(withFile.size >= 40)
        withFile.forEach { entry ->
            assertFalse(entry.id, entry.boardFile.contains(" "))
        }
    }

    @Test
    fun productionSearch_opensExploreSearch() {
        assertEquals("b2_search", BoardRouteMap.library("search"))
    }

    @Test
    fun catalog_find_resolvesKnownEntries() {
        listOf(
            "b1_splash", "b2_home", "b5_choose", "b6_choose", "b7_sheet",
            "b_diff_choose", "b13_photo", "b14_input", "b15_open", "b_c07",
            "b_c07_processing", "b15_offline", "b16_safety", "b17_low_credits"
        ).forEach { id ->
            assertNotNull("Missing catalog entry $id", BoardUiCatalog.find(id))
        }
    }

    @Test
    fun cTools_allHaveIntroAndEdgeScreens() {
        BoardCTool.entries.forEach { tool ->
            assertNotNull(BoardUiCatalog.find(tool.prefix))
            assertNotNull(BoardUiCatalog.find("${tool.prefix}_offline"))
            assertNotNull(BoardUiCatalog.find("${tool.prefix}_low_credits"))
            assertNotNull(BoardUiCatalog.find("${tool.prefix}_safety"))
        }
    }
}

class BoardCToolUnitTest {

    @Test
    fun fromScreenId_resolvesAllTools() {
        BoardCTool.entries.forEach { tool ->
            assertEquals(tool, BoardCTool.fromScreenId(tool.prefix))
            assertEquals(tool, BoardCTool.fromScreenId("${tool.prefix}_processing"))
            assertEquals(tool, BoardCTool.fromScreenId("${tool.prefix}_offline"))
        }
    }

    @Test
    fun stepOf_parsesSuffix() {
        assertEquals("intro", BoardCTool.stepOf("b_c07"))
        assertEquals("processing", BoardCTool.stepOf("b_c07_processing"))
        assertEquals("low_credits", BoardCTool.stepOf("b_c04_low_credits"))
        assertEquals("consent_denied", BoardCTool.stepOf("b_c06_consent_denied"))
    }

    @Test
    fun sceneTool_hasProcessingInCatalog() {
        assertNotNull(BoardUiCatalog.find("b_c07_processing"))
        assertEquals(BoardCTool.Scene, BoardCTool.fromScreenId("b_c07_adjust"))
    }
}

class BoardFlowJourneysUnitTest {

    @Test
    fun journeys_coverCoreProductFlows() {
        val ids = BoardFlowJourneys.all.map { it.id }.toSet()
        listOf(
            "F01", "F02", "F05", "F06", "F07", "DIFF", "F13", "F14",
            "C04", "C05", "C06", "C07", "C08", "C09", "C10", "C11", "C12", "C14",
            "F15", "F16", "F17", "F28", "CREATE", "DAY"
        ).forEach { assertTrue("Missing journey $it", ids.contains(it)) }
    }

    @Test
    fun everyJourneyScreen_existsInCatalog() {
        val missing = BoardFlowJourneys.allReferencedIds()
            .filter { BoardUiCatalog.find(it) == null }
            .sorted()
        assertTrue(
            "Journey ids missing from catalog:\n${missing.joinToString("\n")}",
            missing.isEmpty()
        )
    }

    @Test
    fun everyJourney_hasNonEmptyHappyPath() {
        BoardFlowJourneys.all.forEach { journey ->
            assertTrue(journey.id, journey.steps.isNotEmpty())
            assertEquals(journey.id, journey.entry, journey.steps.first())
        }
    }

    @Test
    fun rematchAndCTool_journeysDeclareEdges() {
        val requireEdges = setOf(
            "C00", "C04", "C05", "C06", "C07", "C08", "C09", "C10", "C11", "C12", "C14",
            "F05", "F06", "F07", "DIFF", "F13", "F14", "F15", "F16", "F17", "F19"
        )
        BoardFlowJourneys.all
            .filter { it.id in requireEdges }
            .forEach { journey ->
                assertTrue("${journey.id} needs edges", journey.edgeIds.size >= 3)
            }
    }
}
