package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportExifPolicyTest {
    @Test
    fun stripAll_writesNothing() {
        assertFalse(ExportExifPolicy.shouldWriteTag("DateTime", keepExif = false, stripGps = true))
        assertFalse(ExportExifPolicy.shouldWriteTag("GPSLatitude", keepExif = false, stripGps = true))
    }

    @Test
    fun keepExif_stillStripsGpsByDefault() {
        assertFalse(
            ExportExifPolicy.shouldWriteTag(
                "GPSLatitude",
                keepExif = true,
                stripGps = ExportExifPolicy.DEFAULT_STRIP_GPS
            )
        )
        assertFalse(ExportExifPolicy.isGpsTag("DateTime"))
        assertTrue(ExportExifPolicy.isGpsTag("GPSLatitude"))
        assertTrue(ExportExifPolicy.isGpsTag("GPSLongitude"))
    }

    @Test
    fun keepExif_allowsNonGpsCopyTags() {
        for (tag in ExportExifPolicy.COPY_TAGS) {
            assertTrue(
                "expected keep for $tag",
                ExportExifPolicy.shouldWriteTag(tag, keepExif = true, stripGps = true)
            )
        }
    }

    @Test
    fun formatResolve_avifFallsBack() {
        assertTrue(ExportFormatSupport.usedFallback(ExportFormat.AVIF))
        assertEquals(ExportFormat.JPEG, ExportFormatSupport.resolve(ExportFormat.AVIF))
    }

    @Test
    fun chipFormats_excludeUnavailableAvif() {
        val keys = ExportFormatSupport.chipFormats().map { it.first }
        assertEquals(listOf("jpeg", "png", "webp", "heic"), keys)
        assertFalse("AVIF must not be selectable without an encoder", "avif" in keys)
    }
}
