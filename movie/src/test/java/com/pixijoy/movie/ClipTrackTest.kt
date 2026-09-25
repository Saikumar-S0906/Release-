package com.pixijoy.movie

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipTrackTest {
    @After
    fun tearDown() {
        TrackFacade.mlAvailableOverride = null
    }

    @Test
    fun encodeDecode_roundTrip() {
        TrackFacade.mlAvailableOverride = true
        val track = ClipTrack(0.4f, 0.6f, 0.1f, TrackEngine.Ml)
        val decoded = ClipTrack.decode(track.encode())
        assertTrue(decoded.isActive)
        assertEquals(0.4f, decoded.x, 0.01f)
        assertEquals(0.6f, decoded.y, 0.01f)
        assertEquals(TrackEngine.Ml, decoded.engine)
        assertEquals("none", ClipTrack.None.encode())
        assertFalse(ClipTrack.decode(null).isActive)
    }

    @Test
    fun mlFallsBackUntilPack() {
        TrackFacade.mlAvailableOverride = false
        assertEquals(TrackEngine.Off, TrackFacade.resolve(TrackEngine.Ml))
        TrackFacade.mlAvailableOverride = true
        assertEquals(TrackEngine.Ml, TrackFacade.resolve(TrackEngine.Ml))
    }

    @Test
    fun baseBuild_isUnavailable() {
        TrackFacade.mlAvailableOverride = null
        assertFalse(TrackFacade.mlAvailable())
        assertEquals(TrackEngine.Off, TrackFacade.resolve(TrackEngine.Ml))
    }

    @Test
    fun timelineAndMtRoundTrip() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 1500,
            track = ClipTrack(0.5f, 0.5f, 0.08f, TrackEngine.Ml)
        )
        val flat = Timeline.decode(Timeline(listOf(clip)).encode(), 1) { 5000 }
        assertTrue(flat.clips[0].hasTrack)
        assertEquals(TrackEngine.Ml, flat.clips[0].track.engine)

        val mt = MultiTrackTimeline.decode(
            MultiTrackTimeline.fromFlatTimeline(Timeline(listOf(clip))).encode()
        )!!
        assertEquals(TrackEngine.Ml, mt.tracks.first().clips.single().track.engine)
    }
}
