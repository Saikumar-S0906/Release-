package com.pixijoy.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixiSkeletonMotionTest {
    @Test
    fun sheenEntersFromTheLeadingEdgeAndLeavesPastTheTrailingEdge() {
        val width = 200f
        val band = width * PixiSkeletonSheenFraction
        val start = pixiSkeletonSheenStart(0f, width)
        assertEquals(-band, start, 0.01f)
        assertTrue(start + band <= 0.01f)

        val end = pixiSkeletonSheenStart(1f, width)
        assertTrue(end >= width)

        val mid = pixiSkeletonSheenStart(0.5f, width)
        assertTrue(mid < width)
        assertTrue(mid + band > 0f)
    }

    @Test
    fun sheenClampsProgressAndIgnoresEmptyWidth() {
        val width = 100f
        assertEquals(pixiSkeletonSheenStart(0f, width), pixiSkeletonSheenStart(-2f, width), 0.01f)
        assertEquals(pixiSkeletonSheenStart(1f, width), pixiSkeletonSheenStart(3f, width), 0.01f)
        assertEquals(0f, pixiSkeletonSheenStart(0.5f, 0f), 0.01f)
        assertEquals(0f, pixiSkeletonSheenStart(0.5f, -10f), 0.01f)
    }
}
