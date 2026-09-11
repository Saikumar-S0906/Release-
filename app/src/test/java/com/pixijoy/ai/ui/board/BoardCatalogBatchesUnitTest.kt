package com.pixijoy.ai.ui.board

import com.pixijoy.ai.testing.BoardCatalogBatches
import com.pixijoy.ai.testing.BoardFlowJourneys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM coverage for batched catalog QA + day-in-life path. */
class BoardCatalogBatchesUnitTest {

    @Test
    fun batches_coverEntireCatalog_withoutOverlap() {
        assertTrue(
            "Uncovered: ${BoardCatalogBatches.uncoveredIds()}",
            BoardCatalogBatches.uncoveredIds().isEmpty()
        )
        assertTrue(
            "Overlaps: ${BoardCatalogBatches.overlappingIds()}",
            BoardCatalogBatches.overlappingIds().isEmpty()
        )
    }

    @Test
    fun eachBatch_hasScreens() {
        BoardCatalogBatches.all.forEach { batch ->
            val ids = BoardCatalogBatches.screenIds(batch.id)
            assertTrue(batch.id, ids.isNotEmpty())
        }
    }

    @Test
    fun goldenKeys_existInCatalog() {
        BoardCatalogBatches.goldenKeyScreenIds.forEach { id ->
            assertTrue(id, BoardUiCatalog.find(id) != null)
        }
    }

    @Test
    fun dayInLife_matchesJourneyAndCatalog() {
        val day = BoardFlowJourneys.byId("DAY")
        assertEquals(BoardCatalogBatches.dayInLifeSteps, day.steps)
        BoardCatalogBatches.dayInLifeSteps.forEach { id ->
            assertTrue(id, BoardUiCatalog.find(id) != null)
        }
        assertEquals("b2_home", day.entry)
        assertEquals("b18_browse", day.steps.last())
    }
}
