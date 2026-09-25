package com.pixijoy.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test

class PixiEmptyWashTest {
    @Test
    fun restingSheenSitsOnThePlateUsingTheSkeletonBand() {
        val width = 88f
        val band = width * PixiSkeletonSheenFraction
        val start = pixiSkeletonSheenStart(PixiEmptySheenRest, width)
        assertTrue(start < width)
        assertTrue(start + band > 0f)
    }
}
