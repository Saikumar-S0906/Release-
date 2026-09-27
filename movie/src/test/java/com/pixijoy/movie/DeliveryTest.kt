package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeliveryTest {
    @Test
    fun tc_formatRoundTrip() {
        assertEquals("00:00:00:00", DeliveryTc.format(0))
        assertEquals("00:00:01:00", DeliveryTc.format(1000))
        assertEquals(1000L, DeliveryTc.parseToMs("00:00:01:00"))
    }

    @Test
    fun preset_appliesHeightAndHdr() {
        val base = MultiTrackTimeline.fromSingleVideo(
            listOf(TimelineClip("c0", 0, 0, 2000))
        )
        assertEquals(720, DeliverablePreset.Social.apply(base).outputHeight)
        assertEquals(MovieHdrMode.Sdr, DeliverablePreset.Social.apply(base).hdrMode)
        assertEquals(1080, DeliverablePreset.Review.apply(base).outputHeight)
        val master = DeliverablePreset.Master.apply(base.copy(outputHeight = 2160))
        assertEquals(2160, master.outputHeight)
        assertEquals(MovieHdrMode.KeepHdr, master.hdrMode)
        assertEquals(DeliverablePreset.Master, master.delivery.preset)
    }

    @Test
    fun delivery_encodeDecode_roundTrip() {
        val mt = MultiTrackTimeline.fromSingleVideo(
            listOf(TimelineClip("c0", 0, 0, 1500))
        ).copy(
            delivery = DeliveryOptions(
                burnInTc = true,
                preset = DeliverablePreset.Social,
                notes = listOf(ReviewNote("rn0", 400, "tighten hold"))
            ),
            markers = listOf(TimelineMarker("mk0", 800, MarkerKind.Beat, "clap"))
        )
        val back = MultiTrackTimeline.decode(mt.encode())!!
        assertTrue(back.delivery.burnInTc)
        assertFalse(back.delivery.watermark)
        assertEquals(DeliverablePreset.Social, back.delivery.preset)
        assertEquals(1, back.delivery.notes.size)
        assertEquals("tighten hold", back.delivery.notes[0].text)
        assertEquals(400L, back.delivery.notes[0].ms)
    }

    @Test
    fun edl_containsClipsAndMarkers() {
        val mt = MultiTrackTimeline.fromSingleVideo(
            listOf(
                TimelineClip("c0", 0, 0, 1000),
                TimelineClip("c1", 1, 0, 500)
            )
        ).copy(markers = listOf(TimelineMarker("m0", 200, MarkerKind.Music, "sting")))
        val edl = EdlLite.encode("Hero", mt, listOf("a.mp4", "b.mp4"))
        assertTrue(edl.contains("TITLE: Hero"))
        assertTrue(edl.contains("FROM CLIP NAME: a.mp4"))
        assertTrue(edl.contains("MARKER Music"))
    }

    @Test
    fun fcpxml_isWellFormedLite() {
        val mt = MultiTrackTimeline.fromSingleVideo(
            listOf(TimelineClip("c0", 0, 0, 1000))
        )
        val xml = FcpxmlLite.encode("Cut", mt, listOf("clip.mp4"))
        assertTrue(xml.contains("<fcpxml version=\"1.9\">"))
        assertTrue(xml.contains("asset-clip"))
        assertTrue(xml.contains("</fcpxml>"))
    }

    @Test
    fun handoff_writesFiles() {
        val dir = File(System.getProperty("java.io.tmpdir"), "pj-handoff-${System.nanoTime()}")
        assertTrue(dir.mkdirs())
        try {
            val mt = MultiTrackTimeline.fromSingleVideo(
                listOf(TimelineClip("c0", 0, 0, 800))
            ).copy(
                grade = TimelineGrade(lookKey = MovieLooks.WARM),
                delivery = DeliveryOptions(
                    notes = listOf(ReviewNote("rn0", 100, "note"))
                )
            )
            val arts = DeliveryHandoff.writeAll(dir, "Reel", mt, listOf("v.mp4"))
            assertTrue(arts.notesJson.exists() && arts.notesJson.length() > 10)
            assertTrue(arts.edl.exists())
            assertTrue(arts.fcpxml.exists())
            val meta = arts.metadataJson.readText()
            assertTrue(meta.contains("\"look\""))
            assertTrue(meta.contains(MovieLooks.WARM))
            assertTrue(meta.contains("\"watermark\": false"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
