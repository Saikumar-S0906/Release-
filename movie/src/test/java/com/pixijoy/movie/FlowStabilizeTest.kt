package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FlowStabilizeTest {
    @Test
    fun global_motion_is_median_and_ignores_outliers() {
        val n = 1000
        val flow = FloatArray(n * 2) { if (it % 2 == 0) 3f else -2f }
        for (i in 0 until 100) { // 10 % moving subject must not drag the camera estimate
            flow[2 * i] = 40f
            flow[2 * i + 1] = 40f
        }
        val (x, y) = FlowStabilize.globalMotion(flow)
        assertEquals(3f, x, 1e-6f)
        assertEquals(-2f, y, 1e-6f)
    }

    @Test
    fun gaussian_keeps_constant_and_damps_jitter() {
        val flat = FloatArray(20) { 0.3f }
        FlowStabilize.gaussian(flat, 3f).forEach { assertEquals(0.3f, it, 1e-5f) }
        val jitter = FloatArray(40) { if (it % 2 == 0) 0.05f else -0.05f }
        val smooth = FlowStabilize.gaussian(jitter, 3f)
        assertTrue(smooth.drop(5).dropLast(5).all { abs(it) < 0.01f })
    }

    @Test
    fun path_interpolates_between_samples() {
        val p = FlowStabilize.Path(longArrayOf(0L, 1_000_000L), floatArrayOf(0f, 0.1f), floatArrayOf(0f, -0.1f), 1.1f)
        val (x, y) = p.correctionAt(500_000L)
        assertEquals(0.05f, x, 1e-6f)
        assertEquals(-0.05f, y, 1e-6f)
        assertEquals(0.1f, p.correctionAt(5_000_000L).first, 1e-6f)
    }
}
