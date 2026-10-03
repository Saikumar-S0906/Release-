package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyScrubTest {
    @Test
    fun fingerDownOnLong4kUsesLighterFrame() {
        val duration = ProxyScrub.LONG_4K_MS
        assertEquals(540, ProxyScrub.previewHeight(duration, 2160, fingerDown = true))
        assertEquals(2160, ProxyScrub.previewHeight(duration, 2160, fingerDown = false))
    }

    @Test
    fun shortOrNot4kStaysOnExportHeight() {
        assertEquals(2160, ProxyScrub.previewHeight(ProxyScrub.LONG_4K_MS - 1, 2160, fingerDown = true))
        assertEquals(1080, ProxyScrub.previewHeight(ProxyScrub.LONG_4K_MS, 1080, fingerDown = true))
        assertEquals(720, ProxyScrub.previewHeight(ProxyScrub.LONG_4K_MS, 720, fingerDown = true))
    }

    @Test
    fun fingerUpAtEndRestoresThatPosition() {
        val end = ProxyScrub.LONG_4K_MS
        assertEquals(end, ProxyScrub.restorePositionMs(end, end))
        assertEquals(1L, ProxyScrub.restorePositionMs(1, end))
        assertNull(ProxyScrub.restorePositionMs(0, end))
        assertNull(ProxyScrub.restorePositionMs(end + 1, end))
    }

    @Test
    fun scrubbingTurnsOffWhenTimelineStopsQualifying() {
        assertTrue(ProxyScrub.scrubbingEnabled(fingerDown = true, ProxyScrub.LONG_4K_MS, 2160))
        assertFalse(ProxyScrub.scrubbingEnabled(fingerDown = true, ProxyScrub.LONG_4K_MS - 1, 2160))
        assertFalse(ProxyScrub.scrubbingEnabled(fingerDown = true, ProxyScrub.LONG_4K_MS, 1080))
        assertFalse(ProxyScrub.scrubbingEnabled(fingerDown = false, ProxyScrub.LONG_4K_MS, 2160))
    }

    @Test
    fun exportLabelStays4k() {
        assertFalse(ProxyScrub.applies(ProxyScrub.LONG_4K_MS, 1080))
        assertEquals("4K", MovieExportSize.label(2160))
        assertEquals("FHD", MovieExportSize.label(1080))
        assertEquals("HD", MovieExportSize.label(720))
    }
}
