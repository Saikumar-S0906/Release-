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
        // Pinned compressed_esrgan.tflite: 180×320 NHWC → 720×1280 (4×).
        assertEquals(4, PackModelContract.superResScale(intArrayOf(1, 180, 320, 3), intArrayOf(1, 720, 1280, 3)))
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
        assertEquals(
            0 to 1,
            PackModelContract.inpaintInputIndices(
                intArrayOf(1, 256, 256, 3),
                intArrayOf(1, 256, 256, 1)
            )
        )
        // On-disk graph: mask is input 0, image is input 1.
        assertEquals(
            1 to 0,
            PackModelContract.inpaintInputIndices(
                intArrayOf(1, 256, 256, 1),
                intArrayOf(1, 256, 256, 3)
            )
        )
        assertNull(
            PackModelContract.inpaintInputIndices(
                intArrayOf(1, 256, 256, 3),
                intArrayOf(1, 128, 128, 1)
            )
        )
    }

    @Test
    fun depth_accepts256RelativeMap_rejectsRgbOut() {
        assertTrue(
            PackModelContract.isMonocularDepth(
                intArrayOf(1, 256, 256, 3),
                intArrayOf(1, 256, 256, 1)
            )
        )
        assertFalse(
            PackModelContract.isMonocularDepth(
                intArrayOf(1, 128, 128, 3),
                intArrayOf(1, 128, 128, 1)
            )
        )
        assertFalse(
            PackModelContract.isMonocularDepth(
                intArrayOf(1, 256, 256, 3),
                intArrayOf(1, 256, 256, 3)
            )
        )
    }

    @Test
    fun denoise_acceptsSameSizeRgb_rejectsSuperRes() {
        assertTrue(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 256, 256, 3), intArrayOf(1, 256, 256, 3)))
        assertTrue(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 3, 256, 256), intArrayOf(1, 3, 256, 256)))
        assertFalse(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 50, 50, 3), intArrayOf(1, 200, 200, 3)))
        assertFalse(PackModelContract.isDenoiseSameRgb(intArrayOf(1, 180, 320, 3), intArrayOf(1, 720, 1280, 3)))
        assertNull(PackModelContract.superResScale(intArrayOf(1, 256, 256, 3), intArrayOf(1, 256, 256, 3)))
    }

    @Test
    fun frameInterp_acceptsFilmStyleGraph() {
        val time = intArrayOf(1, 1)
        val a = intArrayOf(1, 512, 512, 3)
        val b = intArrayOf(1, 512, 512, 3)
        val outs = listOf(
            intArrayOf(1, 512, 512, 2),
            intArrayOf(1, 512, 512, 3)
        )
        assertTrue(PackModelContract.isFrameInterpFilm(time, a, b, outs))
        assertFalse(PackModelContract.isFrameInterpFilm(intArrayOf(1), a, b, outs))
        assertFalse(
            PackModelContract.isFrameInterpFilm(
                time,
                intArrayOf(1, 256, 256, 3),
                b,
                outs
            )
        )
        assertFalse(
            PackModelContract.isFrameInterpFilm(
                time,
                a,
                b,
                listOf(intArrayOf(1, 512, 512, 2))
            )
        )
    }

    @Test
    fun pointTrack_accepts64Template128SearchToXy() {
        assertTrue(
            PackModelContract.isPointTrack(
                intArrayOf(1, 64, 64, 3),
                intArrayOf(1, 128, 128, 3),
                intArrayOf(1, 2)
            )
        )
        assertFalse(
            PackModelContract.isPointTrack(
                intArrayOf(1, 32, 32, 3),
                intArrayOf(1, 128, 128, 3),
                intArrayOf(1, 2)
            )
        )
        assertFalse(
            PackModelContract.isPointTrack(
                intArrayOf(1, 64, 64, 3),
                intArrayOf(1, 128, 128, 3),
                intArrayOf(1, 4)
            )
        )
        assertEquals(
            0 to 1,
            PackModelContract.trackInputIndices(
                intArrayOf(1, 64, 64, 3),
                intArrayOf(1, 128, 128, 3)
            )
        )
        assertEquals(
            1 to 0,
            PackModelContract.trackInputIndices(
                intArrayOf(1, 128, 128, 3),
                intArrayOf(1, 64, 64, 3)
            )
        )
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
