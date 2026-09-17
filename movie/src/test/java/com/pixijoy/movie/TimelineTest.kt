package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    @Test
    fun splitCreatesTwoClips() {
        val tl = Timeline(
            listOf(TimelineClip("a", 0, 0, 4000, 1f, false))
        )
        val split = tl.split(2000)
        assertEquals(2, split.clips.size)
        assertEquals(2000L, split.clips[0].outMs)
        assertEquals(2000L, split.clips[1].inMs)
        assertEquals(4000L, split.totalMs())
        val faded = Timeline(listOf(TimelineClip("a", 0, 0, 4000, fadeInMs = 200, fadeOutMs = 200)))
            .split(2000)
        assertEquals(200L, faded.clips[0].fadeInMs)
        assertEquals(0L, faded.clips[0].fadeOutMs)
        assertEquals(0L, faded.clips[1].fadeInMs)
        assertEquals(200L, faded.clips[1].fadeOutMs)
    }

    @Test
    fun roundTrip() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 1000, 1f, false, 0.8f, 120, 80),
                TimelineClip("b", 1, 200, 1800, 2f, true)
            )
        )
        val decoded = Timeline.decode(tl.encode(), 2) { 2000 }
        assertEquals(tl, decoded)
    }

    @Test
    fun audioBedRoundTripAndSurvivesSplit() {
        val tl = Timeline(
            listOf(TimelineClip("a", 0, 0, 4000)),
            AudioBed("bed.m4a", 0.6f, 200, 300, false)
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 4000 }
        assertEquals(tl.bed, decoded.bed)
        val split = decoded.split(2000)
        assertEquals("bed.m4a", split.bed?.fileName)
        assertEquals(0.6f, split.bed?.volume ?: 0f, 0.001f)
        assertEquals(2, split.clips.size)
        assertEquals(0.25f, decoded.bed?.duck ?: 0f, 0.001f)
    }

    @Test
    fun dialogueWindowsSkipMutedClips() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 1000, muted = false, volume = 1f),
                TimelineClip("b", 1, 0, 1000, muted = true, volume = 1f),
                TimelineClip("c", 2, 0, 1000, muted = false, volume = 0f)
            )
        )
        val windows = tl.dialogueWindows()
        assertEquals(1, windows.size)
        assertEquals(0L, windows[0].startMs)
        assertEquals(1000L, windows[0].endMs)
    }

    @Test
    fun oldSevenFieldClipKeepsFullVolume() {
        val decoded = Timeline.decode("C,0,0,1000,1.0,0,a", 1) { 1000 }
        assertEquals(1f, decoded.clips[0].volume, 0.001f)
        assertEquals(0L, decoded.clips[0].fadeInMs)
        assertEquals(0L, decoded.clips[0].fadeOutMs)
    }

    @Test
    fun windowSeekMapsCompositionTimeToClipWindow() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 1000),
                TimelineClip("b", 1, 0, 1000)
            )
        )
        assertEquals(0 to 400L, tl.windowSeek(400))
        assertEquals(1 to 100L, tl.windowSeek(1100))
        assertEquals(0L, tl.startOf("a"))
        assertEquals(1000L, tl.startOf("b"))
    }

    @Test
    fun splitNearEdgeDoesNothing() {
        val tl = Timeline(listOf(TimelineClip("a", 0, 0, 400, 1f, false)))
        assertEquals(1, tl.split(10).clips.size)
    }

    @Test
    fun rippleRemoveAndMove() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 1000),
                TimelineClip("b", 1, 0, 1000),
                TimelineClip("c", 2, 0, 1000)
            )
        )
        assertEquals(listOf("a", "c"), tl.remove("b").clips.map { it.id })
        assertEquals(listOf("b", "a", "c"), tl.move("a", 1).clips.map { it.id })
    }

    @Test
    fun trimHandlesKeepMinimum() {
        val tl = Timeline(listOf(TimelineClip("a", 0, 0, 4000)))
        assertEquals(500L, tl.trimIn("a", 500).clips[0].inMs)
        assertEquals(3000L, tl.trimOut("a", 3000, 4000).clips[0].outMs)
        assertTrue(tl.trimIn("a", 3900).clips[0].outMs - tl.trimIn("a", 3900).clips[0].inMs >= 250)
    }

    @Test
    fun rippleTrimRollsAlignedCutAndKeepsLaterClips() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 2000),
                TimelineClip("b", 0, 2000, 4000),
                TimelineClip("c", 1, 0, 1000)
            )
        )
        val out = tl.rippleTrimOut("a", 1500, 4000)
        assertEquals(1500L, out.clips[0].outMs)
        assertEquals(1500L, out.clips[1].inMs)
        assertEquals(4000L, out.clips[1].outMs)
        assertEquals("c", out.clips[2].id)
        assertEquals(0L, out.clips[2].inMs)
        assertEquals(1000L, out.clips[2].outMs)

        val inn = tl.rippleTrimIn("b", 2500)
        assertEquals(2500L, inn.clips[0].outMs)
        assertEquals(2500L, inn.clips[1].inMs)
    }

    @Test
    fun rippleTrimDifferentMediaDoesNotRollNeighborSource() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 2000),
                TimelineClip("b", 1, 0, 2000)
            )
        )
        val trimmed = tl.rippleTrimOut("a", 800, 4000)
        assertEquals(800L, trimmed.clips[0].outMs)
        assertEquals(0L, trimmed.clips[1].inMs)
        assertEquals(2, trimmed.clips.size)
        assertEquals(800L + 2000L, trimmed.totalMs())
    }

    @Test
    fun rippleTrimKeepsMinimumHoldOnBothSidesOfCut() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 2000),
                TimelineClip("b", 0, 2000, 2300)
            )
        )
        val rolled = tl.rippleTrimOut("a", 2200, 4000)
        assertTrue(rolled.clips[0].outMs - rolled.clips[0].inMs >= 250)
        assertTrue(rolled.clips[1].outMs - rolled.clips[1].inMs >= 250)
        assertEquals(rolled.clips[0].outMs, rolled.clips[1].inMs)
    }
}
