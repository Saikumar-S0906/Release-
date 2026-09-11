package com.pixijoy.ai.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for [CreateExtraAlgorithms] (no Robolectric / Bitmap).
 * Bitmap expand + Android ColorMatrix paths are exercised via layout / matrix math helpers.
 */
class CreateExtraAlgorithmTest {

    @Test
    fun expandPaddingRatio_cyclesFourTiers() {
        assertEquals(0.10f, CreateExtraAlgorithms.expandPaddingRatio(0), 0.0001f)
        assertEquals(0.18f, CreateExtraAlgorithms.expandPaddingRatio(1), 0.0001f)
        assertEquals(0.25f, CreateExtraAlgorithms.expandPaddingRatio(2), 0.0001f)
        assertEquals(0.35f, CreateExtraAlgorithms.expandPaddingRatio(3), 0.0001f)
        assertEquals(0.10f, CreateExtraAlgorithms.expandPaddingRatio(4), 0.0001f)
        assertEquals(0.10f, CreateExtraAlgorithms.expandPaddingRatio(-1), 0.0001f)
    }

    @Test
    fun expandCanvasLayout_padsBothSides() {
        val layout = CreateExtraAlgorithms.expandCanvasLayout(100, 80, 0.10f)
        assertEquals(120, layout.outWidth) // 100 + 10*2
        assertEquals(96, layout.outHeight) // 80 + 8*2
        assertEquals(10, layout.offsetX)
        assertEquals(8, layout.offsetY)
    }

    @Test
    fun expandCanvasLayout_minimumPadIsOne() {
        val layout = CreateExtraAlgorithms.expandCanvasLayout(10, 10, 0f)
        assertEquals(12, layout.outWidth)
        assertEquals(12, layout.outHeight)
        assertEquals(1, layout.offsetX)
        assertEquals(1, layout.offsetY)
    }

    @Test
    fun expandCanvasLayout_clampsRatioAboveOne() {
        val layout = CreateExtraAlgorithms.expandCanvasLayout(50, 50, 2f)
        assertEquals(150, layout.outWidth)
        assertEquals(150, layout.outHeight)
        assertEquals(50, layout.offsetX)
    }

    @Test(expected = IllegalArgumentException::class)
    fun expandCanvasLayout_rejectsNonPositiveSize() {
        CreateExtraAlgorithms.expandCanvasLayout(0, 10, 0.1f)
    }

    @Test
    fun restoreContrastMatrix_hasExpectedShapeAndDiagonal() {
        val m = CreateExtraAlgorithms.restoreContrastMatrix()
        assertEquals(20, m.size)
        assertEquals(1.15f, m[0], 0.0001f)
        assertEquals(1.15f, m[6], 0.0001f)
        assertEquals(1.15f, m[12], 0.0001f)
        assertEquals(1f, m[18], 0.0001f)
        assertEquals(12f, m[4], 0.0001f)
        assertEquals(12f, m[9], 0.0001f)
        assertEquals(12f, m[14], 0.0001f)
    }

    @Test
    fun restoreSaturationFactor_isAboveNeutral() {
        assertTrue(CreateExtraAlgorithms.restoreSaturationFactor() > 1f)
    }

    @Test
    fun applyColorMatrixArgb_identityPreservesPixel() {
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val color = 0xFF406080.toInt()
        assertEquals(color, CreateExtraAlgorithms.applyColorMatrixArgb(color, identity))
    }

    @Test
    fun applyColorMatrixArgb_restoreContrastBrightensMidGray() {
        val mid = 0xFF808080.toInt()
        val out = CreateExtraAlgorithms.applyColorMatrixArgb(mid, CreateExtraAlgorithms.restoreContrastMatrix())
        val outR = (out shr 16) and 0xFF
        val inR = (mid shr 16) and 0xFF
        // 1.15 * 128 + 12 ≈ 159
        assertTrue("expected brighter channel, got $outR from $inR", outR > inR)
        assertEquals(159, outR)
    }

    @Test
    fun applyColorMatrixArgb_clampsToByteRange() {
        val bright = floatArrayOf(
            2f, 0f, 0f, 0f, 200f,
            0f, 2f, 0f, 0f, 200f,
            0f, 0f, 2f, 0f, 200f,
            0f, 0f, 0f, 1f, 0f
        )
        val out = CreateExtraAlgorithms.applyColorMatrixArgb(0xFFFFFFFF.toInt(), bright)
        assertEquals(255, (out shr 16) and 0xFF)
        assertEquals(255, (out shr 8) and 0xFF)
        assertEquals(255, out and 0xFF)
    }

    @Test(expected = IllegalArgumentException::class)
    fun applyColorMatrixArgb_rejectsWrongLength() {
        CreateExtraAlgorithms.applyColorMatrixArgb(0xFF000000.toInt(), floatArrayOf(1f))
    }

    @Test
    fun squarePadSide_usesMaxDimension() {
        assertEquals(200, CreateExtraAlgorithms.squarePadSide(100, 200))
        assertEquals(120, CreateExtraAlgorithms.squarePadSide(120, 80))
        assertEquals(50, CreateExtraAlgorithms.squarePadSide(50, 50))
    }

    @Test(expected = IllegalArgumentException::class)
    fun squarePadSide_rejectsNonPositive() {
        CreateExtraAlgorithms.squarePadSide(0, 10)
    }

    @Test
    fun cappedScaledSide_caps4xOf2048AtMax() {
        assertEquals(2048, CreateExtraAlgorithms.cappedScaledSide(2048, 4, 2048))
        assertEquals(2048, CreateExtraAlgorithms.cappedScaledSide(1024, 4, 2048))
        assertEquals(1024, CreateExtraAlgorithms.cappedScaledSide(512, 2, 2048))
    }
}
