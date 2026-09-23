package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieHdrModeTest {
    @Test
    fun encodeDecode_roundTrip() {
        assertEquals("sdr", MovieHdrMode.Sdr.encode())
        assertEquals("keep", MovieHdrMode.KeepHdr.encode())
        assertEquals(MovieHdrMode.Sdr, MovieHdrMode.parse(null))
        assertEquals(MovieHdrMode.Sdr, MovieHdrMode.parse("nope"))
        assertEquals(MovieHdrMode.KeepHdr, MovieHdrMode.parse("keep"))
        assertEquals(MovieHdrMode.KeepHdr, MovieHdrMode.parse("HDR"))
        assertTrue(MovieHdrMode.Sdr.isDefault())
        assertFalse(MovieHdrMode.KeepHdr.isDefault())
    }

    @Test
    fun compositionConstants() {
        assertEquals(
            androidx.media3.transformer.Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL,
            MovieHdrMode.Sdr.toCompositionHdrMode()
        )
        assertEquals(
            androidx.media3.transformer.Composition.HDR_MODE_KEEP_HDR,
            MovieHdrMode.KeepHdr.toCompositionHdrMode()
        )
    }

    @Test
    fun timelineRoundTrip_hdr() {
        val tl = Timeline(
            clips = listOf(TimelineClip(id = "c0", mediaIndex = 0, inMs = 0, outMs = 2000)),
            hdrMode = MovieHdrMode.KeepHdr
        )
        val decoded = Timeline.decode(tl.encode(), 1) { 5000 }
        assertEquals(MovieHdrMode.KeepHdr, decoded.hdrMode)
        assertTrue(decoded.encode().contains("HDR,keep"))
        assertEquals(MovieHdrMode.Sdr, Timeline().hdrMode)
        assertFalse(Timeline().encode().contains("HDR,"))
    }

    @Test
    fun mtRoundTrip_hdr() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(TimelineClip(id = "c0", mediaIndex = 0, inMs = 0, outMs = 3000)),
                hdrMode = MovieHdrMode.KeepHdr
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(MovieHdrMode.KeepHdr, decoded.hdrMode)
        assertEquals(MovieHdrMode.KeepHdr, decoded.toFlatTimeline().hdrMode)
        assertTrue(decoded.encode().contains("HDR,keep"))
        assertEquals(
            MovieHdrMode.KeepHdr,
            MultiTrackTimeline.fromFlatTimeline(
                Timeline(hdrMode = MovieHdrMode.KeepHdr)
            ).withPrimaryFlat(Timeline(hdrMode = MovieHdrMode.KeepHdr)).hdrMode
        )
    }
}
