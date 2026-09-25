package com.pixijoy.ml

import com.pixijoy.ml.pack.PackId

/**
 * Play feature-module entry. Call only after [PackSplits.install], from the
 * module that owns the model files.
 *
 * The base [android.app.Application] does not call this and does not register
 * interpreters. Missing models stay on Telea, bicubic, and frame-hold.
 */
object PackFeatureEntry {
    fun registerEngines(hooks: PackEngineHooks, engines: Map<PackId, NeuralEngine>) {
        hooks.register(engines)
    }

    /**
     * After [PackSplits.install]. Registers an interpreter only when [PackLiteRt]
     * accepts a real model file. A removed pack releases that capability.
     * [android.app.Application] does not call this.
     */
    fun watchVerifiedModels(context: android.content.Context, hooks: PackEngineHooks) {
        PackLiteRt.watch(context) { id, engine ->
            if (engine == null) hooks.release(id) else registerEngines(hooks, mapOf(id to engine))
        }
    }
}
