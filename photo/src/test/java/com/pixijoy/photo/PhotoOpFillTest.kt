package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Graph pack/unpack + mask helper tests for [PhotoOp.Fill].
 * Pixel Telea asserts need Bitmap / Robolectric — graph coverage is the CI gate.
 */
class PhotoOpFillTest {

    @Test
    fun fillGraph_roundTrip_letterI() {
        val stamps = listOf(
            Stamp(0.35f, 0.40f, 0.05f),
            Stamp(0.60f, 0.55f, 0.04f, sx = 0.10f, sy = 0.12f)
        )
        val encoded = listOf(PhotoOp.Fill(stamps)).encodeGraph()
        assertTrue(encoded.startsWith("I,"))
        assertFalse(encoded.startsWith("B,"))
        val decoded = encoded.decodeGraph().filterIsInstance<PhotoOp.Fill>().single()
        assertEquals(stamps, decoded.stamps)
        assertFalse(decoded.neural)
        assertFalse(encoded.contains(";n"))
        assertTrue(decoded.stamps[1].hasSource)
        assertFalse(decoded.stamps[0].hasSource)
    }

    @Test
    fun fillGraph_neuralFlagRoundTripsAndLegacyStaysTelea() {
        val stamps = listOf(Stamp(0.2f, 0.3f, 0.04f))
        val neural = listOf(PhotoOp.Fill(stamps, neural = true)).encodeGraph().decodeGraph()
            .filterIsInstance<PhotoOp.Fill>().single()
        assertTrue(neural.neural)
        assertEquals(stamps, neural.stamps)
        val legacy = "I,0.2,0.3,0.04".decodeGraph().filterIsInstance<PhotoOp.Fill>().single()
        assertFalse(legacy.neural)
        assertEquals(1, legacy.stamps.size)
        val emptyNeural = listOf(PhotoOp.Fill(emptyList(), neural = true)).encodeGraph()
        assertEquals("I,n", emptyNeural)
        assertTrue(
            emptyNeural.decodeGraph().filterIsInstance<PhotoOp.Fill>().single().neural
        )
    }

    @Test
    fun fillInMixedGraph_preservesNeighbors() {
        val ops = listOf(
            PhotoOp.Grade(0.1f, 1.05f),
            PhotoOp.Fill(listOf(Stamp(0.5f, 0.5f, 0.06f))),
            PhotoOp.Heal(listOf(Stamp(0.2f, 0.3f, 0.04f))),
            PhotoOp.Erase(listOf(Stamp(0.7f, 0.7f, 0.03f)))
        )
        val round = ops.encodeGraph().decodeGraph()
        assertEquals(ops, round)
        assertTrue(round[1] is PhotoOp.Fill)
        assertTrue(round[2] is PhotoOp.Heal)
        assertEquals("I", ops.encodeGraph().lineSequence().elementAt(1).substringBefore(','))
        assertEquals("B", ops.encodeGraph().lineSequence().elementAt(2).substringBefore(','))
    }

    @Test
    fun emptyFill_encodesAndDecodes() {
        val again = listOf(PhotoOp.Fill(emptyList())).encodeGraph().decodeGraph()
            .filterIsInstance<PhotoOp.Fill>().single()
        assertTrue(again.stamps.isEmpty())
    }

    @Test
    fun buildFillMask_marksStampDisk() {
        val mask = buildFillMask(32, 32, listOf(Stamp(0.5f, 0.5f, 0.2f)))
        assertTrue(mask[16 * 32 + 16])
        assertFalse(mask[0])
        assertFalse(mask[31 * 32 + 31])
    }

    @Test
    fun neuralInpaint_isUnavailableInBaseApk() {
        assertFalse(LiteRtInpaintHook.isAvailable())
    }
}
