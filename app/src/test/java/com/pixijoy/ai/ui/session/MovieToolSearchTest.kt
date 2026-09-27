package com.pixijoy.ai.ui.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieToolSearchTest {
    @Test
    fun findsGradeInColor() {
        val hits = MovieToolSearch.find("grade")
        assertTrue(hits.any { it.groupTag == MovieGroup.Color.tag })
    }

    @Test
    fun findsCg() {
        val hits = MovieToolSearch.find("cg")
        assertEquals(MovieGroup.Cg.tag, hits.first().groupTag)
    }

    @Test
    fun findsTrackEffectSub() {
        val hits = MovieToolSearch.find("track")
        assertTrue(hits.any { it.effectSubTag == MovieEffectSub.Track.tag })
    }

    @Test
    fun shortQueryIgnored() {
        assertTrue(MovieToolSearch.find("c").isEmpty())
    }
}
