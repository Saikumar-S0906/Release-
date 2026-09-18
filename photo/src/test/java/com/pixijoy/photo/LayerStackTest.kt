package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerStackTest {
    @Test
    fun encodeDecode_twoLayersAndMask_roundTrip() {
        val layers = listOf(
            LayeredOp(
                id = "L1",
                name = "Grade",
                visible = true,
                opacity = 0.8f,
                blend = BlendMode.Multiply,
                op = PhotoOp.Grade(0.1f, 1.1f),
                mask = listOf(MaskStroke(0.4f, 0.5f, 0.1f, reveal = true)),
                maskFill = MaskFill.None
            ),
            LayeredOp(
                id = "L2",
                name = "Heal",
                visible = false,
                opacity = 1f,
                blend = BlendMode.Normal,
                op = PhotoOp.Heal(emptyList()),
            )
        )
        val decoded = layers.encodeLayers().decodeLayers()
        assertEquals(2, decoded.size)
        assertEquals("L1", decoded[0].id)
        assertEquals(0.8f, decoded[0].opacity, 0.001f)
        assertEquals(BlendMode.Multiply, decoded[0].blend)
        assertEquals(1, decoded[0].mask.size)
        assertTrue(decoded[0].mask[0].reveal)
        assertEquals("L2", decoded[1].id)
        assertEquals(false, decoded[1].visible)
    }

    @Test
    fun toOps_skipsHiddenLayers() {
        val layers = listOf(
            LayeredOp("a", "Grade", visible = true, op = PhotoOp.Grade(0f, 1f)),
            LayeredOp("b", "Blur", visible = false, op = PhotoOp.Blur(2))
        )
        val ops = layers.toOps()
        assertEquals(1, ops.size)
        assertTrue(ops[0] is PhotoOp.Grade)
    }
}
