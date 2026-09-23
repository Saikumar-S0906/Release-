package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTrackTimelineTest {
    @Test
    fun encodeDecode_roundTrip() {
        val original = MultiTrackTimeline.fromSingleVideo(
            listOf(
                TimelineClip(id = "c1", mediaIndex = 0, inMs = 0, outMs = 2000, speed = 1f)
            )
        ).copy(
            tracks = listOf(
                TimelineTrack(
                    id = "V1",
                    kind = TrackKind.Video,
                    name = "Video 1",
                    clips = listOf(
                        TimelineClip(id = "c1", mediaIndex = 0, inMs = 0, outMs = 2000)
                    )
                ),
                TimelineTrack(
                    id = "A1",
                    kind = TrackKind.Audio,
                    name = "Audio 1",
                    clips = emptyList()
                )
            )
        )
        val encoded = original.encode()
        assertTrue(encoded.startsWith("MT,"))
        val decoded = MultiTrackTimeline.decode(encoded)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.tracks.size)
        assertEquals(TrackKind.Video, decoded.tracks[0].kind)
        assertEquals(TrackKind.Audio, decoded.tracks[1].kind)
        assertEquals(1, decoded.tracks[0].clips.size)
        assertEquals(2000, decoded.tracks[0].clips[0].outMs)
    }
}
