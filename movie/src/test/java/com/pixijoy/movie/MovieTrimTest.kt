package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieTrimTest {
    @Test
    fun normalize_keepsMinimumWindow() {
        val (start, end) = MovieTrim.normalize(0, 10, 5000)
        assertTrue(end - start >= 250)
        assertEquals(0L, start)
    }

    @Test
    fun normalize_clampsToDuration() {
        val (start, end) = MovieTrim.normalize(-100, 99999, 2000)
        assertEquals(0L, start)
        assertEquals(2000L, end)
    }

    @Test
    fun stillDurations() {
        assertEquals(3000L, MovieTrim.stillDurationMs(true))
        assertEquals(4000L, MovieTrim.stillDurationMs(false))
    }

    @Test
    fun speed_clampsAndShortens() {
        assertEquals(0.5f, MovieSpeed.normalize(0.1f), 0.001f)
        assertEquals(2f, MovieSpeed.normalize(8f), 0.001f)
        assertEquals(2000L, MovieSpeed.outputDurationMs(4000, 2f))
        assertEquals(8000L, MovieSpeed.outputDurationMs(4000, 0.5f))
    }

    @Test
    fun frameStepSnapsToFps() {
        assertEquals(24f, MovieFrame.fps(null), 0.001f)
        assertEquals(30f, MovieFrame.fps(30f), 0.001f)
        assertEquals(24f, MovieFrame.fps(0f), 0.001f)
        assertEquals(41L, MovieFrame.durationMs(24f))
        assertEquals(1025L, MovieFrame.snap(1020, 24f, 5000))
        assertEquals(41L, MovieFrame.step(0, 1, 24f, 5000))
        assertEquals(0L, MovieFrame.step(20, -1, 24f, 5000))
        assertEquals(5000L, MovieFrame.step(4980, 2, 24f, 5000))
    }

    @Test
    fun kenBurnsScale_bounds() {
        assertEquals(1f, MovieMotion.scale(0f, false), 0.001f)
        assertEquals(1.18f, MovieMotion.scale(1f, false), 0.001f)
        val mid = MovieMotion.scale(0.5f, true)
        assertTrue(mid > 1f)
        assertEquals(0f, MovieMotion.pan(0.5f, true, 1000f), 0.001f)
    }
}
