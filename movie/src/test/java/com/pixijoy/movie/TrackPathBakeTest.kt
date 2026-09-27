package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackPathBakeTest {
    @Test
    fun pan_pullsPointTowardCenter() {
        // Point on the right → pan left (negative tx).
        assertTrue(TrackPathBake.panX(0.75f) < 0f)
        // Point below center → pan up (negative ty in NDC-ish).
        assertTrue(TrackPathBake.panY(0.8f) < 0f)
        assertEquals(0f, TrackPathBake.panX(0.5f), 0.001f)
        assertEquals(0f, TrackPathBake.panY(0.5f), 0.001f)
    }
}
