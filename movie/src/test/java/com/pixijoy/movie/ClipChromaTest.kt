package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipChromaTest {
    @Test
    fun encodeDecode_roundTrip() {
        val chroma = ClipChroma(key = ChromaKey.Green, similarity = 0.35f, softness = 0.1f)
        val decoded = ClipChroma.decode(chroma.encode())
        assertEquals(ChromaKey.Green, decoded.key)
        assertEquals(0.35f, decoded.similarity, 0.01f)
        assertEquals(0.1f, decoded.softness, 0.01f)
        assertFalse(decoded.isIdentity)
        assertEquals("none", ClipChroma.None.encode())
        assertTrue(ClipChroma.decode(null).isIdentity)
        assertTrue(ClipChroma.decode("none").isIdentity)
    }

    @Test
    fun presets_andKeyRgb() {
        val green = ClipChroma.None.withKey(ChromaKey.Green)
        assertTrue(green.matchesKey(ChromaKey.Green))
        assertEquals(3, green.keyRgb()!!.size)
        assertEquals(
            ChromaSimilarityPreset.Wide,
            ChromaSimilarityPreset.matching(green.withSimilarity(0.6f))
        )
        assertTrue(green.withKey(ChromaKey.Off).isIdentity)
    }

    @Test
    fun timelineRoundTrip_chroma() {
        val clip = TimelineClip(
            id = "c0",
            mediaIndex = 0,
            inMs = 0,
            outMs = 2000,
            chroma = ClipChroma(key = ChromaKey.Blue, similarity = 0.5f, softness = 0.2f)
        )
        val decoded = Timeline.decode(Timeline(listOf(clip)).encode(), 1) { 5000 }
        assertTrue(decoded.clips[0].hasChroma)
        assertEquals(ChromaKey.Blue, decoded.clips[0].chroma.key)
        assertEquals(0.5f, decoded.clips[0].chroma.similarity, 0.01f)
    }

    @Test
    fun mtRoundTrip_chroma() {
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(
                clips = listOf(
                    TimelineClip(
                        id = "c0",
                        mediaIndex = 0,
                        inMs = 0,
                        outMs = 1500,
                        chroma = ClipChroma(key = ChromaKey.Green)
                    )
                )
            )
        )
        val decoded = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(ChromaKey.Green, decoded.tracks.first().clips.single().chroma.key)
    }
}
