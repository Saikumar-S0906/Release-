package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineGradeTest {
    @Test
    fun encodeDecode_roundTrip() {
        val grade = TimelineGrade(exposure = 0.5f, contrast = 1.2f, saturation = 0.8f, lookKey = MovieLooks.WARM)
        val tl = Timeline(
            clips = listOf(TimelineClip("c", 0, 0, 2000)),
            grade = grade
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 5000 }
        assertFalse(decoded.grade.isNeutral())
        assertEquals(MovieLooks.WARM, decoded.grade.lookKey)
        assertEquals(0.5f, decoded.grade.exposure, 0.01f)
        assertEquals(1.2f, decoded.grade.contrast, 0.01f)
    }

    @Test
    fun mtRoundTrip_grade() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip("c", 0, 0, 1500)),
                grade = TimelineGrade(lookKey = MovieLooks.FILM, exposure = -0.25f)
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(MovieLooks.FILM, decoded.grade.lookKey)
        assertEquals(-0.25f, decoded.grade.exposure, 0.01f)
    }

    @Test
    fun scopeMeters_respondToWarmLook() {
        val neutral = MovieGrade.scopeMeters(TimelineGrade.Neutral)
        val warm = MovieGrade.scopeMeters(TimelineGrade(lookKey = MovieLooks.WARM))
        assertTrue(warm.red >= neutral.red - 0.05f)
        assertTrue(warm.luma in 0f..1.2f)
    }

    @Test
    fun colorMatrix_identityWhenNeutral() {
        assertTrue(TimelineGrade.Neutral.isNeutral())
        val m = MovieGrade.colorMatrix(TimelineGrade.Neutral)
        assertEquals(1f, m[0], 0.01f)
        assertEquals(1f, m[6], 0.01f)
        assertEquals(1f, m[12], 0.01f)
    }

    @Test
    fun withPrimaryFlat_preservesGrade() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip("c", 0, 0, 2000)),
                grade = TimelineGrade(lookKey = MovieLooks.COOL)
            )
        )
        val next = mt.withPrimaryFlat(
            mt.toFlatTimeline().copy(clips = listOf(TimelineClip("c", 0, 0, 2500)))
        )
        assertEquals(MovieLooks.COOL, next.grade.lookKey)
    }
}
