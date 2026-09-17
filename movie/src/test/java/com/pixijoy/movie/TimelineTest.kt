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
}
