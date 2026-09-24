package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StudioCapabilityTest {
    @Test
    fun neuralFrameInterp_isUnavailableInBaseBuild() {
        FrameInterp.mlAvailableOverride = null
        assertFalse(FrameInterp.mlAvailable())
        assertEquals(SlowMoEngine.Heuristic, FrameInterp.resolve(SlowMoEngine.Ml))
    }

    @Test
    fun multiInputPreview_disablesSeeking() {
        assertFalse(
            CompositionPreviewPolicy.supportsSeeking(CompositionPreviewGraphMode.MultipleInput)
        )
    }
}
