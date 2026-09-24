package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchPresetApplyTest {
    @Test
    fun missingPreset_keepsProjectGraph() {
        val graph = listOf(PhotoOp.Grade(0.2f, 1.1f)).toLayers().encodeLayers()
        val layers = BatchPresetApply.layersForExport(graph, preset = null)
        assertEquals(1, layers.size)
        val grade = layers.single().op as PhotoOp.Grade
        assertEquals(0.2f, grade.exposure, 0.001f)
    }

    @Test
    fun preset_replacesDestinationGraph() {
        val project = listOf(PhotoOp.Grade(0f, 1f)).toLayers().encodeLayers()
        val look = listOf(
            LayeredOp("L1", "Grade", op = PhotoOp.Grade(0.3f, 1.2f)),
            LayeredOp("L2", "HSL", op = PhotoOp.Hsl(12f, 1.1f))
        )
        val preset = PhotoPreset("p1", "Warm", look.encodeLayers())
        val applied = BatchPresetApply.layersForExport(project, preset)
        assertEquals(2, applied.size)
        assertEquals(0.3f, (applied[0].op as PhotoOp.Grade).exposure, 0.001f)
        assertTrue(applied[1].op is PhotoOp.Hsl)
        assertEquals(preset.layersEncoded, BatchPresetApply.appliedGraph(project, preset))
    }
}
