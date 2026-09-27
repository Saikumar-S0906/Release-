package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineAssemblyTest {
    @Test
    fun marker_encodeDecode() {
        val m = TimelineMarker("mk1", 1500, MarkerKind.Vfx, "boom in")
        val back = TimelineMarker.decodeLine(m.encodeLine().split(','))!!
        assertEquals(m.id, back.id)
        assertEquals(m.ms, back.ms)
        assertEquals(m.kind, back.kind)
        assertEquals(m.note, back.note)
    }

    @Test
    fun bin_encodeDecode() {
        val b = ProjectBin("bin_sel", "Selects", listOf(0, 2))
        val back = ProjectBin.decodeLine(b.encodeLine().split(','))!!
        assertEquals(b.id, back.id)
        assertEquals(b.name, back.name)
        assertEquals(b.mediaIndices, back.mediaIndices)
    }

    @Test
    fun multiTrack_markersBinsVersions_roundTrip() {
        val base = MultiTrackTimeline.fromSingleVideo(
            listOf(
                TimelineClip("a", 0, 0, 1000),
                TimelineClip("b", 0, 1000, 2000)
            )
        ).ensureDefaultBins()
            .copy(markers = listOf(TimelineMarker("mk0", 500, MarkerKind.Beat, "hit")))
        val saved = base.saveAssembly("v1_assembly")
        val encoded = saved.encode()
        val back = MultiTrackTimeline.decode(encoded)!!
        assertEquals(1, back.markers.size)
        assertTrue(back.bins.isNotEmpty())
        assertEquals(1, back.assemblies.size)
        assertEquals("v1_assembly", back.assemblies.first().name)

        val alt = back.copy(
            markers = back.markers + TimelineMarker("mk1", 900, MarkerKind.Music)
        ).saveAssembly("director_alt")
        val switched = alt.switchAssembly(alt.assemblies.first { it.name == "v1_assembly" }.id)
        assertEquals(1, switched.markers.size)
        assertEquals("v1_assembly", switched.assemblies.first { it.id == switched.activeAssemblyId }.name)
    }

    @Test
    fun editSnap_magnetizesNearEdge() {
        val edges = listOf(0L, 1000L, 2500L)
        assertEquals(1000L, EditSnap.snapMs(1050, edges, thresholdMs = 120))
        assertEquals(1121L, EditSnap.snapMs(1121, edges, thresholdMs = 120))
    }

    @Test
    fun jlCut_appliesSoftFades() {
        val tl = Timeline(
            clips = listOf(
                TimelineClip("a", 0, 0, 2000),
                TimelineClip("b", 0, 2000, 4000)
            )
        )
        val j = tl.applyJLCut("a", jCut = true, leadMs = 400)
        assertEquals(ClipTransition.Soft, j.clips[0].transition)
        assertTrue(j.clips[0].fadeOutMs >= 400)
        val l = tl.applyJLCut("a", jCut = false, leadMs = 400)
        assertTrue(l.clips[1].fadeInMs >= 400)
    }
}
