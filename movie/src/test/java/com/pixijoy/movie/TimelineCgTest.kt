package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineCgTest {
    @Test
    fun encodeDecode_roundTrip() {
        val layer = TimelineCgLayer(
            id = "cg1",
            preset = CgPreset.PropCube,
            startMs = 100,
            endMs = 2400,
            x = 0.1f,
            y = -0.2f,
            scale = 1.25f,
            yaw = 40f,
            zOrder = 1,
            followTrack = true,
            matte = CgMatte.SoftOval,
            pitch = 12f,
            endYaw = 80f,
            endScale = 1.6f
        )
        val decoded = TimelineCgLayer.decodeLine(layer.encodeLine().split(','))!!
        assertEquals(layer.id, decoded.id)
        assertEquals(layer.preset, decoded.preset)
        assertEquals(layer.startMs, decoded.startMs)
        assertEquals(layer.endMs, decoded.endMs)
        assertEquals(layer.followTrack, decoded.followTrack)
        assertEquals(layer.zOrder, decoded.zOrder)
        assertEquals(layer.scale, decoded.scale, 0.001f)
        assertEquals(layer.yaw, decoded.yaw, 0.001f)
        assertEquals(CgMatte.SoftOval, decoded.matte)
        assertEquals(12f, decoded.pitch, 0.001f)
        assertEquals(80f, decoded.endYaw, 0.001f)
        assertEquals(1.6f, decoded.endScale, 0.001f)
        assertEquals("", decoded.propFile)
    }

    @Test
    fun propFile_roundTrip_andRejectsPathEscape() {
        val layer = TimelineCgLayer(
            id = "cg2",
            preset = CgPreset.PropGltf,
            endMs = 1500,
            x = 0.2f,
            y = -0.1f,
            scale = 1.4f,
            propFile = "chair.glb"
        )
        val decoded = TimelineCgLayer.decodeLine(layer.encodeLine().split(','))!!
        assertEquals(CgPreset.PropGltf, decoded.preset)
        assertEquals("chair.glb", decoded.propFile)
        assertEquals(1.4f, decoded.scale, 0.001f)
        assertEquals(0.2f, decoded.x, 0.001f)
        val escaped = "CG,cg0,PropGltf,0,2000,0,0,1,0,0,0,None,0,nan,nan,../a.glb"
        val blocked = TimelineCgLayer.decodeLine(escaped.split(','))!!
        assertEquals("", blocked.propFile)
    }

    @Test
    fun legacyDecode_withoutMatteFields() {
        val legacy = "CG,cg0,PropCube,0,2000,0,0,1.1,28,0,0"
        val decoded = TimelineCgLayer.decodeLine(legacy.split(','))!!
        assertEquals(CgMatte.None, decoded.matte)
        assertTrue(decoded.endYaw.isNaN())
        assertEquals("", decoded.propFile)
    }

    @Test
    fun cameraKeys_interpolate() {
        val layer = TimelineCgLayer(
            id = "cg0",
            yaw = 0f,
            endYaw = 90f,
            scale = 1f,
            endScale = 2f,
            endMs = 1000
        )
        assertEquals(0f, layer.yawAt(0, 1000), 0.01f)
        assertEquals(45f, layer.yawAt(500, 1000), 0.01f)
        assertEquals(1.5f, layer.scaleAt(500, 1000), 0.01f)
    }

    @Test
    fun zOrder_sortsOverlays() {
        val layers = listOf(
            TimelineCgLayer("a", CgPreset.PropCube, zOrder = 2),
            TimelineCgLayer("b", CgPreset.SkyDome, zOrder = -1),
            TimelineCgLayer("c", CgPreset.GroundPlane, zOrder = 0)
        )
        val sorted = layers.sortedBy { it.zOrder }.map { it.id }
        assertEquals(listOf("b", "c", "a"), sorted)
    }

    @Test
    fun multiTrack_preservesCg() {
        val mt = MultiTrackTimeline.fromSingleVideo(
            listOf(TimelineClip(id = "c0", mediaIndex = 0, inMs = 0, outMs = 2000))
        ).copy(
            cg = listOf(
                TimelineCgLayer.propDefault(2000).copy(
                    matte = CgMatte.ChromaEdge,
                    followTrack = true,
                    zOrder = -1
                )
            )
        )
        val back = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(1, back.cg.size)
        assertEquals(CgPreset.PropCube, back.cg.first().preset)
        assertEquals(CgMatte.ChromaEdge, back.cg.first().matte)
        assertTrue(back.cg.first().followTrack)
        assertFalse(back.toFlatTimeline().cg.isEmpty())
    }
}
