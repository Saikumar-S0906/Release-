package com.pixijoy.studio.ui.session

import com.pixijoy.studio.R
import com.pixijoy.movie.CgPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CgPresetControlsTest {
    @Test
    fun ground_hidesYawAndPitch() {
        val options = cgOptions(CgPreset.GroundPlane)
        assertTrue(CgOption.Scale in options)
        assertTrue(CgOption.Place in options)
        assertFalse(CgOption.Yaw in options)
        assertFalse(CgOption.Pitch in options)
        assertEquals(R.string.studio_cg_horizon, cgScaleLabelRes(CgPreset.GroundPlane))
        assertEquals(R.string.studio_cg_ground_note, cgNoteRes(CgPreset.GroundPlane))
    }

    @Test
    fun dust_usesDriftAndHidesPitch() {
        val options = cgOptions(CgPreset.DustTrail)
        assertTrue(CgOption.Scale in options)
        assertTrue(CgOption.Yaw in options)
        assertFalse(CgOption.Pitch in options)
        assertEquals(R.string.studio_cg_amount, cgScaleLabelRes(CgPreset.DustTrail))
        assertEquals(R.string.studio_cg_drift, cgYawLabelRes(CgPreset.DustTrail))
        assertEquals(R.string.studio_cg_dust_note, cgNoteRes(CgPreset.DustTrail))
    }

    @Test
    fun sky_isZOrderAndMatteOnly() {
        assertEquals(setOf(CgOption.Matte, CgOption.Z), cgOptions(CgPreset.SkyDome))
        assertEquals(R.string.studio_cg_sky_note, cgNoteRes(CgPreset.SkyDome))
    }

    @Test
    fun cubeAndMesh_keepTurnAndPitch() {
        assertTrue(CgOption.Pitch in cgOptions(CgPreset.PropCube))
        assertTrue(CgOption.Yaw in cgOptions(CgPreset.PropCube))
        assertEquals(cgOptions(CgPreset.PropCube), cgOptions(CgPreset.PropGltf))
        assertEquals(R.string.studio_cg_scale, cgScaleLabelRes(CgPreset.PropCube))
        assertEquals(R.string.studio_cg_yaw, cgYawLabelRes(CgPreset.PropCube))
    }
}
