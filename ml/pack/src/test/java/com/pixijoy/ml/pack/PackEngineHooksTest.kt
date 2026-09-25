package com.pixijoy.ml.pack

import org.junit.Assert.assertFalse
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
}
