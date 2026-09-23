package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFinishTest {
    @Test
    fun duckPreset_appliesFloorAndRamps() {
        val bed = AudioBed("bed.m4a")
        val hard = DuckPreset.Hard.apply(bed)
        assertEquals(0.10f, hard.duck, 0.001f)
        assertEquals(80L, hard.attackMs)
        assertEquals(150L, hard.releaseMs)
        assertEquals(DuckPreset.Hard, DuckPreset.matching(hard))
    }

    @Test
    fun encodeDecode_preservesRelease() {
        val tl = Timeline(
            clips = listOf(TimelineClip("c", 0, 0, 2000)),
            bed = AudioBed("bed.m4a", duck = 0.3f, attackMs = 100, releaseMs = 400),
            voice = AudioBed("vo.m4a", volume = 1f, duck = 1f, fadeInMs = 80, fadeOutMs = 100)
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 5000 }
        assertEquals(400L, decoded.bed?.releaseMs)
        assertEquals(0.3f, decoded.bed?.duck ?: 0f, 0.001f)
        assertEquals(80L, decoded.voice?.fadeInMs)
    }

    @Test
    fun mtRoundTrip_release() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip("c", 0, 0, 1500)),
                bed = DuckPreset.Soft.apply(AudioBed("bed.m4a"))
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(DuckPreset.Soft, DuckPreset.matching(decoded.bed))
        assertEquals(380L, decoded.bed?.releaseMs)
    }

    @Test
    fun polishWindows_expandsSlightly() {
        val windows = listOf(ClipMix.Window(1000, 3000))
        val polished = AudioFinish.polishWindows(windows, attackMs = 200, releaseMs = 400, totalMs = 5000)
        assertEquals(1, polished.size)
        assertTrue(polished[0].startMs < 1000)
        assertTrue(polished[0].endMs > 3000)
        assertNotNull(polished[0])
    }
}
