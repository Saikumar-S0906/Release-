package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOpTest {
    @Test
    fun cropToPixels_clampsAndKeepsMinimumSize() {
        val px = PhotoOp.Crop(0f, 0f, 1f, 1f).toPixels(100, 80)
        assertEquals(0, px.left)
        assertEquals(0, px.top)
        assertEquals(100, px.right)
        assertEquals(80, px.bottom)

        val tiny = PhotoOp.Crop(0.9f, 0.9f, 0.91f, 0.91f).toPixels(10, 10)
        assertTrue(tiny.width >= 1)
        assertTrue(tiny.height >= 1)
    }

    @Test
    fun graphRoundTrip() {
        val ops = listOf(
            PhotoOp.Crop(0.1f, 0.2f, 0.8f, 0.9f),
            PhotoOp.Rotate(90),
            PhotoOp.FlipH,
            PhotoOp.Grade(0.25f, 1.1f),
            PhotoOp.Hsl(-12f, 1.2f),
            PhotoOp.Restore,
            PhotoOp.Expand(0.15f),
            PhotoOp.Cutout,
            PhotoOp.Heal(listOf(Stamp(0.4f, 0.5f, 0.05f))),
            PhotoOp.Erase(listOf(Stamp(0.2f, 0.3f, 0.04f))),
            PhotoOp.TextLayer("Hello, Pixi", 0.5f, 0.8f, 0.08f, 0xFFFFFFFF.toInt()),
            PhotoOp.Sticker("❤", 0.5f, 0.5f, 0.2f),
            PhotoOp.Collage(2),
            PhotoOp.Curves(floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f)),
            PhotoOp.Blur(3),
            PhotoOp.Sharpen(0.6f),
            PhotoOp.Vignette(0.4f),
            PhotoOp.Subject
        )
        val decoded = ops.encodeGraph().decodeGraph()
        assertEquals(ops, decoded)
        assertTrue("".decodeGraph().isEmpty())
    }

    @Test
    fun curvesIdentityMidpoint() {
        val ys = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        assertEquals(0.5f, evalCurve(0.5f, ys), 0.001f)
        assertEquals(0f, evalCurve(0f, ys), 0.001f)
        assertEquals(1f, evalCurve(1f, ys), 0.001f)
    }

    @Test
    fun colorDistance_identicalIsZero() {
        assertEquals(0f, colorDistance(0xFF336699.toInt(), 0xFF336699.toInt()), 0.01f)
        assertTrue(colorDistance(0xFFFFFFFF.toInt(), 0xFF000000.toInt()) > 400f)
    }

    @Test
    fun payloadEscapesCommas() {
        assertEquals("Hello, Pixi", "Hello, Pixi".encodePayload().decodePayload())
    }

    @Test
    fun layersRoundTripAndBlend() {
        val layers = listOf(
            LayeredOp("1", "Grade", true, 0.8f, BlendMode.Multiply, PhotoOp.Grade(0.1f, 1.1f)),
            LayeredOp("2", "Type", false, 1f, BlendMode.Normal, PhotoOp.TextLayer("Hi", 0.5f, 0.5f, 0.1f, 1))
        )
        val decoded = layers.encodeLayers().decodeLayers()
        assertEquals(layers, decoded)
        assertEquals(128, blendChannel(128, 128, BlendMode.Normal))
        assertTrue(blendChannel(200, 50, BlendMode.Multiply) < 50)
    }

    @Test
    fun maskRoundTripAndRaster() {
        val strokes = listOf(MaskStroke(0.5f, 0.5f, 0.2f, false))
        val layer = LayeredOp("1", "Grade", true, 1f, BlendMode.Normal, PhotoOp.Grade(0f, 1f), strokes)
        val decoded = listOf(layer).encodeLayers().decodeLayers().first()
        assertEquals(strokes, decoded.mask)
        val bits = rasterizeMask(32, 32, strokes)
        assertTrue((bits[16 * 32 + 16].toInt() and 0xff) < 200)
        assertTrue((bits[0].toInt() and 0xff) > 200)
    }

    @Test
    fun subjectFillRoundTripAndRaster() {
        val layer = LayeredOp(
            id = "1",
            name = "Grade",
            op = PhotoOp.Grade(0f, 1f),
            maskFill = MaskFill.Subject
        )
        val decoded = listOf(layer).encodeLayers().decodeLayers().first()
        assertEquals(MaskFill.Subject, decoded.maskFill)
        assertEquals(MaskFill.None, "".decodeLayers().firstOrNull()?.maskFill ?: MaskFill.None)

        val fill = ByteArray(16 * 16) { 0 }
        for (y in 4..11) for (x in 4..11) fill[y * 16 + x] = 255.toByte()
        val subject = rasterizeMask(16, 16, emptyList(), fill)
        assertEquals(255, subject[8 * 16 + 8].toInt() and 0xff)
        assertEquals(0, subject[0].toInt() and 0xff)

        val hideStroke = listOf(MaskStroke(0.5f, 0.5f, 0.15f, false))
        val refined = rasterizeMask(16, 16, hideStroke, fill)
        assertTrue((refined[8 * 16 + 8].toInt() and 0xff) < 200)

        val inverted = invertMask(fill)
        assertEquals(0, inverted[8 * 16 + 8].toInt() and 0xff)
        assertEquals(255, inverted[0].toInt() and 0xff)

        val argb = intArrayOf(0x00000000, 0x80FFFFFF.toInt(), 0xFFFFFFFF.toInt())
        val bytes = maskBytesFromArgb(argb)
        assertEquals(0, bytes[0].toInt() and 0xff)
        assertEquals(128, bytes[1].toInt() and 0xff)
        assertEquals(255, bytes[2].toInt() and 0xff)
        assertEquals(255, maskBytesFromArgb(argb, invert = true)[0].toInt() and 0xff)
    }

    @Test
    fun oldLyWithoutFillStillDecodes() {
        val graph = "LY,1,Grade,1,1.0,Normal\nG,0.0,1.0"
        val decoded = graph.decodeLayers().first()
        assertEquals(MaskFill.None, decoded.maskFill)
        assertEquals("Grade", decoded.name)
    }
}
