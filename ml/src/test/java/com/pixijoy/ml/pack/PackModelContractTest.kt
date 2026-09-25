package com.pixijoy.ml.pack

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackModelContractTest {
    @Test
    fun superRes_accepts4xRgb() {
        assertEquals(4, PackModelContract.superResScale(intArrayOf(1, 50, 50, 3), intArrayOf(1, 200, 200, 3)))
        assertEquals(2, PackModelContract.superResScale(intArrayOf(1, 64, 64, 3), intArrayOf(1, 128, 128, 3)))
    }

    @Test
    fun superRes_rejectsClassifierOrSameSize() {
        assertNull(PackModelContract.superResScale(intArrayOf(1, 224, 224, 3), intArrayOf(1, 1000)))
        assertNull(PackModelContract.superResScale(intArrayOf(1, 64, 64, 3), intArrayOf(1, 64, 64, 3)))
        assertNull(PackModelContract.superResScale(intArrayOf(1, 50, 50, 3), intArrayOf(1, 150, 200, 3)))
    }

    @Test
    fun inpaint_requiresRgbPlusMask() {
        assertTrue(PackModelContract.isInpaintRgbMask(intArrayOf(1, 256, 256, 4), intArrayOf(1, 256, 256, 3)))
        assertFalse(PackModelContract.isInpaintRgbMask(intArrayOf(1, 256, 256, 3), intArrayOf(1, 256, 256, 3)))
    }

    @Test
    fun inpaint_acceptsRgbPlusSeparateMask() {
        assertTrue(
            PackModelContract.isInpaintRgbAndMask(
                intArrayOf(1, 256, 256, 3),
                intArrayOf(1, 256, 256, 1),
                intArrayOf(1, 256, 256, 3)
            )
        )
        assertFalse(
            PackModelContract.isInpaintRgbAndMask(
                intArrayOf(1, 256, 256, 3),
                intArrayOf(1, 128, 128, 1),
                intArrayOf(1, 256, 256, 3)
            )
        )
    }

    @Test
    fun denoise_acceptsSameSizeRgb_rejectsSuperRes() {
        assertTrue(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 256, 256, 3), intArrayOf(1, 256, 256, 3)))
        assertTrue(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 3, 256, 256), intArrayOf(1, 3, 256, 256)))
        assertFalse(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 50, 50, 3), intArrayOf(1, 200, 200, 3)))
    }
}

class PackDownloaderTest {
    @Test
    fun download_rejectsCleartext() {
        val tmp = File.createTempFile("pack", ".bin")
        try {
            val result = PackDownloader.download("http://example.com/x.tflite", tmp) {}
            assertTrue(result.isFailure)
        } finally {
            tmp.delete()
        }
    }
}
