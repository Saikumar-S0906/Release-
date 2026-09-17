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
}
