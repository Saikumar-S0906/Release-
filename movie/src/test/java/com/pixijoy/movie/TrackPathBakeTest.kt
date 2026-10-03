package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackPathBakeTest {
    @Test
    fun pan_pullsPointTowardCenter() {
        // Point on the right → pan left (negative tx).
        assertTrue(TrackPathBake.panX(0.75f) < 0f)
        // Point below center → pan up (negative ty in NDC-ish).
        assertTrue(TrackPathBake.panY(0.8f) < 0f)
        assertEquals(0f, TrackPathBake.panX(0.5f), 0.001f)
        assertEquals(0f, TrackPathBake.panY(0.5f), 0.001f)
    }

    @Test
    fun anchors_seedWhenEngineOff() {
        val track = ClipTrack(0.35f, 0.65f, 0.1f, TrackEngine.Off)
        val path = TrackPathBake.anchors("/missing.mp4", track, 0L, 2_000L)
        assertFalse(path.isEmpty)
        assertEquals(0.35f, path.sample(0.25f)!!.first, 0.001f)
        assertEquals(0.65f, path.sample(0.75f)!!.second, 0.001f)
    }

    @Test
    fun anchors_seedWhenMlRequestedButUnavailable() {
        val prev = TrackFacade.mlAvailableOverride
        try {
            TrackFacade.mlAvailableOverride = false
            val track = ClipTrack(0.2f, 0.8f, 0.08f, TrackEngine.Ml)
            val path = TrackPathBake.anchors("/missing.mp4", track, 0L, 2_000L)
            assertEquals(0.2f, path.sample(0.5f)!!.first, 0.001f)
            assertEquals(0.8f, path.sample(0.5f)!!.second, 0.001f)
            assertTrue(TrackPathBake.motion("/missing.mp4", track, 0L, 2_000L).isIdentity)
        } finally {
            TrackFacade.mlAvailableOverride = prev
        }
    }

    @Test
    fun sourceSampleMs_usesTrimWindowNotOutputDuration() {
        // Trimmed 10s–12s: samples must land in that source window, not 0–2s.
        assertEquals(10_000L, TrackPathBake.sourceSampleMs(0, 5, 10_000L, 12_000L))
        assertEquals(12_000L, TrackPathBake.sourceSampleMs(4, 5, 10_000L, 12_000L))
        assertEquals(11_000L, TrackPathBake.sourceSampleMs(2, 5, 10_000L, 12_000L))
    }

    @Test
    fun sourceSampleMs_coversFullSpanAtEnds() {
        assertEquals(0L, TrackPathBake.sourceSampleMs(0, 10, 0L, 5_000L))
        assertEquals(5_000L, TrackPathBake.sourceSampleMs(9, 10, 0L, 5_000L))
    }

    @Test
    fun shortRamp_stepsKeepParentTrackWindow() {
        // One second, six steps: each source slice is ~167 ms, under the 200 ms floor.
        val clip = TimelineClip(
            id = "ramp",
            mediaIndex = 0,
            inMs = 0,
            outMs = 1_000,
            speed = 0.5f,
            speedEnd = 1.5f,
            track = ClipTrack(0.4f, 0.6f, 0.08f, TrackEngine.Ml)
        )
        val steps = MovieSpeed.expandRamp(clip, steps = 6)
        assertTrue(steps.size >= 2)
        steps.forEach { step ->
            assertTrue(
                "step span ${step.sourceDurationMs} should be the short-ramp case",
                step.sourceDurationMs < TrackPathBake.MIN_ML_SOURCE_MS
            )
            assertFalse(TrackPathBake.acceptsMlSourceSpan(step.sourceDurationMs))
            assertEquals(0L, step.trackBakeIn())
            assertEquals(1_000L, step.trackBakeOut())
            assertTrue(TrackPathBake.acceptsMlSourceSpan(step.trackBakeOut() - step.trackBakeIn()))
            val (t0, t1) = step.trackBakeWindow()
            assertEquals(step.inMs / 1_000f, t0, 0.002f)
            assertEquals(step.outMs / 1_000f, t1, 0.002f)
        }
        // Missing media still does not invent a path; the window is what stays tracked.
        val follow = TrackPathBake.followMotion("/missing.mp4", steps[0])
        assertTrue(follow.isIdentity)
    }
}