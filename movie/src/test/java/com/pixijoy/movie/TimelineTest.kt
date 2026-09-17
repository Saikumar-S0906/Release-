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
    }

    @Test
    fun roundTrip() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 1000, 1f, false),
                TimelineClip("b", 1, 200, 1800, 2f, true)
            )
        )
        val decoded = Timeline.decode(tl.encode(), 2) { 2000 }
        assertEquals(tl, decoded)
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
