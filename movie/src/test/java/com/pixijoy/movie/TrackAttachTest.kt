package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackAttachTest {
    @Test
    fun anchorPath_interpolatesBetweenKeys() {
        val path = TrackAnchorPath(
            listOf(
                TrackAnchorKeyframe(0f, 0.2f, 0.3f),
                TrackAnchorKeyframe(1f, 0.8f, 0.7f)
            )
        )
        val mid = path.sample(0.5f)!!
        assertEquals(0.5f, mid.first, 0.001f)
        assertEquals(0.5f, mid.second, 0.001f)
        assertEquals(0.2f, path.sample(0f)!!.first, 0.001f)
        assertEquals(0.8f, path.sample(1f)!!.first, 0.001f)
    }

    @Test
    fun seedPath_isConstant() {
        val path = TrackAnchorPath.seed(0.4f, 0.6f)
        assertEquals(0.4f, path.sample(0f)!!.first, 0.001f)
        assertEquals(0.4f, path.sample(0.5f)!!.first, 0.001f)
        assertEquals(0.6f, path.sample(1f)!!.second, 0.001f)
    }

    @Test
    fun emptyPath_samplesNull() {
        assertNull(TrackAnchorPath.None.sample(0.5f))
    }

    @Test
    fun cgOffset_centersAtSeedAndPullsTowardTrack() {
        val center = TrackAttach.cgOffset(0f, 0f, 0.5f, 0.5f)
        assertEquals(0f, center.first, 0.001f)
        assertEquals(0f, center.second, 0.001f)
        val right = TrackAttach.cgOffset(0f, 0f, 1f, 0.5f)
        assertTrue(right.first > 0f)
        val left = TrackAttach.cgOffset(0f, 0f, 0f, 0.5f)
        assertTrue(left.first < 0f)
    }

    @Test
    fun resolver_usesSeedWhenTrackActiveWithoutMedia() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 2000,
            track = ClipTrack(0.3f, 0.7f, 0.08f, TrackEngine.Ml)
        )
        val timeline = Timeline(listOf(clip))
        val resolver = TrackAnchorResolver.forTimeline(emptyList(), timeline)
        val at = resolver.at(500)
        assertNotNull(at)
        assertEquals(0.3f, at!!.first, 0.001f)
        assertEquals(0.7f, at.second, 0.001f)
    }

    @Test
    fun resolver_fixedIgnoresTime() {
        val r = TrackAnchorResolver.fixed(0.25f, 0.75f)
        assertEquals(0.25f, r.at(0)!!.first, 0.001f)
        assertEquals(0.75f, r.at(9_000)!!.second, 0.001f)
    }

    @Test
    fun followTrack_movesCgOffsetAlongPath() {
        val path = TrackAnchorPath(
            listOf(
                TrackAnchorKeyframe(0f, 0.2f, 0.5f),
                TrackAnchorKeyframe(1f, 0.9f, 0.5f)
            )
        )
        val start = TrackAttach.cgOffset(0f, 0f, path.sample(0f)!!.first, path.sample(0f)!!.second)
        val end = TrackAttach.cgOffset(0f, 0f, path.sample(1f)!!.first, path.sample(1f)!!.second)
        assertTrue(end.first > start.first)
    }
}
