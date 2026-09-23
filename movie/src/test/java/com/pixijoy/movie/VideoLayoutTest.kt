package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLayoutTest {
    @Test
    fun inputCounts() {
        assertEquals(1, VideoLayout.Single.videoInputs)
        assertEquals(2, VideoLayout.Pip.videoInputs)
        assertEquals(2, VideoLayout.Split.videoInputs)
        assertEquals(4, VideoLayout.Grid.videoInputs)
        assertFalse(VideoLayout.Single.isMulti())
        assertTrue(VideoLayout.Pip.isMulti())
    }

    @Test
    fun inputPaths_padsAndWraps() {
        assertEquals(listOf("a"), VideoLayoutPlan.inputPaths(listOf("a"), VideoLayout.Single))
        assertEquals(listOf("a", "a"), VideoLayoutPlan.inputPaths(listOf("a"), VideoLayout.Pip))
        assertEquals(listOf("a", "b"), VideoLayoutPlan.inputPaths(listOf("a", "b", "c"), VideoLayout.Split))
        assertEquals(
            listOf("a", "b", "a", "b"),
            VideoLayoutPlan.inputPaths(listOf("a", "b"), VideoLayout.Grid)
        )
    }

    @Test
    fun timelineRoundTrip_layout() {
        val tl = Timeline(
            clips = listOf(TimelineClip(id = "c0", mediaIndex = 0, inMs = 0, outMs = 2000)),
            layout = VideoLayout.Pip
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 5000 }
        assertEquals(VideoLayout.Pip, decoded.layout)
        assertTrue(decoded.encode().contains("L,Pip"))
    }

    @Test
    fun mtRoundTrip_layoutAndPlan() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip(id = "c0", mediaIndex = 0, inMs = 0, outMs = 3000)),
                layout = VideoLayout.Grid
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(VideoLayout.Grid, decoded.layout)
        assertEquals(VideoLayout.Grid, decoded.toFlatTimeline().layout)
        assertEquals(4, decoded.compositionSequencePlan(false, false, 0))
        assertEquals(
            VideoLayout.Split,
            MultiTrackTimeline.fromFlatTimeline(
                Timeline(layout = VideoLayout.Split)
            ).withPrimaryFlat(Timeline(layout = VideoLayout.Split)).layout
        )
    }

    @Test
    fun parse_defaultsUnknownToSingle() {
        assertEquals(VideoLayout.Single, VideoLayout.parse(null))
        assertEquals(VideoLayout.Single, VideoLayout.parse("nope"))
        assertEquals(VideoLayout.Split, VideoLayout.parse("split"))
    }
}
