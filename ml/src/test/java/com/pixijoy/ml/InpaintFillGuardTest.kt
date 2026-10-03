package com.pixijoy.ml

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InpaintFillGuardTest {
    @Test
    fun flatGrayHoleOnTexturedPhoto_isDead() {
        val hole = FloatArray(48) { 0.502f }
        val surround = FloatArray(48) { i -> if ((i / 3) % 2 == 0) 0.15f else 0.85f }
        assertTrue(inpaintHoleIsDeadFill(hole, surround))
    }

    @Test
    fun texturedHole_isKept() {
        val hole = FloatArray(48) { i -> if ((i / 3) % 2 == 0) 0.2f else 0.8f }
        val surround = FloatArray(48) { i -> if ((i / 3) % 2 == 0) 0.15f else 0.85f }
        assertFalse(inpaintHoleIsDeadFill(hole, surround))
    }

    @Test
    fun flatHoleOnFlatSurround_isKept() {
        val hole = FloatArray(48) { 0.62f }
        val surround = FloatArray(48) { 0.60f }
        assertFalse(inpaintHoleIsDeadFill(hole, surround))
    }
}
