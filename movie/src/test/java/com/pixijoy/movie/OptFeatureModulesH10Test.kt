package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * H10: on-demand module names stay stable so SplitInstall + reflection bind
 * keep Filament / CG out of the base APK, and feature manifests stay on-demand.
 */
class OptFeatureModulesH10Test {
    @Test
    fun cgModuleIsOnDemandFeatureCg() {
        assertEquals("feature_cg", OptFeatureModules.CG)
    }

    @Test
    fun titles3dModuleNameIsStable() {
        assertEquals("feature_titles3d", OptFeatureModules.TITLES3D)
    }

    @Test
    fun filamentOwnedModulesDeclareOnDemandDelivery() {
        val root = repoRoot()
        for (module in listOf(OptFeatureModules.CG, OptFeatureModules.TITLES3D)) {
            val manifest = File(root, "$module/src/main/AndroidManifest.xml")
            assertTrue("missing manifest for $module at ${manifest.path}", manifest.isFile)
            val text = manifest.readText()
            assertTrue(
                "$module must declare <dist:on-demand> delivery",
                text.contains("<dist:on-demand")
            )
            assertFalse(
                "$module must not use <dist:install-time> (H10 base-APK contract)",
                text.contains("<dist:install-time")
            )
        }
    }

    private fun repoRoot(): File =
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
}
