package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C4 SkyReplace — graph letter H + heuristic matte math (JVM, no Bitmap).
 */
class SkyReplaceTest {
    @Test
    fun encodeDecode_letterH() {
        val sky = PhotoOp.SkyReplace(SkyReplaceTarget.Sky, SkyReplacePresets.BlueSky)
        val encoded = listOf(sky).encodeGraph()
        assertTrue(encoded.trim().startsWith("H,Sky,"))
        val decoded = encoded.decodeGraph().single() as PhotoOp.SkyReplace
        assertEquals(SkyReplaceTarget.Sky, decoded.target)
        assertEquals(SkyReplacePresets.BlueSky, decoded.color)

        val bg = PhotoOp.SkyReplace(SkyReplaceTarget.Background, SkyReplacePresets.StudioGreen)
        assertEquals(bg, listOf(bg).encodeGraph().decodeGraph().single())
    }

    @Test
    fun skyMatte_prefersTopBlue() {
        val w = 32
        val h = 32
        val blue = 0xFF78B4E6.toInt()
        val green = 0xFF287828.toInt()
        val px = IntArray(w * h) { i ->
            val y = i / w
            if (y < h / 3) blue else green
        }
        val matte = buildSkyMatte(px, w, h)
        val top = matte[2 * w + 16]
        val bottom = matte[(h - 2) * w + 16]
        assertTrue("top sky score=$top", top > 0.4f)
        assertTrue("bottom ground score=$bottom", bottom < top)
    }

    @Test
    fun backgroundMatte_fromMaskArgb() {
        val maskPx = IntArray(16 * 16) { 0 }
        for (y in 4..11) for (x in 4..11) {
            maskPx[y * 16 + x] = 0xFFFFFFFF.toInt()
        }
        val matte = backgroundMatteFromMaskArgb(maskPx)
        assertEquals(0f, matte[8 * 16 + 8], 0.01f)
        assertTrue(matte[0] > 0.9f)
    }

    @Test
    fun skyReplace_notIdentity_andLayerName() {
        val op = PhotoOp.SkyReplace(SkyReplaceTarget.Sky, SkyReplacePresets.BlueSky)
        assertTrue(!op.isIdentity())
        assertEquals("Sky", op.layerName())
        assertEquals(
            "Backdrop",
            PhotoOp.SkyReplace(SkyReplaceTarget.Background, 0).layerName()
        )
    }
}
