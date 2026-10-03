package com.pixijoy.core.data

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportFileOpsTest {
    @Test
    fun replaceFile_swapsInNewBytesAndRemovesTemp() {
        val dir = Files.createTempDirectory("pixijoy-import").toFile()
        try {
            val dest = java.io.File(dir, "look.cube").apply { writeText("old-lut") }
            val tmp = java.io.File(dir, ".look.cube.part").apply { writeText("new-lut-ok") }
            assertTrue(replaceFile(tmp, dest))
            assertEquals("new-lut-ok", dest.readText())
            assertFalse(tmp.exists())
            assertFalse(java.io.File(dir, ".look.cube.bak").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun replaceFile_keepsOriginalWhenIncomingIsMissing() {
        val dir = Files.createTempDirectory("pixijoy-import").toFile()
        try {
            val dest = java.io.File(dir, "look.cube").apply { writeText("old-lut") }
            val tmp = java.io.File(dir, ".look.cube.part")
            assertFalse(replaceFile(tmp, dest))
            assertEquals("old-lut", dest.readText())
            assertFalse(java.io.File(dir, ".look.cube.bak").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun uniquePropFile_skipsNamesThatAlreadyExist() {
        val dir = Files.createTempDirectory("pixijoy-prop").toFile()
        try {
            java.io.File(dir, "hat.glb").writeText("a")
            java.io.File(dir, "hat_2.glb").writeText("b")
            val next = uniquePropFile(dir, "hat.glb")
            assertEquals("hat_3.glb", next.name)
            assertFalse(next.exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun uniquePropFile_doesNotReturnAnExistingFileAfterNinetyNine() {
        val dir = Files.createTempDirectory("pixijoy-prop").toFile()
        try {
            java.io.File(dir, "hat.glb").writeText("a")
            for (i in 2 until 100) {
                java.io.File(dir, "hat_$i.glb").writeText("x")
            }
            val next = uniquePropFile(dir, "hat.glb")
            assertFalse(next.exists())
            assertTrue(next.name.startsWith("hat_"))
            assertTrue(next.name.endsWith(".glb"))
            assertFalse(next.name == "hat_99.glb")
        } finally {
            dir.deleteRecursively()
        }
    }
}
