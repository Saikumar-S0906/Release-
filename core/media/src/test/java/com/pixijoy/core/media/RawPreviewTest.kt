package com.pixijoy.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawPreviewTest {
    @Test
    fun looksLikeRaw_extensions() {
        assertTrue(RawPreview.looksLikeRaw("/x/a.dng"))
        assertTrue(RawPreview.looksLikeRaw("/x/a.NEF"))
        assertFalse(RawPreview.looksLikeRaw("/x/a.jpg"))
    }

    @Test
    fun isRawMime() {
        assertTrue(RawPreview.isRawMime("image/x-adobe-dng"))
        assertFalse(RawPreview.isRawMime("image/jpeg"))
    }
}
