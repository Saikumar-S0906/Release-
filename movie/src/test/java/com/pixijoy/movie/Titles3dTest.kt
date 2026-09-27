package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Titles3dTest {
    @Test
    fun encodeDecode_roundTrip() {
        val t = TimelineTitle3d("t3", "Hello 3D", startMs = 0, endMs = 2000, depth = 0.7f)
        val back = TimelineTitle3d.decodeLine(t.encodeLine().split(','))!!
        assertEquals(t.id, back.id)
        assertEquals(t.text, back.text)
        assertEquals(t.endMs, back.endMs)
        assertEquals(0.7f, back.depth, 0.001f)
    }

    @Test
    fun timeline_persistsTitles3d() {
        val tl = Timeline(
            clips = listOf(TimelineClip("c0", 0, 0, 2000)),
            titles3d = listOf(TimelineTitle3d.default(2000))
        )
        assertTrue(tl.encode().contains("T3,"))
        val back = Timeline.decode(tl.encode(), 1) { 2000 }
        assertEquals(1, back.titles3d.size)
        assertEquals("3D Title", back.titles3d[0].text)
    }
}
