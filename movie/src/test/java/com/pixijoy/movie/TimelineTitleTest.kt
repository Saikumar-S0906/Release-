package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTitleTest {
    @Test
    fun encodeDecode_roundTrip() {
        val title = TimelineTitle(
            id = "t1",
            text = "Hello, world",
            role = TitleRole.Title,
            startMs = 0,
            endMs = 2000,
            anchorX = 0f,
            anchorY = 0.7f
        )
        val caption = TimelineTitle.captionDefault(5000, id = "c1")
        val tl = Timeline(
            clips = listOf(TimelineClip("c", 0, 0, 5000)),
            titles = listOf(title, caption)
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 5000 }
        assertEquals(2, decoded.titles.size)
        assertEquals("Hello, world", decoded.titles[0].text)
        assertEquals(TitleRole.Title, decoded.titles[0].role)
        assertEquals(TitleRole.Caption, decoded.titles[1].role)
    }

    @Test
    fun mtRoundTrip_titles() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip("c", 0, 0, 3000)),
                titles = listOf(TimelineTitle.titleDefault(3000, "t0"))
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())
        assertNotNull(decoded)
        assertEquals(1, decoded!!.titles.size)
        assertEquals("Title", decoded.titles[0].text)
        assertEquals(TitleRole.Title, decoded.titles[0].role)
    }

    @Test
    fun resolvedEnd_usesTimelineWhenOpen() {
        val open = TimelineTitle("t", "X", endMs = -1, startMs = 0)
        assertEquals(4000L, open.resolvedEnd(4000))
        val capped = TimelineTitle("t", "X", endMs = 9000, startMs = 0)
        assertEquals(4000L, capped.resolvedEnd(4000))
    }

    @Test
    fun overlayPreview_textPresent() {
        val titles = listOf(TimelineTitle.titleDefault(2000))
        assertTrue(TitleOverlays.spannablePreview(titles[0]).isNotEmpty())
        assertEquals("Title", TitleOverlays.spannablePreview(titles[0]))
    }

    @Test
    fun withPrimaryFlat_preservesTitles() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip("c", 0, 0, 2000)),
                titles = listOf(TimelineTitle.captionDefault(2000))
            )
        )
        val next = mt.withPrimaryFlat(
            mt.toFlatTimeline().copy(clips = listOf(TimelineClip("c", 0, 0, 2500)))
        )
        assertEquals(1, next.titles.size)
        assertEquals(TitleRole.Caption, next.titles[0].role)
    }
}
