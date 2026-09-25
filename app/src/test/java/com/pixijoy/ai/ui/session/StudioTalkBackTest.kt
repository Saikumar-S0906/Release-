package com.pixijoy.ai.ui.session

import androidx.compose.ui.geometry.Rect
import com.pixijoy.photo.PhotoOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StudioTalkBackTest {
    @Test
    fun talkBackName_selectedRailIncludesState() {
        assertEquals("Looks, selected", talkBackName("Looks", selected = true, "%1\$s, selected"))
    }

    @Test
    fun talkBackName_unselectedIsTheShortVerb() {
        val spoken = talkBackName("Crop", selected = false, "%1\$s, selected")
        assertEquals("Crop", spoken)
        assertFalse(spoken.contains("button", ignoreCase = true))
        assertFalse(spoken.contains("tab", ignoreCase = true))
    }

    @Test
    fun packKind_downloadedUntilEngineBound() {
        assertEquals(
            PackTalkBackKind.Downloaded,
            packTalkBackKind(fileReady = true, engineReady = false)
        )
        val phrase = packTalkBackPhrase(
            PackTalkBackKind.Downloaded,
            downloaded = "Downloaded",
            readyToApply = "Ready to apply"
        )
        assertEquals("Downloaded", phrase)
        assertFalse(phrase!!.contains("neural", ignoreCase = true))
        assertFalse(phrase.contains("AI"))
    }

    @Test
    fun packKind_readyToApplyOnlyWhenEngineBound() {
        assertEquals(
            PackTalkBackKind.ReadyToApply,
            packTalkBackKind(fileReady = true, engineReady = true)
        )
        assertEquals(
            "Ready to apply",
            packTalkBackPhrase(
                PackTalkBackKind.ReadyToApply,
                downloaded = "Downloaded",
                readyToApply = "Ready to apply"
            )
        )
        assertEquals(PackTalkBackKind.Other, packTalkBackKind(fileReady = false, engineReady = true))
        assertEquals(PackTalkBackKind.Other, packTalkBackKind(fileReady = false, engineReady = false))
        assertNull(
            packTalkBackPhrase(
                PackTalkBackKind.Other,
                downloaded = "Downloaded",
                readyToApply = "Ready to apply"
            )
        )
    }

    @Test
    fun cropEdgeTags_coverEachEdgeOnce() {
        val tags = CropBound.entries.map { cropEdgeTag(it) }
        assertEquals(CropBound.entries.size, tags.toSet().size)
        assertEquals(
            setOf(
                "session_crop_edge_left",
                "session_crop_edge_right",
                "session_crop_edge_top",
                "session_crop_edge_bottom"
            ),
            tags.toSet()
        )
    }

    @Test
    fun cropEdgeOffset_matchesTheEdgeMidpoint() {
        val image = Rect(10f, 20f, 110f, 220f)
        val crop = PhotoOp.Crop(left = 0.25f, top = 0.5f, right = 0.75f, bottom = 1f)
        val left = cropEdgeOffset(image, crop, CropBound.Left)
        val bottom = cropEdgeOffset(image, crop, CropBound.Bottom)
        assertEquals(35f, left.x, 0.001f)
        assertEquals(170f, left.y, 0.001f)
        assertEquals(60f, bottom.x, 0.001f)
        assertEquals(220f, bottom.y, 0.001f)
    }

    @Test
    fun cropHandleOffset_matchesTheCorner() {
        val image = Rect(10f, 20f, 110f, 220f)
        val crop = PhotoOp.Crop(left = 0.25f, top = 0.5f, right = 0.75f, bottom = 1f)
        val tl = cropHandleOffset(image, crop, CropHandle.TL)
        val br = cropHandleOffset(image, crop, CropHandle.BR)
        assertEquals(35f, tl.x, 0.001f)
        assertEquals(120f, tl.y, 0.001f)
        assertEquals(85f, br.x, 0.001f)
        assertEquals(220f, br.y, 0.001f)
    }
}
