package com.pixijoy.movie

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StudioCapabilityTest {
    @After
    fun tearDown() {
        FrameInterp.mlAvailableOverride = null
        TrackFacade.mlAvailableOverride = null
    }
    @Test
    fun neuralFrameInterp_isUnavailableInBaseBuild() {
        FrameInterp.mlAvailableOverride = null
        assertFalse(FrameInterp.mlAvailable())
        assertEquals(SlowMoEngine.Heuristic, FrameInterp.resolve(SlowMoEngine.Ml))
    }

    @Test
    fun tracking_isUnavailableInBaseBuild() {
        TrackFacade.mlAvailableOverride = null
        assertFalse(TrackFacade.mlAvailable())
        assertEquals(TrackEngine.Off, TrackFacade.resolve(TrackEngine.Ml))
    }

    @Test
    fun multiInputPreview_disablesSeeking() {
        assertFalse(
            CompositionPreviewPolicy.supportsSeeking(CompositionPreviewGraphMode.MultipleInput)
        )
    }
}
