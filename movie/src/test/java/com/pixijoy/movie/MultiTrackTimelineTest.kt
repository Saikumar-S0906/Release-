package com.pixijoy.movie

import com.pixijoy.core.common.StudioDiagnostics
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
    fun corruptNumericLines_areSkipped_andTheRestOpens() {
        StudioDiagnostics.clearForTest()
        val raw = """
            MT,1,1080
            TK,V1,Video,Video 1,0
            TC,V1,bad,nope,0,2000,1,0,1,0,0
            TC,V1,c2,0,0,1500,1,0,1,0,0
            B,bed.wav,loud,0,0,0,0.2,40
            V,voice.wav,1,0,0,0,0,40
            S,s1,stem.wav,nope,0,0,0,Music
        """.trimIndent()
        val decoded = MultiTrackTimeline.decode(raw)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.tracks.single().clips.size)
        assertEquals("c2", decoded.tracks.single().clips.single().id)
        assertEquals(1500, decoded.tracks.single().clips.single().outMs)
        assertEquals(null, decoded.bed)
        assertNotNull(decoded.voice)
        assertTrue(decoded.extraStems.isEmpty())
        val notes = StudioDiagnostics.snapshot()
        assertTrue(notes.any { it == "timeline: rejected TC line" })
        assertTrue(notes.any { it == "timeline: rejected B line" })
        assertTrue(notes.any { it == "timeline: rejected S line" })
        StudioDiagnostics.clearForTest()
    }

    @Test
    fun composedVideoClips_skipsMutedTracks_andOrdersTracks() {
        val mt = MultiTrackTimeline(
            tracks = listOf(
                TimelineTrack(
                    id = "V1",
                    kind = TrackKind.Video,
                    name = "Video 1",
                    clips = listOf(TimelineClip(id = "a", mediaIndex = 0, inMs = 0, outMs = 1000))
                ),
                TimelineTrack(
                    id = "V2",
                    kind = TrackKind.Video,
                    name = "Video 2",
                    muted = true,
                    clips = listOf(TimelineClip(id = "b", mediaIndex = 1, inMs = 0, outMs = 500))
                ),
                TimelineTrack(
                    id = "V3",
                    kind = TrackKind.Video,
                    name = "Video 3",
                    clips = listOf(TimelineClip(id = "c", mediaIndex = 2, inMs = 0, outMs = 800))
                ),
                TimelineTrack(id = "A1", kind = TrackKind.Audio, name = "Audio 1")
            )
        )
        assertEquals(listOf("a", "c"), mt.composedVideoClips().map { it.id })
        assertEquals(1800L, mt.totalMs())
    }

    @Test
    fun withPrimaryFlat_preservesExtraTracks() {
        val base = MultiTrackTimeline(
            tracks = listOf(
                TimelineTrack(
                    id = "V1",
                    kind = TrackKind.Video,
                    name = "Video 1",
                    clips = listOf(TimelineClip(id = "old", mediaIndex = 0, inMs = 0, outMs = 1000))
                ),
                TimelineTrack(
                    id = "V2",
                    kind = TrackKind.Video,
                    name = "B-roll",
                    clips = listOf(TimelineClip(id = "broll", mediaIndex = 1, inMs = 0, outMs = 400))
                ),
                TimelineTrack(id = "A1", kind = TrackKind.Audio, name = "Audio 1")
            )
        )
        val next = base.withPrimaryFlat(
            Timeline(clips = listOf(TimelineClip(id = "new", mediaIndex = 0, inMs = 0, outMs = 2000)))
        )
        assertEquals("new", next.tracks.first { it.id == "V1" }.clips.single().id)
        assertEquals("broll", next.tracks.first { it.id == "V2" }.clips.single().id)
        assertEquals(2, next.videoTracks().size)
    }

    @Test
    fun compositionSequencePlan_countsAudioTracksAndBeds() {
        val mt = MultiTrackTimeline(
            tracks = listOf(
                TimelineTrack(
                    id = "V1",
                    kind = TrackKind.Video,
                    name = "Video 1",
                    clips = listOf(TimelineClip(id = "a", mediaIndex = 0, inMs = 0, outMs = 1000))
                ),
                TimelineTrack(
                    id = "A1",
                    kind = TrackKind.Audio,
                    name = "Audio 1",
                    clips = listOf(TimelineClip(id = "sfx", mediaIndex = 0, inMs = 0, outMs = 500))
                )
            ),
            bed = AudioBed(fileName = "bed.m4a")
        )
        // video + audio track + bed
        assertEquals(3, mt.compositionSequencePlan(hasBedFile = true, hasVoiceFile = false, stemCount = 0))
        // muted audio track drops that sequence; bed remains → video + bed
        assertEquals(2, mt.withTrackMuted("A1", true).compositionSequencePlan(true, false, 0))
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
