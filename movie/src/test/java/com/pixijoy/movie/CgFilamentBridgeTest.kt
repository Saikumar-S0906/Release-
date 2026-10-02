package com.pixijoy.movie

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CgFilamentBridgeTest {
    @Test
    fun propPath_usesFilamentOnlyForBoundGltfFile() {
        assertEquals(
            CgPropDrawPath.Filament,
            cgPropDrawPath(CgPreset.PropGltf, filamentPropBound = true, hasPropFile = true)
        )
        assertEquals(
            CgPropDrawPath.Classical,
            cgPropDrawPath(CgPreset.PropGltf, filamentPropBound = true, hasPropFile = false)
        )
        assertEquals(
            CgPropDrawPath.Classical,
            cgPropDrawPath(CgPreset.PropGltf, filamentPropBound = false, hasPropFile = true)
        )
        assertEquals(
            CgPropDrawPath.Classical,
            cgPropDrawPath(CgPreset.DustTrail, filamentPropBound = true, hasPropFile = true)
        )
        assertEquals(
            CgPropDrawPath.Classical,
            cgPropDrawPath(CgPreset.PropCube, filamentPropBound = true, hasPropFile = false)
        )
    }

    @Test
    fun filamentFailure_fallsBackToClassical() {
        val drawn = cgRenderPropOrClassical(
            path = CgPropDrawPath.Filament,
            filament = { error("filament unavailable") },
            classical = { "classical" }
        )
        assertEquals("classical", drawn)
        val skipped = cgRenderPropOrClassical(
            path = CgPropDrawPath.Classical,
            filament = { "filament" },
            classical = { "classical" }
        )
        assertEquals("classical", skipped)
    }

    @Test
    fun host_reportsFilamentOnlyWhenPropRendererIsBound() {
        val previous = CgHost.renderer
        try {
            CgHost.renderer = null
            assertFalse(CgHost.filamentAvailable())
            CgHost.renderer = object : FilamentPropCgRenderer {
                override fun renderPlate(request: CgPlateRequest): Bitmap = error("unused")
            }
            assertTrue(CgHost.filamentAvailable())
        } finally {
            CgHost.renderer = previous
        }
    }

    @Test
    fun movieClasspath_doesNotContainFilament() {
        val missing = runCatching { Class.forName("com.google.android.filament.Engine") }.exceptionOrNull()
        assertTrue(missing is ClassNotFoundException)
    }

    @Test
    fun propResourceUri_staysInsidePropsDir() {
        val root = File("build/cg-prop-uri-test").apply {
            deleteRecursively()
            mkdirs()
        }
        val props = File(root, CG_PROP_DIR).apply { mkdirs() }
        val prop = File(props, "chair.glb").apply { writeBytes(byteArrayOf(1)) }
        val texture = File(props, "albedo.png").apply { writeBytes(byteArrayOf(2)) }
        val nestedDir = File(props, "textures").apply { mkdirs() }
        val nested = File(nestedDir, "a.png").apply { writeBytes(byteArrayOf(3)) }
        assertEquals(texture.canonicalFile, cgPropResourceFile(prop, "albedo.png")?.canonicalFile)
        assertEquals(nested.canonicalFile, cgPropResourceFile(prop, "textures/a.png")?.canonicalFile)
        assertNull(cgPropResourceFile(prop, "../secret.bin"))
        assertNull(cgPropResourceFile(prop, "/etc/passwd"))
        assertNull(cgPropResourceFile(prop, "C:secret.bin"))
        assertEquals(prop.canonicalFile, cgPropFile(root, "chair.glb")?.canonicalFile)
        assertNull(cgPropFile(root, "../chair.glb"))
    }

    @Test
    fun filamentTransform_isColumnMajorYawAndCentersMesh() {
        val unit = cgFilamentPropTransform(0f, 0f, 1f, floatArrayOf(0f, 0f, 0f), floatArrayOf(0.5f, 0.5f, 0.5f))
        assertEquals(1f, unit[0], 0.001f)
        assertEquals(1f, unit[5], 0.001f)
        assertEquals(1f, unit[10], 0.001f)
        assertEquals(1f, unit[15], 0.001f)
        val yaw90 = cgFilamentPropTransform(90f, 0f, 1f, floatArrayOf(0f, 0f, 0f), floatArrayOf(0.5f, 0.5f, 0.5f))
        assertEquals(0f, yaw90[0], 0.02f)
        assertEquals(-1f, yaw90[2], 0.02f)
        assertEquals(1f, yaw90[5], 0.02f)
        val shifted = cgFilamentPropTransform(0f, 0f, 1f, floatArrayOf(2f, 0f, 0f), floatArrayOf(0.5f, 0.5f, 0.5f))
        assertEquals(-2f, shifted[12], 0.001f)
        assertTrue(cgFilamentCameraDistance() in 1.5..1.8)
    }
}
