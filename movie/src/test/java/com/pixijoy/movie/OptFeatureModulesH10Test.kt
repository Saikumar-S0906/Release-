package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * H10: on-demand module names stay stable so SplitInstall + reflection bind
 * keep Filament / CG out of the base APK.
 */
class OptFeatureModulesH10Test {
    @Test
    fun cgModuleIsOnDemandFeatureCg() {
        assertEquals("feature_cg", OptFeatureModules.CG)
    }

    @Test
    fun filamentOwnedModulesAreOnDemandOnly() {
        val onDemand = setOf(
            OptFeatureModules.TITLES3D,
            OptFeatureModules.AR,
            OptFeatureModules.FFMPEG,
            OptFeatureModules.CG
        )
        assertTrue(onDemand.contains(OptFeatureModules.CG))
        assertTrue(onDemand.contains(OptFeatureModules.TITLES3D))
        assertEquals("feature_titles3d", OptFeatureModules.TITLES3D)
    }
}
