package com.pixijoy.ml.pack

import com.pixijoy.ml.NeuralEngine
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.PackFeatureEntry
import com.pixijoy.ml.PackReadiness
import com.pixijoy.ml.interpreterBindAllowed
import org.junit.Assert.assertFalse
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
}
