package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeSecondaryTest {
    @Test
    fun secondary_encodeDecode() {
        val s = GradeSecondary(
            enabled = true,
            exposure = 0.4f,
            saturation = 1.2f,
            hueCenter = 200f,
            hueWidth = 50f,
            halfW = 0.4f,
            halfH = 0.4f
        )
        val back = GradeSecondary.decodeLine(s.encodeLine().split(','))!!
        assertTrue(back.enabled)
        assertEquals(s.exposure, back.exposure, 0.001f)
        assertEquals(s.hueCenter, back.hueCenter, 0.001f)
    }

    @Test
    fun multiTrack_preservesSecondaryAndMatch() {
        val grade = TimelineGrade(
            exposure = 0.2f,
            lookKey = MovieLooks.WARM,
            secondary = GradeSecondary(enabled = true, exposure = 0.3f, hueCenter = 40f),
            matchRef = GradeMatchRef(0.4f, 0.5f, 0.6f, 0.48f)
        )
        val mt = MultiTrackTimeline.fromSingleVideo(
            listOf(TimelineClip("c0", 0, 0, 1000))
        ).copy(grade = grade)
        val back = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(MovieLooks.WARM, back.grade.lookKey)
        assertTrue(back.grade.secondary.enabled)
        assertEquals(0.3f, back.grade.secondary.exposure, 0.001f)
        assertTrue(back.grade.matchRef.isSet())
        assertEquals(0.48f, back.grade.matchRef.luma, 0.001f)
    }

    @Test
    fun shotMatch_nudgesTowardRef() {
        val dark = TimelineGrade(exposure = -0.8f)
        val ref = GradeMatchRef.fromMeters(MovieGrade.scopeMeters(TimelineGrade.Neutral))
        val suggested = ShotMatch.suggest(dark, ref)
        assertTrue(suggested.exposure > dark.exposure)
    }

    @Test
    fun hueWeight_peaksAtCenter() {
        assertEquals(1f, hueWeight(30f, 30f, 60f), 0.001f)
        assertEquals(0f, hueWeight(30f, 200f, 40f), 0.001f)
        assertTrue(hueWeight(40f, 30f, 60f) > 0.5f)
    }

    @Test
    fun secondary_changesCubeVsPrimaryOnly() {
        val base = TimelineGrade(lookKey = MovieLooks.CLEAN)
        val withSec = base.copy(
            secondary = GradeSecondary(enabled = true, exposure = 0.8f, hueCenter = 0f, hueWidth = 120f)
        )
        assertFalse(withSec.isNeutral())
        val a = MovieGrade.gradeToCube(base, 5)
        val b = MovieGrade.gradeToCube(withSec, 5)
        // At least one cell differs when secondary is hot on reds
        var differ = false
        for (r in 0 until 5) {
            for (g in 0 until 5) {
                for (bl in 0 until 5) {
                    if (a[r][g][bl] != b[r][g][bl]) {
                        differ = true
                        break
                    }
                }
            }
        }
        assertTrue(differ)
    }
}
