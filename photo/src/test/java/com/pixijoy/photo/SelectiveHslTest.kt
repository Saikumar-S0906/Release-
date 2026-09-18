package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Graph encode/decode + soft-mask math for [PhotoOp.SelectiveHsl].
 * Letter **A** (global [PhotoOp.Hsl] stays **Y**). Bitmap bake covered by engine compile.
 */
class SelectiveHslTest {

    @Test
    fun encodeDecode_selectiveHsl_letterA() {
        val op = PhotoOp.SelectiveHsl(
            range = SelectiveHslRange.Greens,
            hue = -18f,
            sat = 1.35f,
            light = 0.12f
        )
        val encoded = listOf(op).encodeGraph()
        assertTrue(encoded.trim().startsWith("A,Greens,"))
        val decoded = encoded.decodeGraph().single() as PhotoOp.SelectiveHsl
        assertEquals(SelectiveHslRange.Greens, decoded.range)
        assertEquals(-18f, decoded.hue, 0.0001f)
        assertEquals(1.35f, decoded.sat, 0.0001f)
        assertEquals(0.12f, decoded.light, 0.0001f)
    }

    @Test
    fun encodeDecode_globalHsl_staysY() {
        val encoded = listOf(PhotoOp.Hsl(-12f, 1.2f)).encodeGraph()
        assertTrue(encoded.trim().startsWith("Y,"))
        assertFalse(encoded.trim().startsWith("A,"))
        val again = encoded.decodeGraph().single() as PhotoOp.Hsl
        assertEquals(-12f, again.hue, 0.0001f)
        assertEquals(1.2f, again.saturation, 0.0001f)
    }

    @Test
    fun graphRoundTrip_mixedWithGlobalHsl() {
        val ops = listOf(
            PhotoOp.Hsl(10f, 1.1f),
            PhotoOp.SelectiveHsl(SelectiveHslRange.Reds, -8f, 1.4f, -0.05f),
            PhotoOp.SelectiveHsl(SelectiveHslRange.Blues, 20f, 0.8f, 0.2f),
            PhotoOp.SelectiveHsl(SelectiveHslRange.Magentas, 0f, 1f, 0f)
        )
        assertEquals(ops, ops.encodeGraph().decodeGraph())
        assertEquals("A", ops.encodeGraph().lineSequence().elementAt(1).substringBefore(','))
        assertEquals("Y", ops.encodeGraph().lineSequence().elementAt(0).substringBefore(','))
    }

    @Test
    fun allRanges_roundTrip() {
        SelectiveHslRange.values().forEach { range ->
            val op = PhotoOp.SelectiveHsl(range, 5f, 0.9f, -0.1f)
            val again = listOf(op).encodeGraph().decodeGraph().single() as PhotoOp.SelectiveHsl
            assertEquals(range, again.range)
        }
    }

    @Test
    fun hueRangeWeight_peaksInsideBand() {
        assertEquals(1f, SelectiveHslRange.Reds.weight(0f), 0.05f)
        assertEquals(1f, SelectiveHslRange.Greens.weight(120f), 0.05f)
        assertTrue(SelectiveHslRange.Reds.weight(180f) < 0.05f)
        assertTrue(SelectiveHslRange.Oranges.weight(30f) > 0.9f)
        assertTrue(SelectiveHslRange.Yellows.weight(60f) > 0.9f)
        assertTrue(SelectiveHslRange.Cyans.weight(180f) > 0.9f)
        assertTrue(SelectiveHslRange.Blues.weight(240f) > 0.9f)
        assertTrue(SelectiveHslRange.Magentas.weight(300f) > 0.9f)
    }

    @Test
    fun softMask_feathersAtEdges() {
        // Near red/orange boundary (~15°) both bands should have partial weight
        val at15 = 15f
        assertTrue(SelectiveHslRange.Reds.weight(at15) in 0.05f..0.95f)
        assertTrue(SelectiveHslRange.Oranges.weight(at15) in 0.05f..0.95f)
    }

    @Test
    fun applySelectiveHslToHsv_boostsTargetOnly() {
        val red = floatArrayOf(0f, 1f, 0.5f)
        val green = floatArrayOf(120f, 1f, 0.5f)
        applySelectiveHslToHsv(red, SelectiveHslRange.Reds, hue = 0f, sat = 1f, light = 0.4f)
        applySelectiveHslToHsv(green, SelectiveHslRange.Reds, hue = 0f, sat = 1f, light = 0.4f)
        assertEquals(0.9f, red[2], 0.05f) // 0.5 + 0.4 * ~1
        assertEquals(0.5f, green[2], 0.02f) // untouched
    }

    @Test
    fun identitySelective_isNoOp() {
        assertTrue(PhotoOp.SelectiveHsl(SelectiveHslRange.Cyans, 0f, 1f, 0f).isIdentity())
        assertFalse(PhotoOp.SelectiveHsl(SelectiveHslRange.Cyans, 1f, 1f, 0f).isIdentity())
        assertFalse(PhotoOp.SelectiveHsl(SelectiveHslRange.Cyans, 0f, 1.1f, 0f).isIdentity())
        assertFalse(PhotoOp.SelectiveHsl(SelectiveHslRange.Cyans, 0f, 1f, 0.05f).isIdentity())
    }

    @Test
    fun unknownRange_decodeSkips() {
        assertTrue("A,Nope,0,1,0".decodeGraph().isEmpty())
        assertTrue("A,Greens".decodeGraph().isEmpty())
    }
}
