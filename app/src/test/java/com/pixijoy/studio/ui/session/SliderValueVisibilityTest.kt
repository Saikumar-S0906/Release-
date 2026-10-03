package com.pixijoy.studio.ui.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SliderValueVisibilityTest {
    @Test
    fun touchTargetStays48dp() {
        assertEquals(48, SLIDER_VALUE_MIN_TOUCH_DP)
        assertTrue(SLIDER_VALUE_REVEAL_CLEARANCE_DP >= 40)
    }

    @Test
    fun zeroPointFourAndEndsStayInRange() {
        assertEquals(0f, sliderValueFraction(0f, 0f..1f), 0.0001f)
        assertEquals(0.4f, sliderValueFraction(0.4f, 0f..1f), 0.0001f)
        assertEquals(1f, sliderValueFraction(1f, 0f..1f), 0.0001f)
        assertEquals(0f, sliderValueFraction(-1.5f, -1.5f..1.5f), 0.0001f)
        assertEquals(1f, sliderValueFraction(1.5f, -1.5f..1.5f), 0.0001f)
        val exposure = sliderValueFraction(0.4f, -1.5f..1.5f)
        assertTrue(exposure in 0f..1f)
    }

    @Test
    fun readoutStaysCenteredAtEndsAndAtPointFour() {
        listOf(0f, 0.4f, 1f).forEach { fraction ->
            assertEquals(0f, sliderReadoutShiftPx(fraction), 0.01f)
        }
    }

    @Test
    fun revealHeightKeepsTheReadoutAndClearance() {
        assertEquals(96f, sliderValueRevealHeightPx(48f, 48f), 0.01f)
        assertTrue(sliderValueRevealHeightPx(48f, 48f) >= SLIDER_VALUE_MIN_TOUCH_DP.toFloat())
    }
}
