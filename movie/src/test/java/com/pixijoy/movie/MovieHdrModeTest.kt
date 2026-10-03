package com.pixijoy.movie

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun keepHdr_doesNotForceH264() {
        val kept = chooseHdrExport(MovieHdrMode.KeepHdr, deviceCanWriteHdr = true)
        assertEquals(MovieHdrMode.KeepHdr, kept.mode)
        assertEquals(MimeTypes.VIDEO_H265, kept.videoMimeType)
        assertNotEquals(MimeTypes.VIDEO_H264, kept.videoMimeType)
        assertFalse(kept.fellBackToSdr)

        val fell = chooseHdrExport(MovieHdrMode.KeepHdr, deviceCanWriteHdr = false)
        assertEquals(MovieHdrMode.Sdr, fell.mode)
        assertEquals(MimeTypes.VIDEO_H264, fell.videoMimeType)
        assertTrue(fell.fellBackToSdr)

        val sdr = chooseHdrExport(MovieHdrMode.Sdr, deviceCanWriteHdr = true)
        assertEquals(MimeTypes.VIDEO_H264, sdr.videoMimeType)
        assertFalse(sdr.fellBackToSdr)
    }

    @Test
    fun gifFrame_usesResolutionChip() {
        assertEquals(1280 to 720, MovieExportSize.gifFrameSize(720))
        assertEquals(1920 to 1080, MovieExportSize.gifFrameSize(1080))
        assertEquals(3840 to 2160, MovieExportSize.gifFrameSize(2160))
        assertEquals(1280 to 720, MovieExportSize.gifFrameSize(480))
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
