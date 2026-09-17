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
}
