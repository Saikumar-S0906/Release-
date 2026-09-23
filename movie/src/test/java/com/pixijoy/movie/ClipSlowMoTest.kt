package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test

class ClipSlowMoTest {
    @After
    fun tearDown() {
        FrameInterp.mlAvailableOverride = null
    }

    @Test
    fun encodeDecode_roundTrip() {
        val slow = ClipSlowMo(rate = 0.25f, engine = SlowMoEngine.Heuristic)
        val decoded = ClipSlowMo.decode(slow.encode())
        assertTrue(decoded.isActive)
        assertEquals(0.25f, decoded.rate, 0.01f)
        assertEquals(SlowMoEngine.Heuristic, decoded.engine)
        assertEquals("none", ClipSlowMo.None.encode())
        assertFalse(ClipSlowMo.decode(null).isActive)
    }

    @Test
    fun presets_andDuration() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 1000,
            slowMo = SlowMoPreset.Quarter.toSlowMo()
        )
        assertTrue(clip.hasSlowMo)
        assertEquals(4000L, clip.durationMs)
        assertEquals(0.25f, MovieSpeed.playbackRate(clip), 0.01f)
        assertTrue(clip.slowMo.matchesPreset(SlowMoPreset.Quarter))
    }

    @Test
    fun frameInterp_mlFallsBackUntilPack() {
        FrameInterp.mlAvailableOverride = false
        val slow = ClipSlowMo(rate = 0.125f, engine = SlowMoEngine.Ml)
        assertEquals(SlowMoEngine.Heuristic, slow.resolvedEngine())
        FrameInterp.mlAvailableOverride = true
        assertEquals(SlowMoEngine.Ml, slow.resolvedEngine())
        assertEquals("ml", FrameInterp.engineLabelKey(SlowMoEngine.Ml))
    }

    @Test
    fun expandRamp_defersWhenSlowMo() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 2000,
            speed = 0.5f,
            speedEnd = 2f,
            slowMo = SlowMoPreset.Half.toSlowMo()
        )
        assertFalse(clip.hasSpeedRamp)
        val expanded = MovieSpeed.expandRamp(clip)
        assertEquals(1, expanded.size)
        assertEquals(0.5f, expanded[0].speed, 0.01f)
    }

    @Test
    fun timelineAndMtRoundTrip() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 1500,
            slowMo = SlowMoPreset.Eighth.toSlowMo()
        )
        val flat = Timeline.decode(Timeline(listOf(clip)).encode(), 1) { 5000 }
        assertTrue(flat.clips[0].hasSlowMo)
        assertEquals(0.125f, flat.clips[0].slowMo.rate, 0.01f)

        val mt = MultiTrackTimeline.decode(
            MultiTrackTimeline.fromFlatTimeline(Timeline(listOf(clip))).encode()
        )!!
        assertEquals(0.125f, mt.tracks.first().clips.single().slowMo.rate, 0.01f)
    }
}
