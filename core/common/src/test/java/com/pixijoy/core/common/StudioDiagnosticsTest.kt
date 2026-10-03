package com.pixijoy.core.common

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioDiagnosticsTest {
    @Test
    fun note_stripsPaths_andKeepsAShortLine() {
        StudioDiagnostics.clearForTest()
        StudioDiagnostics.note(
            "export",
            "Could not read C:\\Users\\andro\\Videos\\clip.mp4"
        )
        val line = StudioDiagnostics.latest()
        assertEquals("export: Could not read [path]", line)
        assertTrue(line!!.length < 180)
        StudioDiagnostics.clearForTest()
    }

    @Test
    fun ring_dropsOldest_andReloadsFromTheFile() {
        StudioDiagnostics.clearForTest()
        val dir = File("build/studio-diagnostics-test").apply {
            deleteRecursively()
            mkdirs()
        }
        StudioDiagnostics.bind(dir)
        repeat(30) { index ->
            StudioDiagnostics.note("preview", "fallback $index")
        }
        assertEquals(24, StudioDiagnostics.snapshot().size)
        assertTrue(StudioDiagnostics.latest()!!.endsWith("fallback 29"))
        StudioDiagnostics.clearForTest()
        assertNull(StudioDiagnostics.latest())
        StudioDiagnostics.bind(dir)
        assertEquals(24, StudioDiagnostics.snapshot().size)
        assertTrue(StudioDiagnostics.latest()!!.startsWith("preview:"))
        assertTrue(!StudioDiagnostics.latest()!!.contains("fallback 0"))
        StudioDiagnostics.clearForTest()
    }
}