package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioCapabilityTest {
    @Test
    fun layerLabels_matchProductTruth() {
        assertEquals("Quick enhance", PhotoOp.Restore.layerName())
        assertEquals("Mirror expand", PhotoOp.Expand(0.12f).layerName())
        assertEquals("Basic cutout", PhotoOp.Cutout.layerName())
        assertEquals("Basic cutout", PhotoOp.Subject.layerName())
        assertEquals("Fill", PhotoOp.Fill(emptyList()).layerName())
    }

    @Test
    fun neuralInpaint_isUnavailable() {
        assertFalse(LiteRtInpaintHook.isAvailable())
        assertFalse(PackEnhance.inpaintAvailable())
        assertFalse(PackEnhance.superResAvailable())
        assertFalse(PackEnhance.denoiseAvailable())
    }

    @Test
    fun avif_isNotASelectableChip() {
        val keys = ExportFormatSupport.chipFormats().map { it.first }
        assertFalse("avif" in keys)
        assertTrue("jpeg" in keys)
        assertEquals(ExportFormat.JPEG, ExportFormatSupport.resolve(ExportFormat.AVIF))
    }

    @Test
    fun selectiveHsl_staysOnCpuPath() {
        assertFalse(SelectiveHslGpu.requested)
    }
}
