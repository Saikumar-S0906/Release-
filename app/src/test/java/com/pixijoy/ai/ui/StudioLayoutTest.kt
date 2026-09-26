package com.pixijoy.ai.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioLayoutTest {
    @Test
    fun phonePortrait_isSinglePane() {
        assertFalse(isStudioTwoPane(411, 891))
    }

    @Test
    fun phoneLandscape_isSinglePaneEvenIfWide() {
        assertFalse(isStudioTwoPane(891, 411))
        assertFalse(isStudioTwoPane(840, 400))
        assertFalse(isStudioTwoPane(914, 411))
    }

    @Test
    fun tabletOrFoldInner_isTwoPane() {
        assertTrue(isStudioTwoPane(840, 520))
        assertTrue(isStudioTwoPane(1024, 800))
        // Pixel Tablet AVD: 2560×1600 @ 320dpi
        assertTrue(isStudioTwoPane(800, 500))
    }

    @Test
    fun foldOuter_isSinglePane() {
        assertFalse(isStudioTwoPane(600, 960))
    }

    @Test
    fun glassMax_shortLandscape_leavesCanvas() {
        assertEquals(172, studioGlassMaxDp(411))
        assertEquals(220, studioGlassMaxDp(559))
        assertEquals(280, studioGlassMaxDp(560))
        assertEquals(280, studioGlassMaxDp(891))
    }
}
