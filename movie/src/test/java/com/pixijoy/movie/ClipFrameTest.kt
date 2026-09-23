package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipFrameTest {
    @Test
    fun encodeDecode_roundTrip() {
        val frame = ClipFrame(
            stabilize = 0.65f,
            cropLeft = -0.4f,
            cropRight = 0.4f,
            cropBottom = -1f,
            cropTop = 1f
        )
        val decoded = ClipFrame.decode(frame.encode())
        assertEquals(0.65f, decoded.stabilize, 0.01f)
        assertEquals(-0.4f, decoded.cropLeft, 0.01f)
        assertEquals(0.4f, decoded.cropRight, 0.01f)
        assertTrue(decoded.hasCrop)
        assertFalse(decoded.isIdentity)
        assertEquals("none", ClipFrame.None.encode())
        assertTrue(ClipFrame.decode(null).isIdentity)
    }

    @Test
    fun presets_applyIndependently() {
        val stab = ClipFrame.None.withStabilize(StabilizePreset.Strong)
        assertTrue(stab.matchesStabilize(StabilizePreset.Strong))
        assertTrue(stab.matchesReframe(ReframePreset.Original))

        val reframed = stab.withReframe(ReframePreset.Vertical)
        assertTrue(reframed.matchesStabilize(StabilizePreset.Strong))
        assertTrue(reframed.matchesReframe(ReframePreset.Vertical))
        assertEquals(StabilizePreset.Strong, StabilizePreset.matching(reframed))
    }

    @Test
    fun timelineRoundTrip_frame() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 2000,
            frame = ReframePreset.Close.toFrame().withStabilize(StabilizePreset.Light)
        )
        val decoded = Timeline.decode(Timeline(listOf(clip)).encode(), 1) { 5000 }
        assertTrue(decoded.clips[0].hasFrame)
        assertTrue(decoded.clips[0].frame.matchesReframe(ReframePreset.Close))
        assertTrue(decoded.clips[0].frame.matchesStabilize(StabilizePreset.Light))
    }

    @Test
    fun mtRoundTrip_frame() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(
                    TimelineClip(
                        id = "c0",
                        mediaIndex = 0,
                        inMs = 0,
                        outMs = 1500,
                        frame = ReframePreset.Left.toFrame()
                    )
                )
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        val frame = decoded.tracks.first().clips.single().frame
        assertTrue(frame.matchesReframe(ReframePreset.Left))
    }
}
