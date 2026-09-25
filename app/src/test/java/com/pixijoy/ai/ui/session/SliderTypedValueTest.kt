package com.pixijoy.ai.ui.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SliderTypedValueTest {
    private val exposure = -1.5f..1.5f
    private val blur = 0f..8f

    @Test
    fun parse_keepsAnInRangeDecimal() {
        assertEquals(0.35f, parseSliderTypedValue("0.35", exposure)!!, 0.0001f)
        assertEquals(-0.5f, parseSliderTypedValue("  -0.5 ", exposure)!!, 0.0001f)
    }

    @Test
    fun parse_clampsToTheSliderEnds() {
        assertEquals(1.5f, parseSliderTypedValue("9", exposure)!!, 0f)
        assertEquals(-1.5f, parseSliderTypedValue("-9", exposure)!!, 0f)
        assertEquals(1.5f, parseSliderTypedValue("1.5", exposure)!!, 0f)
        assertEquals(0f, parseSliderTypedValue("-2", blur)!!, 0f)
        assertEquals(8f, parseSliderTypedValue("8.2", blur)!!, 0f)
    }

    @Test
    fun parse_acceptsACommaDecimal() {
        assertEquals(1.25f, parseSliderTypedValue("1,25", exposure)!!, 0.0001f)
        assertEquals(-0.5f, parseSliderTypedValue("-0,5", exposure)!!, 0.0001f)
    }

    @Test
    fun parse_treatsTheLaterMarkAsDecimalWhenBothAppear() {
        val wide = 0f..2000f
        assertEquals(1234.5f, parseSliderTypedValue("1.234,5", wide)!!, 0.01f)
        assertEquals(1234.5f, parseSliderTypedValue("1,234.5", wide)!!, 0.01f)
    }

    @Test
    fun parse_dropsATrailingSeparatorAndALeadingDot() {
        assertEquals(1f, parseSliderTypedValue("1.", exposure)!!, 0f)
        assertEquals(-0.5f, parseSliderTypedValue("-.5", exposure)!!, 0.0001f)
    }

    @Test
    fun parse_rejectsBlankAndNonFiniteText() {
        assertNull(parseSliderTypedValue("", exposure))
        assertNull(parseSliderTypedValue("   ", exposure))
        assertNull(parseSliderTypedValue("-", exposure))
        assertNull(parseSliderTypedValue(".", exposure))
        assertNull(parseSliderTypedValue("abc", exposure))
        assertNull(parseSliderTypedValue("NaN", exposure))
        assertNull(parseSliderTypedValue("Infinity", exposure))
    }

    @Test
    fun partial_allowsOneSeparatorAndAnOptionalSign() {
        assertTrue(isPartialSliderInput("", allowNegative = true))
        assertTrue(isPartialSliderInput("-", allowNegative = true))
        assertTrue(isPartialSliderInput("-1,5", allowNegative = true))
        assertTrue(isPartialSliderInput("0.25", allowNegative = false))
        assertFalse(isPartialSliderInput("-1", allowNegative = false))
        assertFalse(isPartialSliderInput("1.2.3", allowNegative = true))
        assertFalse(isPartialSliderInput("1e2", allowNegative = true))
        assertFalse(isPartialSliderInput("--1", allowNegative = true))
    }

    @Test
    fun toggleSign_flipsALeadingMinus() {
        assertEquals("-0.35", toggleSliderSign("0.35"))
        assertEquals("0.35", toggleSliderSign("-0.35"))
        assertEquals("-", toggleSliderSign(""))
    }
}
