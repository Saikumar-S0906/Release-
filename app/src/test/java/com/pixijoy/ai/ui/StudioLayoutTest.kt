package com.pixijoy.ai.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioLayoutTest {
    @Test
    fun phonePortrait_isSinglePane() {
        assertFalse(isStudioTwoPane(411, 891))
        assertFalse(isStudioSessionSideTools(411, 891))
    }

    @Test
    fun phoneLandscape_hubStaysSinglePane_sessionGetsSideTools() {
        assertFalse(isStudioTwoPane(891, 411))
        assertFalse(isStudioTwoPane(840, 400))
        assertFalse(isStudioTwoPane(914, 411))
        assertTrue(isStudioSessionSideTools(891, 411))
        assertTrue(isStudioSessionSideTools(840, 400))
        assertTrue(isStudioSessionSideTools(914, 411))
        assertTrue(isStudioSessionSideTools(700, 360))
        // Narrow landscape keeps bottom glass (compact / multi-window).
        assertFalse(isStudioSessionSideTools(600, 360))
    }

    @Test
    fun tabletOrFoldInner_isTwoPane() {
        assertTrue(isStudioTwoPane(840, 520))
        assertTrue(isStudioTwoPane(1024, 800))
        // Pixel Tablet AVD: 2560×1600 @ 320dpi
        assertTrue(isStudioTwoPane(800, 500))
        assertTrue(isStudioSessionSideTools(800, 500))
    }

    @Test
    fun foldOuter_isSinglePane() {
        assertFalse(isStudioTwoPane(600, 960))
        assertFalse(isStudioSessionSideTools(600, 960))
    }

    @Test
    fun glassMax_shortLandscape_leavesCanvas() {
        assertEquals(172, studioGlassMaxDp(411))
        assertEquals(220, studioGlassMaxDp(559))
        assertEquals(280, studioGlassMaxDp(560))
        assertEquals(280, studioGlassMaxDp(891))
    }

    @Test
    fun glassMax_largeFont_growsButCaps() {
        assertEquals(172, studioGlassMaxDp(411, 1f))
        assertTrue(studioGlassMaxDp(411, 1.3f) > 172)
        assertTrue(studioGlassMaxDp(411, 1.3f) <= 260)
        assertTrue(studioGlassMaxDp(411, 1.3f) <= (411 * 55 / 100))
        assertEquals(280, studioGlassMaxDp(891, 1f))
        assertTrue(studioGlassMaxDp(891, 1.5f) > 280)
        assertTrue(studioGlassMaxDp(891, 1.5f) <= 360)
    }

    @Test
    fun sideDock_phoneLandscape_narrowerThanTablet() {
        assertEquals(360, studioSideDockWidthDp(800, 500))
        assertEquals(360, studioSideDockWidthDp(1024, 800))
        // ~36% of 914 → 329, coerced to 320
        assertEquals(320, studioSideDockWidthDp(914, 411))
        assertEquals(280, studioSideDockWidthDp(700, 360))
    }

    @Test
    fun multiWindow_halfPhone_staysSinglePane() {
        // Typical portrait split: ~half width, full-ish height.
        assertFalse(isStudioTwoPane(411, 800))
        assertFalse(isStudioSessionSideTools(411, 800))
        assertFalse(isStudioTwoPane(540, 720))
        // Half of Pixel Tablet landscape still under two-pane width gate.
        assertFalse(isStudioTwoPane(400, 500))
        assertFalse(isStudioSessionSideTools(400, 500))
    }
}
