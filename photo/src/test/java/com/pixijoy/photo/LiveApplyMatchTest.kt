package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Apply must bake the same picture the live slider already shows.
 * Hue is the grade matrix, not an HSV rotate. Vignette uses normalized
 * coordinates. Blur is the 5×5 shader mix, not stacked 3×3 box passes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class LiveApplyMatchTest {
    @Test
    fun hsl_zeroSaturation_isLumaGray_notHsvWhite() {
        val src = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.RED)
        val out = applyHsl(src, 0f, 0f)
        assertNotSame(src, out)
        val c = out.getPixel(0, 0)
        assertEquals(54, Color.red(c))
        assertEquals(54, Color.green(c))
        assertEquals(54, Color.blue(c))
        assertNotEquals(Color.WHITE, c)
    }

    @Test
    fun vignette_usesNormalizedFalloffOnWideFrame() {
        val src = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.WHITE)
        val out = applyVignette(src, 1f)
        val cornerMul = LiveGrade.vignetteMul(0f, 0f, 1f)
        assertEquals((255f * cornerMul).roundToInt(), Color.red(out.getPixel(0, 0)))
        assertEquals(255, Color.red(out.getPixel(4, 2)))
        val side = out.getPixel(7, 2)
        val sideMul = LiveGrade.vignetteMul(7f / 8f, 2f / 4f, 1f)
        assertEquals((255f * sideMul).roundToInt(), Color.red(side))
    }

    @Test
    fun blur_belowGate_copies_fullMixAveragesFiveByFive() {
        val src = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.BLACK)
        src.setPixel(4, 4, Color.WHITE)
        val skipped = applyBlurPasses(src, 0.4f)
        assertNotSame(src, skipped)
        assertEquals(Color.WHITE, skipped.getPixel(4, 4))
        val blurred = applyBlurPasses(src, 8f)
        assertEquals(10, Color.red(blurred.getPixel(4, 4)))
        assertEquals(Color.BLACK, blurred.getPixel(0, 0))
    }

    @Test
    fun histogram_exposureShiftsPeak_vignetteAddsDarkBins() {
        val flat = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        flat.eraseColor(Color.rgb(40, 40, 40))
        val plain = lumaHistogram(flat)
        val graded = lumaHistogramGraded(flat, LiveGrade.colorMatrix(1f, 1f, 0f, 1f), 0f)
        assertTrue(plain[40] > 0)
        assertEquals(0, graded[40])
        assertTrue(graded[80] > 0)

        val white = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        white.eraseColor(Color.WHITE)
        val vig = lumaHistogramGraded(white, LiveGrade.colorMatrix(0f, 1f, 0f, 1f), 1f)
        assertTrue(vig[255] > 0)
        assertTrue(vig.sum() > vig[255])
    }
}
