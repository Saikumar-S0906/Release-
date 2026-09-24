package com.pixijoy.photo

/**
 * Copy a saved still-edit onto other local photo projects. The preset graph
 * replaces the destination graph so batch export and reopen stay in sync.
 */
object BatchPresetApply {
    fun layersForExport(projectGraph: String, preset: PhotoPreset?): List<LayeredOp> {
        val fromPreset = preset?.layersEncoded?.decodeLayers().orEmpty()
        return fromPreset.ifEmpty { projectGraph.decodeLayers() }
    }

    fun appliedGraph(projectGraph: String, preset: PhotoPreset?): String {
        val layers = layersForExport(projectGraph, preset)
        return if (layers.isEmpty()) projectGraph else layers.encodeLayers()
    }
}
