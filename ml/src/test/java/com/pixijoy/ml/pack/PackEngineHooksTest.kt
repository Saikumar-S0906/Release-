package com.pixijoy.ml.pack

import com.pixijoy.ml.NeuralEngine
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.PackFeatureEntry
import com.pixijoy.ml.PackReadiness
import com.pixijoy.ml.interpreterBindAllowed
import com.pixijoy.ml.modelFileVerified
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackEngineHooksTest {
    @Test
    fun baseBuild_hasNoNeuralEngines() {
        PackEngineHooks.resetForTests()
        PackId.entries.forEach { id ->
            assertFalse(PackEngineHooks.hasEngine(id))
            assertFalse(PackRuntime.engineReady(id))
        }
    }

    @Test
    fun register_bindsOnlyWhenReadyAndChecksumMatches() {
        PackEngineHooks.resetForTests()
        val refused = PackEngineHooks(PackReadiness { false })
        refused.register(mapOf(PackId.Inpaint to NeuralEngine.Inpaint { _, _ -> null }))
        assertFalse(refused.isBound(PackId.Inpaint))
        assertFalse(PackEngineHooks.hasEngine(PackId.Inpaint))

        val ready = linkedSetOf(PackId.Inpaint)
        val accepted = PackEngineHooks(PackReadiness { it in ready })
        accepted.register(
            mapOf(
                PackId.Inpaint to NeuralEngine.Inpaint { _, _ -> null },
                PackId.Denoise to NeuralEngine.Denoise { _, _ -> null }
            )
        )
        assertTrue(accepted.isBound(PackId.Inpaint))
        assertFalse(accepted.isBound(PackId.Denoise))
        assertTrue(PackEngineHooks.hasEngine(PackId.Inpaint))
        assertFalse(PackEngineHooks.hasEngine(PackId.Denoise))
        PackEngineHooks.resetForTests()
        assertFalse(PackEngineHooks.hasEngine(PackId.Inpaint))
    }

    @Test
    fun featureEntry_registersAfterSplitAndLeavesBaseUnboundUntilThen() {
        PackEngineHooks.resetForTests()
        assertFalse(PackEngineHooks.hasEngine(PackId.Inpaint))
        val hooks = PackEngineHooks(PackReadiness { true })
        PackFeatureEntry.registerEngines(
            hooks,
            mapOf(PackId.SuperRes to NeuralEngine.SuperRes { _, _ -> null })
        )
        assertTrue(PackEngineHooks.hasEngine(PackId.SuperRes))
        assertFalse(PackEngineHooks.hasEngine(PackId.Inpaint))
    }

    @Test
    fun register_dropsEngineWhoseCapabilityDoesNotMatchTheKey() {
        val hooks = PackEngineHooks(PackReadiness { true })
        hooks.register(mapOf(PackId.Track to NeuralEngine.Inpaint { _, _ -> null }))
        assertFalse(hooks.isBound(PackId.Track))
        assertFalse(hooks.isBound(PackId.Inpaint))
    }

    @Test
    fun bindGate_requiresReadyStateAndMatchingSha() {
        val ready = PackInstallState.Ready(sha256 = "abc", version = "1")
        assertFalse(interpreterBindAllowed(PackInstallState.Missing, true, null))
        assertFalse(interpreterBindAllowed(PackInstallState.Failed("no"), true, null))
        assertFalse(interpreterBindAllowed(ready, fileMatchesRecordedSha = false, expectedSha256 = null))
        assertFalse(interpreterBindAllowed(ready, true, expectedSha256 = "def"))
        assertTrue(interpreterBindAllowed(ready, true, expectedSha256 = null))
        assertTrue(interpreterBindAllowed(ready, true, expectedSha256 = "ABC"))
    }

    @Test
    fun engineChip_requiresBoundEngineNotReadyAlone() {
        assertFalse(PackRuntime.isEngineReady(packReady = true, engineBound = false))
        assertFalse(PackRuntime.isEngineReady(packReady = false, engineBound = true))
        assertFalse(PackRuntime.isEngineReady(packReady = false, engineBound = false))
        assertTrue(PackRuntime.isEngineReady(packReady = true, engineBound = true))
    }

    @Test
    fun superResAndDenoise_readyFileIsNotNeural() {
        listOf(PackId.SuperRes, PackId.Denoise).forEach { id ->
            val spec = PackCatalog.spec(id)
            assertTrue(PackCatalog.isBundled(id))
            assertFalse(PackCatalog.hasHttpsFallback(spec))
            assertEquals(spec.expectedSha256, PackCatalog.spec(id).expectedSha256)
            assertFalse(PackRuntime.engineReady(id))
            assertEquals(
                SuperResDenoiseStatus.ReadyFileOnly,
                PackRuntime.superResDenoiseStatus(packReady = true, engineBound = false)
            )
            assertEquals(
                SuperResDenoiseStatus.EngineReady,
                PackRuntime.superResDenoiseStatus(packReady = true, engineBound = true)
            )
            assertEquals(
                SuperResDenoiseStatus.NotReady,
                PackRuntime.superResDenoiseStatus(packReady = false, engineBound = true)
            )
        }
    }

    @Test
    fun inpaint_readyFileIsNotNeural() {
        val spec = PackCatalog.spec(PackId.Inpaint)
        assertEquals(PackCatalog.INPAINT_DOWNLOAD_URL, spec.downloadUrl)
        assertEquals(PackCatalog.INPAINT_DOWNLOAD_SHA256, spec.downloadSha256)
        assertNull(spec.expectedSha256)
        assertNull(spec.assetPath)
        assertTrue(PackCatalog.hasHttpsFallback(spec))
        assertFalse(PackCatalog.isBundled(PackId.Inpaint))
        assertEquals("inpaint.tflite", spec.fileName)
        assertFalse(PackRuntime.engineReady(PackId.Inpaint))
        assertEquals(
            InpaintStatus.ReadyFileOnly,
            PackRuntime.inpaintStatus(packReady = true, engineBound = false)
        )
        assertEquals(
            InpaintStatus.EngineReady,
            PackRuntime.inpaintStatus(packReady = true, engineBound = true)
        )
        assertEquals(
            InpaintStatus.NotReady,
            PackRuntime.inpaintStatus(packReady = false, engineBound = true)
        )
        assertEquals(
            InpaintStatus.NotReady,
            PackRuntime.inpaintStatus(packReady = false, engineBound = false)
        )
        assertFalse(PackRuntime.inpaintNeuralApplyEnabled(packReady = true, engineBound = false))
        assertFalse(PackRuntime.inpaintNeuralApplyEnabled(packReady = false, engineBound = true))
        assertFalse(PackRuntime.inpaintNeuralApplyEnabled(packReady = false, engineBound = false))
        assertTrue(PackRuntime.inpaintNeuralApplyEnabled(packReady = true, engineBound = true))
    }

    @Test
    fun depth_readyFileIsNotNeural() {
        val spec = PackCatalog.spec(PackId.Depth)
        assertEquals(PackCatalog.DEPTH_DOWNLOAD_URL, spec.downloadUrl)
        assertEquals(PackCatalog.DEPTH_DOWNLOAD_SHA256, spec.downloadSha256)
        assertNull(spec.expectedSha256)
        assertNull(spec.assetPath)
        assertFalse(PackCatalog.isBundled(PackId.Depth))
        assertTrue(PackCatalog.hasHttpsFallback(spec))
        assertEquals("depth.tflite", spec.fileName)
        assertEquals("ml_depth", spec.playModule)
        assertEquals(
            DepthStatus.ReadyFileOnly,
            PackRuntime.depthStatus(packReady = true, engineBound = false)
        )
        assertEquals(
            DepthStatus.EngineReady,
            PackRuntime.depthStatus(packReady = true, engineBound = true)
        )
        assertEquals(
            DepthStatus.NotReady,
            PackRuntime.depthStatus(packReady = false, engineBound = true)
        )
        assertFalse(PackRuntime.engineReady(PackId.Depth))
    }

    @Test
    fun modelFile_mustMatchRecordedShaBeforeBind() {
        val file = File.createTempFile("pack-gate", ".bin")
        try {
            file.writeBytes(ByteArray(64) { 3 })
            val sha = PackChecksum.sha256(file)
            val ready = PackInstallState.Ready(sha256 = sha, version = "1")
            assertTrue(modelFileVerified(ready, file, expectedSha256 = null))
            assertTrue(modelFileVerified(ready, file, expectedSha256 = sha.uppercase()))
            assertFalse(modelFileVerified(ready, file, expectedSha256 = "ab".repeat(32)))
            assertFalse(modelFileVerified(PackInstallState.Ready(sha256 = "nope", version = "1"), file, null))
            assertFalse(modelFileVerified(PackInstallState.Missing, file, null))
            assertFalse(modelFileVerified(PackInstallState.Ready(sha256 = "", version = "1"), file, null))
            file.writeBytes(ByteArray(64) { 9 })
            assertFalse(modelFileVerified(ready, file, null))
        } finally {
            file.delete()
        }
    }
}
