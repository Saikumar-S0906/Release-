package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieTimelineDisplayTest {
    @Test
    fun timecode_formatsTenthsMinutesAndHours() {
        assertEquals("0:00.0", MovieTimelineDisplay.formatTimecode(-1))
        assertEquals("0:09.8", MovieTimelineDisplay.formatTimecode(9_899))
        assertEquals("1:02.3", MovieTimelineDisplay.formatTimecode(62_399))
        assertEquals("1:01:01.4", MovieTimelineDisplay.formatTimecode(3_661_499))
    }

    @Test
    fun clipLabel_usesOneBasedNumberAndDuration() {
        assertEquals("Clip 1 · 0:02.5", MovieTimelineDisplay.clipLabel(0, 2_500))
        assertEquals("Clip 4 · 1:01.0", MovieTimelineDisplay.clipLabel(3, 61_000))
    }

    @Test
    fun displayWidth_isDurationProportionalWithinReadableBounds() {
        assertEquals(104f, MovieTimelineDisplay.clipWidthDp(250), 0.001f)
        assertEquals(132f, MovieTimelineDisplay.clipWidthDp(3_000), 0.001f)
        assertEquals(300f, MovieTimelineDisplay.clipWidthDp(60_000), 0.001f)
        assertTrue(
            MovieTimelineDisplay.thumbnailCount(300f) >
                MovieTimelineDisplay.thumbnailCount(104f)
        )
    }
}
