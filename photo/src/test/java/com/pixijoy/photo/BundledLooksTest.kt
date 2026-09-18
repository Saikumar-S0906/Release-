package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledLooksTest {
    @Test
    fun original_isIdentity() {
        assertTrue(BundledLooks.isIdentity(BundledLooks.ORIGINAL))
        assertTrue(BundledLooks.isIdentity(""))
        assertFalse(BundledLooks.isIdentity(BundledLooks.WARM))
    }

    @Test
    fun warm_matrixDistinctFromIdentity() {
        val id = BundledLooks.colorMatrix(BundledLooks.ORIGINAL)
        val warm = BundledLooks.colorMatrix(BundledLooks.WARM)
        assertFalse(id.contentEquals(warm))
        assertEquals(20, warm.size)
    }

    @Test
    fun encodeDecode_lutRoundTrip() {
        val encoded = listOf(PhotoOp.Lut(BundledLooks.FILM)).encodeGraph()
        val decoded = encoded.decodeGraph().filterIsInstance<PhotoOp.Lut>().single()
        assertEquals(BundledLooks.FILM, decoded.assetKey)
    }
}