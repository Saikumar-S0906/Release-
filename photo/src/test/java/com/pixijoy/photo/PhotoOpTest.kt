package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PhotoOp / Stamp graph encoding and Heal source semantics.
 *
 * Heal contract (sx/sy nullable):
 * - [Stamp.hasSource] is explicit (sx != null && sy != null), NOT inferred from sx != x.
 * - Stroke landing on the source point still clones (hasSource stays true when sx==x, sy==y).
 * - No-source stamps take the ring path in applyHeal; empty stamp lists are a no-op.
 */
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
            PhotoOp.Sticker("â¤", 0.5f, 0.5f, 0.2f),
            PhotoOp.Collage(2),
            PhotoOp.Curves(floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f)),
            PhotoOp.Blur(3f),
            PhotoOp.Sharpen(0.6f),
            PhotoOp.Vignette(0.4f),
            PhotoOp.Subject,
            PhotoOp.Fill(listOf(Stamp(0.3f, 0.4f, 0.06f))),
            PhotoOp.SkyReplace(SkyReplaceTarget.Sky, SkyReplacePresets.BlueSky),
            PhotoOp.SkyReplace(SkyReplaceTarget.Background, SkyReplacePresets.StudioGreen),
            PhotoOp.Perspective(0.1f, -0.05f),
            PhotoOp.Liquify(0.5f, 0.4f, 0.15f, 0.3f),
            PhotoOp.Soften(0.35f),
            PhotoOp.SuperRes(2),
            PhotoOp.Denoise(0.4f)
        )
        val decoded = ops.encodeGraph().decodeGraph()
        assertEquals(ops, decoded)
        assertTrue("".decodeGraph().isEmpty())
    }

    @Test
    fun histogramClipCountsEnds() {
        val bins = IntArray(256)
        bins[0] = 10
        bins[3] = 10
        bins[128] = 80
        bins[255] = 20
        val clip = histogramClip(bins)
        assertEquals(20f / 120f, clip.shadowPct, 0.001f)
        assertEquals(20f / 120f, clip.highlightPct, 0.001f)
        assertEquals(128, clip.peak)
        val empty = histogramClip(IntArray(0))
        assertEquals(0f, empty.shadowPct, 0.001f)
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
    fun liveGradeIdentityMatrix() {
        val m = LiveGrade.colorMatrix(0f, 1f, 0f, 1f)
        assertEquals(1f, m[0], 0.01f)
        assertEquals(1f, m[6], 0.01f)
        assertEquals(1f, m[12], 0.01f)
        assertEquals(0f, m[4], 0.01f)
        assertTrue(LiveGrade.isAdjustIdentity(0f, 1f, 0f, 1f))
        assertTrue(LiveGrade.curvesIdentity(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)))
    }

    @Test
    fun compareEligibility_ignoresIdentityAndHiddenLayers() {
        val identityGrade = PhotoOp.Grade(0f, 1f)
        assertTrue(identityGrade.isIdentity())
        assertTrue(PhotoOp.Crop(0f, 0f, 1f, 1f).isIdentity())
        assertTrue(PhotoOp.Lut(BundledLooks.ORIGINAL).isIdentity())
        assertFalse(PhotoOp.Grade(0.2f, 1f).isIdentity())

        assertFalse(LayeredOp("base", "Background", op = identityGrade).hasVisibleEffect())
        assertFalse(
            LayeredOp("hidden", "Grade", visible = false, op = PhotoOp.Grade(0.2f, 1f))
                .hasVisibleEffect()
        )
        assertFalse(
            LayeredOp("transparent", "Grade", opacity = 0f, op = PhotoOp.Grade(0.2f, 1f))
                .hasVisibleEffect()
        )
        assertTrue(LayeredOp("grade", "Grade", op = PhotoOp.Grade(0.2f, 1f)).hasVisibleEffect())
        assertTrue(LayeredOp("rotate", "Rotate", opacity = 0f, op = PhotoOp.Rotate(90)).hasVisibleEffect())
    }

    /**
     * Compare hygiene: Apply for Grade / HSL / Curves must NOT push a no-op layer.
     * The UI guards ([onApplyGrade], [onApplyCurves]) call [PhotoOp.isIdentity] before push;
     * these tests document the identity contract so a future refactor cannot regress it.
     */
    @Test
    fun identityApplyHygiene_noOpsShouldNotPush() {
        // Grade at slider defaults is identity
        assertTrue(PhotoOp.Grade(0f, 1f).isIdentity())
        // HSL at slider defaults is identity
        assertTrue(PhotoOp.Hsl(0f, 1f).isIdentity())
        // Curves at default five-point diagonal is identity
        assertTrue(PhotoOp.Curves(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)).isIdentity())
        // Any deviation from defaults is NOT identity
        assertFalse(PhotoOp.Grade(0.1f, 1f).isIdentity())
        assertFalse(PhotoOp.Grade(0f, 1.1f).isIdentity())
        assertFalse(PhotoOp.Hsl(5f, 1f).isIdentity())
        assertFalse(PhotoOp.Hsl(0f, 1.1f).isIdentity())
        assertFalse(PhotoOp.Curves(floatArrayOf(0f, 0.25f, 0.6f, 0.75f, 1f)).isIdentity())
        // hasVisibleEffect must return false for identity layers — Compare stays disabled
        assertFalse(LayeredOp("id1", "Grade", op = PhotoOp.Grade(0f, 1f)).hasVisibleEffect())
        assertFalse(LayeredOp("id2", "HSL", op = PhotoOp.Hsl(0f, 1f)).hasVisibleEffect())
        assertFalse(
            LayeredOp("id3", "Curves", op = PhotoOp.Curves(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)))
                .hasVisibleEffect()
        )
        // Non-identity layers must enable hasVisibleEffect (Compare can activate)
        assertTrue(LayeredOp("id4", "Grade", op = PhotoOp.Grade(0.2f, 1f)).hasVisibleEffect())
        assertTrue(LayeredOp("id5", "Curves", op = PhotoOp.Curves(floatArrayOf(0f, 0.25f, 0.6f, 0.75f, 1f))).hasVisibleEffect())
    }

    @Test
    fun blurRadius_keepsFloatAndOldInt() {
        val legacy = "W,3".decodeGraph().single() as PhotoOp.Blur
        assertEquals(3f, legacy.radius, 0.001f)
        val half = listOf(PhotoOp.Blur(2.5f)).encodeGraph().decodeGraph().single() as PhotoOp.Blur
        assertEquals(2.5f, half.radius, 0.001f)
        assertTrue(PhotoOp.Blur(0.5f).isIdentity())
        assertFalse(PhotoOp.Blur(0.6f).isIdentity())
    }

    @Test
    fun vignetteMulMatchesCpuFalloff() {
        assertEquals(1f, LiveGrade.vignetteMul(0.5f, 0.5f, 0.4f), 0.001f)
        assertEquals(1f, LiveGrade.vignetteMul(0f, 0f, 0f), 0.001f)
        val corner = LiveGrade.vignetteMul(0f, 0f, 1f)
        assertTrue(corner < 0.05f)
        val mid = LiveGrade.vignetteMul(0.15f, 0.15f, 0.8f)
        assertTrue(mid in 0f..1f)
    }

    @Test
    fun liveGradeExposureDoublesGain() {
        val m = LiveGrade.gradeMatrix(1f, 1f)
        assertEquals(2f, m[0], 0.01f)
        assertEquals(2f, m[6], 0.01f)
        assertEquals(2f, m[12], 0.01f)
    }

    @Test
    fun oldLyWithoutFillStillDecodes() {
        val graph = "LY,1,Grade,1,1.0,Normal\nG,0.0,1.0"
        val decoded = graph.decodeLayers().first()
        assertEquals(MaskFill.None, decoded.maskFill)
        assertEquals("Grade", decoded.name)
    }

    @Test
    fun cloneOffsetStampRoundTrip() {
        val stamps = listOf(
            Stamp(0.4f, 0.5f, 0.05f),
            Stamp(0.6f, 0.55f, 0.04f, sx = 0.2f, sy = 0.25f)
        )
        val packed = stamps.pack()
        val unpacked = packed.unpackStamps()
        assertEquals(stamps, unpacked)
        assertTrue(unpacked[1].hasSource)
        assertEquals(0.2f, unpacked[1].sx!!, 0.0001f)
        assertEquals(0.25f, unpacked[1].sy!!, 0.0001f)
    }

    /** Regression: sx==x / sy==y must still count as clone source (not silent ring). */
    @Test
    fun explicitSourceOnSamePointStillClones() {
        val stamp = Stamp(0.5f, 0.5f, 0.04f, sx = 0.5f, sy = 0.5f)
        assertTrue(stamp.hasSource)
        assertNotNull(stamp.sx)
        assertNotNull(stamp.sy)
        val packed = listOf(stamp).pack()
        // 5-field pack proves hasSource was true at encode time
        assertTrue(packed.split(',').size >= 5)
        val again = packed.unpackStamps().single()
        assertTrue(again.hasSource)
        assertEquals(0.5f, again.sx!!, 0.0001f)
        assertEquals(0.5f, again.sy!!, 0.0001f)
        // Graph round-trip keeps clone semantics
        val heal = listOf(PhotoOp.Heal(listOf(stamp)))
        val decoded = heal.encodeGraph().decodeGraph().filterIsInstance<PhotoOp.Heal>().single()
        assertTrue(decoded.stamps.single().hasSource)
    }

    @Test
    fun ringStampHasNoSource() {
        val stamp = Stamp(0.4f, 0.5f, 0.05f)
        assertFalse(stamp.hasSource)
        assertNull(stamp.sx)
        assertNull(stamp.sy)
        val packed = listOf(stamp).pack()
        assertEquals(3, packed.split(',').size)
        assertFalse(packed.unpackStamps().single().hasSource)
    }

    /**
     * applyHeal fork: hasSource â†’ cloneStamp; else â†’ ringHealStamp.
     * Empty stamps: applyHeal early-returns src (no-op).
     *
     * Pixel-level Bitmap assertions need Robolectric / androidTest â€” see [ApplyHealTest].
     */
    @Test
    fun healFork_cloneVsRing_drivenByHasSource() {
        val clone = Stamp(0.6f, 0.6f, 0.05f, sx = 0.2f, sy = 0.3f)
        val colocatedClone = Stamp(0.5f, 0.5f, 0.04f, sx = 0.5f, sy = 0.5f)
        val ring = Stamp(0.4f, 0.5f, 0.05f)
        assertTrue(clone.hasSource)
        assertTrue(colocatedClone.hasSource)
        assertFalse(ring.hasSource)
        // UI Apply with empty pendingStamps must not push PhotoOp.Heal â€” no-op at session layer.
        val emptyHeal = PhotoOp.Heal(emptyList())
        assertTrue(emptyHeal.stamps.isEmpty())
    }
/** Regression: collage_4 must encode/decode as 4 columns, never silently as 2. */
    @Test
    fun collageFourColumnsIsNotTwo() {
        val four = PhotoOp.Collage(4)
        assertEquals(4, four.columns)
        assertTrue(four.columns != 2)

        val graph = listOf(four).encodeGraph()
        assertTrue("expected L,4 in graph, got: $graph", graph.contains("L,4"))
        assertFalse(graph.trim().endsWith("L,2") || graph.contains("\nL,2"))

        val decoded = graph.decodeGraph().filterIsInstance<PhotoOp.Collage>().single()
        assertEquals(4, decoded.columns)
        assertEquals(four, decoded)
    }

    @Test
    fun collageTwoRoundTrip() {
        val two = PhotoOp.Collage(2)
        assertEquals(2, two.columns)
        assertEquals(two, listOf(two).encodeGraph().decodeGraph().single())
    }
}
