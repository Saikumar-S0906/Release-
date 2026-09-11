package com.pixijoy.ai.ui.board

import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.theme.PixiMotionMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 5 — Bible §27 timing + manifest key coverage. */
class BoardMotionUnitTest {
    @Test
    fun pixiMotionMs_matchesBibleBands() {
        assertTrue(PixiMotionMs.Tap in 100..150)
        assertTrue(PixiMotionMs.Fast in 150..250)
        assertTrue(PixiMotionMs.Normal in 150..250)
        assertTrue(PixiMotionMs.Component in 200..300)
        assertTrue(PixiMotionMs.Screen in 250..400)
        assertTrue(PixiMotionMs.Slow in 250..400)
    }

    @Test
    fun motionManifestKeys_coverAllEntries() {
        val expected = setOf(
            "generation_progress",
            "sparkle_success",
            "sticker_pop",
            "before_after",
            "share_success",
            "favorite",
            "remix",
            "upload_processing",
            "template_preview"
        )
        assertEquals(expected, BoardMotionKey.entries.map { it.manifestId }.toSet())
        assertEquals(BoardMotionKey.SparkleSuccess, BoardMotionKey.fromManifestId("sparkle_success"))
    }
}
