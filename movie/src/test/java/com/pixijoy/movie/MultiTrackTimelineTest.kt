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

    @Test
    fun flatTimeline_bridge_preservesClipsBedsAndStems() {
        val flat = Timeline(
            clips = listOf(
                TimelineClip(id = "c1", mediaIndex = 0, inMs = 0, outMs = 1500, speed = 1.5f)
            ),
            bed = AudioBed(fileName = "bed.m4a", volume = 0.7f),
            voice = AudioBed(fileName = "vo.m4a", volume = 1f, duck = 1f),
            extraStems = listOf(AudioStem(id = "sfx1", fileName = "hit.wav", role = "sfx")),
            outputHeight = 1080,
            masterFx = AudioMasterFx(lowGainDb = 1f)
        )
        val multi = MultiTrackTimeline.fromFlatTimeline(flat)
        assertEquals(2, multi.tracks.size)
        assertEquals(flat.clips, multi.toFlatTimeline().clips)
        val round = MultiTrackTimeline.decode(multi.encode())!!.toFlatTimeline()
        assertEquals(flat.clips, round.clips)
        assertEquals(flat.bed, round.bed)
        assertEquals(flat.voice, round.voice)
        assertEquals(flat.extraStems, round.extraStems)
        assertEquals(flat.outputHeight, round.outputHeight)
        assertEquals(1f, round.masterFx.lowGainDb, 0.01f)
    }
}
