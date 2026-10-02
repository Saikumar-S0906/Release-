package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun anchors_seedWhenEngineOff() {
        val track = ClipTrack(0.35f, 0.65f, 0.1f, TrackEngine.Off)
        val path = TrackPathBake.anchors("/missing.mp4", track, 2_000)
        assertFalse(path.isEmpty)
        assertEquals(0.35f, path.sample(0.25f)!!.first, 0.001f)
        assertEquals(0.65f, path.sample(0.75f)!!.second, 0.001f)
    }

    @Test
    fun anchors_seedWhenMlRequestedButUnavailable() {
        val prev = TrackFacade.mlAvailableOverride
        try {
            TrackFacade.mlAvailableOverride = false
            val track = ClipTrack(0.2f, 0.8f, 0.08f, TrackEngine.Ml)
            val path = TrackPathBake.anchors("/missing.mp4", track, 2_000)
            assertEquals(0.2f, path.sample(0.5f)!!.first, 0.001f)
            assertEquals(0.8f, path.sample(0.5f)!!.second, 0.001f)
            assertTrue(TrackPathBake.motion("/missing.mp4", track, 2_000).isIdentity)
        } finally {
            TrackFacade.mlAvailableOverride = prev
        }
    }
}
