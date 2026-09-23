package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineD2Test {
    @Test
    fun rollCut_movesSharedEditPoint() {
        val tl = Timeline(
            listOf(
                TimelineClip("a", 0, 0, 2000),
                TimelineClip("b", 0, 2000, 4000)
            )
        )
        val rolled = tl.rollCut("a", 500)
        assertEquals(2500L, rolled.clips[0].outMs)
        assertEquals(2500L, rolled.clips[1].inMs)
        assertEquals(tl, tl.rollCut("a", 0))
        assertEquals(tl, tl.rollCut("missing", 100))
    }

    @Test
    fun softTransition_appliesPairedFades() {
        val tl = Timeline(
            listOf(
                TimelineClip(
                    "a", 0, 0, 3000,
                    transition = ClipTransition.Soft,
                    transitionMs = 400
                ),
                TimelineClip("b", 0, 3000, 6000)
            )
        ).withTransitionFadesApplied()
        assertEquals(400L, tl.clips[0].fadeOutMs)
        assertEquals(400L, tl.clips[1].fadeInMs)
    }

    @Test
    fun encodeDecode_preservesTransitionAndRamp() {
        val tl = Timeline(
            listOf(
                TimelineClip(
                    id = "a",
                    mediaIndex = 0,
                    inMs = 0,
                    outMs = 2000,
                    speed = 1f,
                    speedEnd = 1.5f,
                    transition = ClipTransition.Soft,
                    transitionMs = 350
                )
            )
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 5000 }
        assertEquals(ClipTransition.Soft, decoded.clips[0].transition)
        assertEquals(350L, decoded.clips[0].transitionMs)
        assertEquals(1.5f, decoded.clips[0].speedEnd, 0.01f)
        assertTrue(decoded.clips[0].hasSpeedRamp)
    }

    @Test
    fun speedRamp_expandsIntoSteps() {
        val clip = TimelineClip("a", 0, 0, 3000, speed = 0.75f, speedEnd = 1.5f)
        val steps = MovieSpeed.expandRamp(clip, steps = 4)
        assertEquals(4, steps.size)
        assertFalse(steps.any { it.hasSpeedRamp })
        assertEquals(0L, steps[0].inMs)
        assertEquals(3000L, steps.last().outMs)
        assertTrue(steps.first().speed < steps.last().speed)
    }

    @Test
    fun mtRoundTrip_transitionFields() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                listOf(
                    TimelineClip(
                        "c1", 0, 0, 1500,
                        transition = ClipTransition.Soft,
                        transitionMs = 200,
                        speed = 1f,
                        speedEnd = 1.25f
                    )
                )
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        val clip = decoded.tracks.first().clips.single()
        assertEquals(ClipTransition.Soft, clip.transition)
        assertEquals(200L, clip.transitionMs)
        assertEquals(1.25f, clip.speedEnd, 0.01f)
    }
}
