package com.pixijoy.studio.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioLayoutTest {
    @Test
    fun phonePortrait_isSinglePane() {
        assertFalse(isStudioTwoPane(411, 891))
        assertFalse(isStudioSessionSideTools(411, 891))
    }

    @Test
    fun phoneLandscape_hubStaysSinglePane_sessionGetsSideTools() {
        assertFalse(isStudioTwoPane(891, 411))
        assertFalse(isStudioTwoPane(840, 400))
        assertFalse(isStudioTwoPane(914, 411))
        assertTrue(isStudioSessionSideTools(891, 411))
        assertTrue(isStudioSessionSideTools(840, 400))
        assertTrue(isStudioSessionSideTools(914, 411))
        assertTrue(isStudioSessionSideTools(700, 360))
        assertTrue(isStudioSessionSideTools(640, 360))
        // Narrow landscape keeps bottom glass (compact / multi-window).
        assertFalse(isStudioSessionSideTools(600, 360))
        assertEquals(StudioWindow.Compact, studioWindow(600, 360))
    }

    @Test
    fun tabletOrFoldInner_isTwoPane() {
        assertTrue(isStudioTwoPane(840, 520))
        assertTrue(isStudioTwoPane(1024, 800))
        // Pixel Tablet AVD: 2560×1600 @ 320dpi
        assertTrue(isStudioTwoPane(800, 500))
        assertTrue(isStudioSessionSideTools(800, 500))
    }

    @Test
    fun phonePortrait_resolutions_stayBottomTools() {
        listOf(360 to 640, 384 to 800, 411 to 891, 412 to 915).forEach { (w, h) ->
            assertEquals(StudioWindow.Compact, studioWindow(w, h))
            assertEquals(StudioChrome.Bottom, studioChrome(w, h, StudioPosture.Flat))
            assertFalse(isStudioTwoPane(w, h))
        }
    }

    @Test
    fun phoneLandscape_resolutions_getSideTools() {
        listOf(640 to 360, 731 to 360, 800 to 360, 891 to 411, 914 to 411).forEach { (w, h) ->
            assertEquals(StudioWindow.PhoneLandscape, studioWindow(w, h))
            assertFalse(isStudioTwoPane(w, h))
            assertTrue(isStudioSessionSideTools(w, h))
        }
    }

    @Test
    fun tabletPortrait_mediumResolutions_areTwoPane() {
        listOf(600 to 960, 768 to 1024).forEach { (w, h) ->
            assertEquals(StudioWindow.Medium, studioWindow(w, h))
            assertTrue(isStudioTwoPane(w, h))
            assertTrue(isStudioSessionSideTools(w, h))
        }
        // A 10 inch portrait is wide enough for the expanded dock.
        assertEquals(StudioWindow.Expanded, studioWindow(800, 1280))
        assertEquals(220, studioSideDockWidthDp(600, 960))
    }

    @Test
    fun tabletLandscape_resolutions_areExpanded() {
        listOf(800 to 500, 960 to 600, 1024 to 768, 1280 to 800).forEach { (w, h) ->
            assertEquals(StudioWindow.Expanded, studioWindow(w, h))
            assertTrue(isStudioTwoPane(w, h))
            assertEquals(360, studioSideDockWidthDp(w, h))
        }
    }

    @Test
    fun foldCover_matchesPhone_innerFollowsSize() {
        assertEquals(StudioWindow.Compact, studioWindow(373, 841))
        assertEquals(StudioWindow.Compact, studioWindow(412, 892))
        assertEquals(StudioWindow.PhoneLandscape, studioWindow(892, 412))
        assertEquals(StudioWindow.Medium, studioWindow(673, 841))
        assertEquals(StudioWindow.Medium, studioWindow(734, 841))
        assertEquals(StudioWindow.Expanded, studioWindow(841, 673))
    }

    @Test
    fun foldPosture_bookAndTabletop() {
        val book = StudioFold(400, 0, 424, 800, vertical = true, separating = true, halfOpened = true)
        assertEquals(StudioPosture.Book, studioPosture(book))
        assertEquals(StudioChrome.Side, studioChrome(673, 841, StudioPosture.Book))
        val table = StudioFold(0, 400, 700, 424, vertical = false, separating = true, halfOpened = true)
        assertEquals(StudioPosture.Tabletop, studioPosture(table))
        assertEquals(StudioChrome.Tabletop, studioChrome(841, 673, StudioPosture.Tabletop))
        assertEquals(StudioChrome.Tabletop, studioChrome(411, 891, StudioPosture.Tabletop))
        assertEquals(StudioPosture.Flat, studioPosture(book.copy(separating = false, halfOpened = false)))
        assertEquals(StudioPosture.Flat, studioPosture(null))
    }

    @Test
    fun paneColumns_followPaneWidth() {
        assertEquals(1, studioPaneColumns(228))
        assertEquals(1, studioPaneColumns(299))
        assertEquals(2, studioPaneColumns(300))
        assertEquals(2, studioPaneColumns(360))
        assertEquals(2, studioPaneColumns(411))
        assertEquals(2, studioPaneColumns(519))
        assertEquals(3, studioPaneColumns(520))
        assertEquals(3, studioPaneColumns(840))
    }

    @Test
    fun groupRail_scrollsWhenLabelsWouldCrush() {
        assertFalse(studioGroupRailScrolls(411, 6))
        assertTrue(studioGroupRailScrolls(411, 8))
        assertTrue(studioGroupRailScrolls(220, 6))
        assertTrue(studioGroupRailScrolls(360, 8))
        assertFalse(studioGroupRailScrolls(800, 8))
        assertFalse(studioGroupRailScrolls(0, 6))
    }

    @Test
    fun timeline_compactsOnlyOnShortSideTools() {
        assertTrue(studioTimelineCompact(sideTools = true, paneHeightDp = 411))
        assertTrue(studioTimelineCompact(sideTools = true, paneHeightDp = 559))
        assertFalse(studioTimelineCompact(sideTools = true, paneHeightDp = 560))
        assertFalse(studioTimelineCompact(sideTools = false, paneHeightDp = 411))
        assertFalse(studioTimelineCompact(sideTools = true, paneHeightDp = 0))
    }

    @Test
    fun openSession_ownsWindowOnPhoneAndTabletop_notOnFlatTablet() {
        assertTrue(sessionOwnsStudioWindow(twoPane = false, posture = StudioPosture.Flat))
        assertTrue(sessionOwnsStudioWindow(twoPane = false, posture = StudioPosture.Tabletop))
        assertTrue(sessionOwnsStudioWindow(twoPane = true, posture = StudioPosture.Tabletop))
        assertFalse(sessionOwnsStudioWindow(twoPane = true, posture = StudioPosture.Flat))
        assertFalse(sessionOwnsStudioWindow(twoPane = true, posture = StudioPosture.Book))
    }

    @Test
    fun glassMax_shortLandscape_leavesCanvas() {
        assertEquals(172, studioGlassMaxDp(411))
        assertEquals(220, studioGlassMaxDp(559))
        assertEquals(280, studioGlassMaxDp(560))
        assertEquals(280, studioGlassMaxDp(891))
    }

    @Test
    fun glassMax_largeFont_growsButCaps() {
        assertEquals(172, studioGlassMaxDp(411, 1f))
        assertTrue(studioGlassMaxDp(411, 1.3f) > 172)
        assertTrue(studioGlassMaxDp(411, 1.3f) <= 260)
        assertTrue(studioGlassMaxDp(411, 1.3f) <= (411 * 55 / 100))
        assertEquals(280, studioGlassMaxDp(891, 1f))
        assertTrue(studioGlassMaxDp(891, 1.5f) > 280)
        assertTrue(studioGlassMaxDp(891, 1.5f) <= 360)
    }

    @Test
    fun sideDock_phoneLandscape_narrowerThanTablet() {
        assertEquals(360, studioSideDockWidthDp(800, 500))
        assertEquals(360, studioSideDockWidthDp(1024, 800))
        // ~36% of 914 → 329, coerced to 320
        assertEquals(320, studioSideDockWidthDp(914, 411))
        assertEquals(280, studioSideDockWidthDp(700, 360))
    }

    @Test
    fun sessionChrome_followsThePaneNotTheWindow() {
        listOf(360 to 640, 384 to 800, 411 to 891, 412 to 915).forEach { (w, h) ->
            assertEquals(StudioChrome.Bottom, studioSessionChrome(w, h, StudioPosture.Flat))
        }
        listOf(640 to 360, 731 to 360, 800 to 360, 891 to 411, 914 to 411).forEach { (w, h) ->
            assertEquals(StudioChrome.Side, studioSessionChrome(w, h, StudioPosture.Flat))
            assertTrue(w - studioSideDockWidthDp(w, h) >= STUDIO_MIN_CANVAS_DP)
        }
        // Tablet portrait detail after the catalog split is a phone-width pane.
        assertEquals(StudioChrome.Bottom, studioSessionChrome(320, 900, StudioPosture.Flat))
        assertEquals(StudioChrome.Bottom, studioSessionChrome(446, 1100, StudioPosture.Flat))
        // Tablet landscape detail is wide enough for a dock and a picture.
        assertEquals(StudioChrome.Side, studioSessionChrome(744, 700, StudioPosture.Flat))
        assertTrue(744 - studioSideDockWidthDp(744, 700) >= STUDIO_MIN_CANVAS_DP)
        // Full-bleed fold postures still own the hinge.
        assertEquals(StudioChrome.Tabletop, studioSessionChrome(411, 891, StudioPosture.Tabletop))
        assertEquals(StudioChrome.Tabletop, studioSessionChrome(841, 673, StudioPosture.Tabletop))
        assertEquals(StudioChrome.Side, studioSessionChrome(673, 841, StudioPosture.Book))
        assertEquals(StudioChrome.Side, studioSessionChrome(841, 673, StudioPosture.Book))
        // A session embedded in a pane that already cleared the hinge stays flat.
        assertEquals(StudioChrome.Bottom, studioSessionChrome(360, 700, StudioPosture.Flat))
    }

    @Test
    fun hingeStart_tracksTheCreaseInsideThePane() {
        val book = StudioFold(900, 0, 940, 1600, vertical = true, separating = true, halfOpened = true)
        // Rail occupies the first 200 px. Crease is 700 px into the pane.
        val fromPane = sessionHingeStartDp(
            fold = book,
            posture = StudioPosture.Book,
            originXPx = 200f,
            originYPx = 0f,
            pxToDp = { px -> px / 2f },
            paneWidthDp = 700,
            paneHeightDp = 800
        )
        assertEquals(350, fromPane)
        val capped = studioHingeStartDp(900, 600)
        assertTrue(capped <= 600 * 72 / 100)
        assertTrue(capped >= minOf(120, 600 / 3))
        val table = StudioFold(0, 800, 1400, 840, vertical = false, separating = true, halfOpened = false)
        val top = sessionHingeStartDp(
            fold = table,
            posture = StudioPosture.Tabletop,
            originXPx = 0f,
            originYPx = 80f,
            pxToDp = { px -> px / 2f },
            paneWidthDp = 700,
            paneHeightDp = 800
        )
        assertEquals(360, top)
        assertEquals(0, sessionHingeStartDp(book, StudioPosture.Flat, 0f, 0f, { it }, 800, 800))
    }

    @Test
    fun multiWindow_halfPhone_staysSinglePane() {
        // Typical portrait split: ~half width, full-ish height.
        assertFalse(isStudioTwoPane(411, 800))
        assertFalse(isStudioSessionSideTools(411, 800))
        assertFalse(isStudioTwoPane(540, 720))
        // Half of Pixel Tablet landscape still under two-pane width gate.
        assertFalse(isStudioTwoPane(400, 500))
        assertFalse(isStudioSessionSideTools(400, 500))
    }
}
