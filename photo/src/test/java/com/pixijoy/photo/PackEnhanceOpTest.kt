package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackEnhanceOpTest {
    @Test
    fun superResDenoise_roundTrip() {
        val ops = listOf(PhotoOp.SuperRes(2), PhotoOp.SuperRes(4), PhotoOp.Denoise(0.55f))
        val decoded = ops.encodeGraph().decodeGraph()
        assertEquals(ops, decoded)
        assertTrue(listOf(PhotoOp.SuperRes(2)).encodeGraph().startsWith("SR,2"))
        assertTrue(listOf(PhotoOp.Denoise(0.5f)).encodeGraph().startsWith("N,"))
    }

    @Test
    fun identityAndNames() {
        assertTrue(PhotoOp.SuperRes(1).isIdentity())
        assertFalse(PhotoOp.SuperRes(2).isIdentity())
        assertTrue(PhotoOp.Denoise(0f).isIdentity())
        assertFalse(PhotoOp.Denoise(0.2f).isIdentity())
        assertEquals("Super-res", PhotoOp.SuperRes(2).layerName())
        assertEquals("Denoise", PhotoOp.Denoise(0.3f).layerName())
        assertTrue(PhotoOp.SuperRes(2).isGeometry())
        assertFalse(PhotoOp.Denoise(0.3f).isGeometry())
    }

    @Test
    fun enginesStayUnboundInBase() {
        assertFalse(PackEnhance.superResAvailable())
        assertFalse(PackEnhance.denoiseAvailable())
        assertFalse(LiteRtInpaintHook.isAvailable())
    }
}
