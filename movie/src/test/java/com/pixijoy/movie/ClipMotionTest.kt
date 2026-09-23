package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ClipMotionTest {
    @Test
    fun sample_interpolatesScale() {
        val motion = ClipMotionPreset.ZoomIn.toMotion()
        assertEquals(1f, motion.sample(0f).scale, 0.01f)
        assertEquals(1.22f, motion.sample(1f).scale, 0.01f)
        val mid = motion.sample(0.5f).scale
        assertTrue(mid in 1.08f..1.14f)
    }

    @Test
    fun window_remapsLocalTimeline() {
        val motion = ClipMotionPreset.ZoomIn.toMotion().withWindow(0f, 0.5f)
        // local 1 → global 0.5 → halfway through zoom-in
        val end = motion.sample(1f).scale
        assertTrue(abs(end - 1.11f) < 0.05f)
    }

    @Test
    fun encodeDecode_roundTrip() {
        val original = ClipMotionPreset.Drift.toMotion()
        val decoded = ClipMotion.decode(original.encode())
        assertEquals(original.encode(), decoded.encode())
        assertTrue(decoded.matchesPreset(ClipMotionPreset.Drift))
        assertEquals("none", ClipMotion.None.encode())
        assertTrue(ClipMotion.decode(null).isIdentity)
    }

    @Test
    fun timelineAndMt_persistMotion() {
        val clip = TimelineClip(
            id = "c1",
            mediaIndex = 0,
            inMs = 0,
            outMs = 2000,
            motion = ClipMotionPreset.PanLeft.toMotion()
        )
        val flat = Timeline.decode(Timeline(listOf(clip)).encode(), 1) { 5000 }
        assertTrue(flat.clips[0].hasMotion)
        assertTrue(flat.clips[0].motion.matchesPreset(ClipMotionPreset.PanLeft))

        val mt = MultiTrackTimeline.decode(
            MultiTrackTimeline.fromFlatTimeline(Timeline(listOf(clip))).encode()
        )!!
        assertTrue(mt.tracks.first().clips.single().motion.matchesPreset(ClipMotionPreset.PanLeft))
    }

    @Test
    fun expandRamp_preservesMotionWindow() {
        val clip = TimelineClip(
            "a", 0, 0, 4000,
            speed = 0.8f,
            speedEnd = 1.4f,
            motion = ClipMotionPreset.ZoomIn.toMotion()
        )
        val steps = MovieSpeed.expandRamp(clip, steps = 4)
        assertEquals(4, steps.size)
        assertFalse(steps[0].hasSpeedRamp)
        assertTrue(steps[0].hasMotion)
        assertEquals(0f, steps.first().motion.windowStart, 0.01f)
        assertEquals(1f, steps.last().motion.windowEnd, 0.01f)
        assertTrue(steps[1].motion.windowStart > 0f)
    }
}
