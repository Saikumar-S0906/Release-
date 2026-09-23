package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave C — Perspective (D), Liquify (M), Soften (O), presets.
 */
class WarpOpsTest {
    @Test
    fun perspectiveLiquifySoften_roundTrip() {
        val ops = listOf(
            PhotoOp.Perspective(0.12f, -0.08f),
            PhotoOp.Liquify(0.5f, 0.4f, 0.18f, 0.35f),
            PhotoOp.Soften(0.4f)
        )
        val decoded = ops.encodeGraph().decodeGraph()
        assertEquals(ops, decoded)
    }

    @Test
    fun warpIdentityGuards() {
        assertTrue(PhotoOp.Perspective(0f, 0f).isIdentity())
        assertFalse(PhotoOp.Perspective(0.1f, 0f).isIdentity())
        assertTrue(PhotoOp.Liquify(0.5f, 0.5f, 0.2f, 0f).isIdentity())
        assertTrue(PhotoOp.Liquify(0.5f, 0.5f, 0f, 0.5f).isIdentity())
        assertFalse(PhotoOp.Liquify(0.5f, 0.5f, 0.2f, 0.3f).isIdentity())
        assertTrue(PhotoOp.Soften(0f).isIdentity())
        assertTrue(PhotoOp.Soften(0.005f).isIdentity())
        assertFalse(PhotoOp.Soften(0.2f).isIdentity())
    }

    @Test
    fun layerNames() {
        assertEquals("Perspective", PhotoOp.Perspective(0.1f, 0f).layerName())
        assertEquals("Liquify", PhotoOp.Liquify(0.5f, 0.5f, 0.1f, 0.2f).layerName())
        assertEquals("Soften", PhotoOp.Soften(0.3f).layerName())
    }

    @Test
    fun geometryOpsIncludePerspective() {
        assertTrue(PhotoOp.Perspective(0.1f, 0f).isGeometry())
        assertFalse(PhotoOp.Liquify(0.5f, 0.5f, 0.1f, 0.2f).isGeometry())
        assertFalse(PhotoOp.Soften(0.3f).isGeometry())
    }

    @Test
    fun presetEncodeDecode() {
        val layers = listOf(
            LayeredOp(id = "L1", name = "Perspective", op = PhotoOp.Perspective(0.1f, -0.05f)),
            LayeredOp(id = "L2", name = "Soften", op = PhotoOp.Soften(0.25f))
        ).encodeLayers()
        val preset = PhotoPreset("p1", "Warm soft", layers, updatedAt = 42L)
        val decoded = PhotoPreset.decode(preset.encode())
        assertNotNull(decoded)
        assertEquals("p1", decoded!!.id)
        assertEquals("Warm soft", decoded.name)
        assertEquals(42L, decoded.updatedAt)
        assertEquals(layers.trim(), decoded.layersEncoded.trim())
        assertEquals(2, decoded.layersEncoded.decodeLayers().size)
    }
}
