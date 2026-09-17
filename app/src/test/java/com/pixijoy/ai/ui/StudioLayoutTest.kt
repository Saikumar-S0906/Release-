package com.pixijoy.ai.ui

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
    }

    @Test
    fun tabletOrFoldInner_isTwoPane() {
        assertTrue(isStudioTwoPane(840, 520))
        assertTrue(isStudioTwoPane(1024, 800))
    }

    @Test
    fun foldOuter_isSinglePane() {
        assertFalse(isStudioTwoPane(600, 960))
    }
}
