package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineVfxTest {
    @Test
    fun encodeDecode_roundTrip() {
        val layer = TimelineVfx(
            id = "vx1",
            preset = VfxPreset.Ember,
            startMs = 100,
            endMs = 4000,
            anchorX = 0.2f,
            anchorY = -0.1f,
            intensity = 0.8f,
            followTrack = true
        )
        val decoded = TimelineVfx.decodeLine(layer.encodeLine().split(','))!!
        assertEquals(layer.id, decoded.id)
        assertEquals(layer.preset, decoded.preset)
        assertEquals(layer.startMs, decoded.startMs)
        assertEquals(layer.endMs, decoded.endMs)
        assertEquals(layer.followTrack, decoded.followTrack)
        assertEquals(0.8f, decoded.intensity, 0.001f)
    }

    @Test
    fun timeline_persistsVfx() {
        val tl = Timeline(
            clips = listOf(TimelineClip("c0", 0, 0, 2000)),
            vfx = listOf(TimelineVfx.sparkleDefault(2000))
        )
        val raw = tl.encode()
        assertTrue(raw.contains("VX,"))
        val back = Timeline.decode(raw, 1) { 2000 }
        assertEquals(1, back.vfx.size)
        assertEquals(VfxPreset.Sparkle, back.vfx[0].preset)
    }

    @Test
    fun multiTrack_persistsVfx() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip("c0", 0, 0, 1500)),
                vfx = listOf(
                    TimelineVfx("vx0", VfxPreset.Snow, endMs = 1500, followTrack = true)
                )
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(1, decoded.vfx.size)
        assertEquals(VfxPreset.Snow, decoded.vfx[0].preset)
        assertTrue(decoded.vfx[0].followTrack)
    }
}
