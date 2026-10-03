package com.pixijoy.movie

import android.graphics.Bitmap
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Classical plate pixels. A pass means the four presets are different pictures
 * and a knob the renderer ignores does not change pixels. It is not a taste
 * grade, not Filament, and not a glTF load.
 *
 * Legacy Robolectric canvas rasterizes gradient rects and skips path, line,
 * and circle draws, so cube yaw and dust yaw stay bitwise identical. Native
 * graphics uses Skia. The floor is unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CgPlateQualityTest {
    @Test
    fun fourPresets_areDifferentPictures() {
        val cube = plate(CgPreset.PropCube)
        val ground = plate(CgPreset.GroundPlane)
        val sky = plate(CgPreset.SkyDome)
        val dust = plate(CgPreset.DustTrail, progress = 0.5f)
        val pairs = listOf(
            "cube-ground" to meanAbs(cube, ground),
            "cube-sky" to meanAbs(cube, sky),
            "cube-dust" to meanAbs(cube, dust),
            "ground-sky" to meanAbs(ground, sky),
            "ground-dust" to meanAbs(ground, dust),
            "sky-dust" to meanAbs(sky, dust)
        )
        for ((name, diff) in pairs) {
            assertTrue("$name diff=$diff", diff > FLOOR)
        }
    }

    @Test
    fun sky_ignoresScaleYawAndPitch() {
        val base = plate(CgPreset.SkyDome, scale = 1f, yaw = 0f, pitch = 0f)
        assertEquals(0.0, meanAbs(base, plate(CgPreset.SkyDome, scale = 2.2f)), 0.0)
        assertEquals(0.0, meanAbs(base, plate(CgPreset.SkyDome, yaw = 80f)), 0.0)
        assertEquals(0.0, meanAbs(base, plate(CgPreset.SkyDome, pitch = 30f)), 0.0)
    }

    @Test
    fun ground_ignoresYawAndPitch_andScaleChangesIt() {
        val base = plate(CgPreset.GroundPlane, scale = 1f, yaw = 0f, pitch = 0f)
        assertEquals(0.0, meanAbs(base, plate(CgPreset.GroundPlane, yaw = 90f)), 0.0)
        assertEquals(0.0, meanAbs(base, plate(CgPreset.GroundPlane, pitch = 30f)), 0.0)
        val scaled = meanAbs(base, plate(CgPreset.GroundPlane, scale = 2.4f))
        assertTrue("ground scale diff=$scaled", scaled > 0.5)
    }

    @Test
    fun dust_ignoresPitch_andYawAndProgressChangeIt() {
        val base = plate(CgPreset.DustTrail, yaw = 0f, pitch = 0f, progress = 0.2f)
        // plate() defaults yaw to 20. Pitch and progress checks must keep yaw fixed
        // or they measure a yaw change and call it something else.
        assertEquals(
            0.0,
            meanAbs(base, plate(CgPreset.DustTrail, yaw = 0f, pitch = 40f, progress = 0.2f)),
            0.0
        )
        val yawed = meanAbs(base, plate(CgPreset.DustTrail, yaw = 90f, pitch = 0f, progress = 0.2f))
        val later = meanAbs(base, plate(CgPreset.DustTrail, yaw = 0f, pitch = 0f, progress = 0.8f))
        assertTrue("dust yaw diff=$yawed", yawed > 0.05)
        assertTrue("dust progress diff=$later", later > 0.05)
    }

    @Test
    fun cube_yawAndPitchChangeIt() {
        // At scale 1 the projected cube is a small part of the frame (perspective
        // shrinks it), so a real yaw change stays under the full-frame floor.
        // Scale 2.5 is the product maximum and still uses the same raster path.
        val base = plate(CgPreset.PropCube, scale = 2.5f, yaw = 10f, pitch = 0f)
        val yawed = meanAbs(base, plate(CgPreset.PropCube, scale = 2.5f, yaw = 70f, pitch = 0f))
        val pitched = meanAbs(base, plate(CgPreset.PropCube, scale = 2.5f, yaw = 10f, pitch = 35f))
        val yawAtUnit = meanAbs(
            plate(CgPreset.PropCube, scale = 1f, yaw = 10f, pitch = 0f),
            plate(CgPreset.PropCube, scale = 1f, yaw = 70f, pitch = 0f)
        )
        assertTrue("cube yaw diff=$yawed unit=$yawAtUnit", yawed > FLOOR && yawAtUnit > 1.0)
        assertTrue("cube pitch diff=$pitched", pitched > FLOOR)
    }

    private fun plate(
        preset: CgPreset,
        scale: Float = 1f,
        yaw: Float = 20f,
        pitch: Float = 0f,
        progress: Float = 0f
    ): Bitmap = ClassicalCgRenderer.renderFull(
        preset, WIDTH, HEIGHT, scale, yaw, pitch, 0f, 0f, null, progress
    )

    private fun meanAbs(a: Bitmap, b: Bitmap): Double {
        val pa = pixels(a)
        val pb = pixels(b)
        var sum = 0L
        for (i in pa.indices) {
            sum += abs(((pa[i] shr 16) and 0xff) - ((pb[i] shr 16) and 0xff))
            sum += abs(((pa[i] shr 8) and 0xff) - ((pb[i] shr 8) and 0xff))
            sum += abs((pa[i] and 0xff) - (pb[i] and 0xff))
            sum += abs(((pa[i] ushr 24) and 0xff) - ((pb[i] ushr 24) and 0xff))
        }
        return sum.toDouble() / (pa.size * 4)
    }

    private fun pixels(bmp: Bitmap): IntArray {
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        return px
    }

    private companion object {
        const val WIDTH = 160
        const val HEIGHT = 120
        const val FLOOR = 4.0
    }
}
