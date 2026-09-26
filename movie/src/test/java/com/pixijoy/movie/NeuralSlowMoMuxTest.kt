package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeuralSlowMoMuxTest {
    @Test
    fun interpSteps_matchSlowMoPresets() {
        assertEquals(0, NeuralSlowMoMux.interpSteps(1f))
        assertEquals(1, NeuralSlowMoMux.interpSteps(0.5f))
        assertEquals(3, NeuralSlowMoMux.interpSteps(0.25f))
        assertEquals(7, NeuralSlowMoMux.interpSteps(0.125f))
    }

    @Test
    fun keyFrameCount_respectsInferBudget() {
        val keys = NeuralSlowMoMux.keyFrameCount(8_000L, rate = 0.125f, budget = 32)
        val steps = NeuralSlowMoMux.interpSteps(0.125f)
        val pairs = keys - 1
        assertTrue(pairs * steps <= 32)
        assertTrue(keys >= 3)
    }

    @Test
    fun keyFrameCount_shortClip_stillHasMinimumKeys() {
        val keys = NeuralSlowMoMux.keyFrameCount(400L, rate = 0.5f, budget = 32)
        assertTrue(keys >= 3)
        assertTrue(keys <= NeuralSlowMoMux.INFER_BUDGET + 1)
    }
}
