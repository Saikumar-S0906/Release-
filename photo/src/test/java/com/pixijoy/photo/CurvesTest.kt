package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurvesTest {
    @Test
    fun identityCurve_evalMatchesInput() {
        val ys = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        assertEquals(0f, evalCurve(0f, ys), 0.001f)
        assertEquals(0.5f, evalCurve(0.5f, ys), 0.001f)
        assertEquals(1f, evalCurve(1f, ys), 0.001f)
        assertTrue(LiveGrade.curvesIdentity(ys))
    }

    @Test
    fun liftMids_raisesMidGray() {
        val ys = floatArrayOf(0f, 0.25f, 0.7f, 0.75f, 1f)
        assertFalse(LiveGrade.curvesIdentity(ys))
        assertTrue(evalCurve(0.5f, ys) > 0.5f)
    }

    @Test
    fun encodeDecode_curvesRoundTrip() {
        val ys = floatArrayOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        val encoded = listOf(PhotoOp.Curves(ys)).encodeGraph()
        val decoded = encoded.decodeGraph().filterIsInstance<PhotoOp.Curves>().single()
        assertTrue(ys.contentEquals(decoded.y))
    }
}
