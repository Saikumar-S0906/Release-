package com.pixijoy.photo

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pixel checks for the CPU retouch paths. A passing test means the mask,
 * the clone, or the heuristic matte changed the pixels it claims to change.
 * It does not grade the fill as seamless and it does not call the result neural.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RetouchQualityTest {
    @Test
    fun fill_emptyStamp_isUnchangedInstance() {
        val src = solid(16, 16, SURROUND)
        assertSame(src, applyFill(src, emptyList()))
    }

    @Test
    fun fill_changesHole_leavesOutside_andNeuralMissMatchesTelea() {
        val src = solid(48, 48, SURROUND)
        paintDisk(src, 24, 24, 6, HOLE)
        val stamp = Stamp(0.5f, 0.5f, 0.12f)
        val telea = applyFill(src, listOf(stamp), allowNeural = false)
        val neuralMiss = applyFill(src, listOf(stamp), allowNeural = true)
        val mask = buildFillMask(src.width, src.height, listOf(stamp))
        assertTrue(mask[24 * 48 + 24])
        assertEquals(HOLE, at(src, 24, 24))
        assertNotEquals(HOLE, at(telea, 24, 24))
        assertTrue(channelDistance(at(telea, 24, 24), SURROUND) < channelDistance(HOLE, SURROUND) / 2)
        assertUnchangedOutside(src, telea, mask)
        assertEquals(pixels(telea).toList(), pixels(neuralMiss).toList())
    }

    @Test
    fun heal_cloneCopiesSource_ringChangesDisk_farPixelsStay() {
        val src = solid(64, 64, SURROUND)
        paintDisk(src, 10, 10, 6, HOLE)
        val clone = applyHeal(
            src,
            listOf(Stamp(0.7f, 0.7f, 0.08f, sx = 10f / 64f, sy = 10f / 64f))
        )
        val dx = 45
        val dy = 45
        assertTrue(channelDistance(at(clone, dx, dy), HOLE) < 40)
        assertEquals(SURROUND, at(clone, 0, 0))

        val blot = solid(64, 64, SURROUND)
        paintDisk(blot, 32, 32, 8, HOLE)
        val ring = applyHeal(blot, listOf(Stamp(0.5f, 0.5f, 0.1f)))
        assertNotEquals(HOLE, at(ring, 32, 32))
        assertTrue(channelDistance(at(ring, 32, 32), SURROUND) < channelDistance(HOLE, SURROUND))
        assertEquals(SURROUND, at(ring, 0, 0))
    }

    @Test
    fun skyHeuristic_movesTopTowardPreset_andLeavesBottom() {
        val w = 32
        val h = 32
        val src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h) { i -> if (i / w < h / 3) SKY else GROUND }
        src.setPixels(px, 0, w, 0, 0, w, h)
        val out = applySkyReplace(src, SkyReplaceTarget.Sky, SkyReplacePresets.BlueSky)
        val topBefore = channelDistance(at(src, 16, 2), SkyReplacePresets.BlueSky)
        val topAfter = channelDistance(at(out, 16, 2), SkyReplacePresets.BlueSky)
        assertTrue("top moved toward preset: before=$topBefore after=$topAfter", topAfter < topBefore)
        assertTrue(channelDistance(at(out, 16, 30), at(src, 16, 30)) <= 12)
    }

    private fun assertUnchangedOutside(src: Bitmap, out: Bitmap, mask: BooleanArray) {
        val w = src.width
        val h = src.height
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (mask[y * w + x]) continue
                assertEquals("outside $x,$y", at(src, x, y), at(out, x, y))
            }
        }
    }

    private fun solid(w: Int, h: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(IntArray(w * h) { color }, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun paintDisk(bmp: Bitmap, cx: Int, cy: Int, radius: Int, color: Int) {
        val w = bmp.width
        val h = bmp.height
        val px = pixels(bmp)
        for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(h - 1)) {
            for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(w - 1)) {
                if (hypot((x - cx).toDouble(), (y - cy).toDouble()) <= radius) {
                    px[y * w + x] = color
                }
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
    }

    private fun pixels(bmp: Bitmap): IntArray {
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        return px
    }

    private fun at(bmp: Bitmap, x: Int, y: Int): Int = pixels(bmp)[y * bmp.width + x]

    private fun channelDistance(a: Int, b: Int): Int {
        return abs(((a shr 16) and 0xff) - ((b shr 16) and 0xff)) +
            abs(((a shr 8) and 0xff) - ((b shr 8) and 0xff)) +
            abs((a and 0xff) - (b and 0xff))
    }

    private companion object {
        const val SURROUND = 0xFF2060C0.toInt()
        const val HOLE = 0xFFFF2020.toInt()
        const val SKY = 0xFF78B4E6.toInt()
        const val GROUND = 0xFF103010.toInt()
    }
}
