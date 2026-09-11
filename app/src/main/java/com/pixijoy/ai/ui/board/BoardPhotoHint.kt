package com.pixijoy.ai.ui.board

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.pixijoy.ai.R

/**
 * Soft, on-device photo hint — old scan → Restore, busy product shot → Cutout.
 * Not a marketplace, not ML Kit. Cheap pixel stats only.
 */
enum class BoardPhotoHint(
    val openScreenId: String,
    @StringRes val bodyRes: Int,
    @StringRes val ctaRes: Int
) {
    Restore("b_c05", R.string.board_photo_hint_restore, R.string.create_extra_restore),
    Cutout("b_c04", R.string.board_photo_hint_cutout, R.string.board_f3_job_cutout);

    companion object {
        fun from(bitmap: Bitmap): BoardPhotoHint? {
            if (bitmap.isRecycled || bitmap.width < 8 || bitmap.height < 8) return null
            val w = bitmap.width
            val h = bitmap.height
            val stepX = (w / 24).coerceAtLeast(1)
            val stepY = (h / 24).coerceAtLeast(1)
            var n = 0
            var satSum = 0.0
            var lumaSum = 0.0
            var lumaMin = 1.0
            var lumaMax = 0.0
            var yellow = 0
            var edgeSum = 0.0
            var edgeN = 0
            var y = 0
            while (y < h) {
                var x = 0
                while (x < w) {
                    val c = bitmap.getPixel(x, y)
                    val r = (c shr 16 and 0xFF) / 255.0
                    val g = (c shr 8 and 0xFF) / 255.0
                    val b = (c and 0xFF) / 255.0
                    val mx = maxOf(r, g, b)
                    val mn = minOf(r, g, b)
                    val sat = if (mx < 0.001) 0.0 else (mx - mn) / mx
                    val luma = 0.299 * r + 0.587 * g + 0.114 * b
                    satSum += sat
                    lumaSum += luma
                    if (luma < lumaMin) lumaMin = luma
                    if (luma > lumaMax) lumaMax = luma
                    if (r > g && g >= b && (r - b) > 0.08 && sat < 0.45) yellow++
                    if (x + stepX < w && y + stepY < h) {
                        val n1 = bitmap.getPixel(x + stepX, y)
                        val n2 = bitmap.getPixel(x, y + stepY)
                        edgeSum += channelDelta(c, n1) + channelDelta(c, n2)
                        edgeN++
                    }
                    n++
                    x += stepX
                }
                y += stepY
            }
            if (n == 0) return null
            val meanSat = satSum / n
            val contrast = lumaMax - lumaMin
            val yellowRatio = yellow.toDouble() / n
            val meanEdge = if (edgeN == 0) 0.0 else edgeSum / edgeN
            val faded = meanSat < 0.22 && contrast < 0.45 &&
                (yellowRatio > 0.18 || meanSat < 0.12)
            val busy = meanEdge > 0.12 && meanSat > 0.18 && contrast > 0.28
            return when {
                faded -> Restore
                busy -> Cutout
                else -> null
            }
        }

        private fun channelDelta(a: Int, b: Int): Double {
            val dr = ((a shr 16 and 0xFF) - (b shr 16 and 0xFF)).toDouble()
            val dg = ((a shr 8 and 0xFF) - (b shr 8 and 0xFF)).toDouble()
            val db = ((a and 0xFF) - (b and 0xFF)).toDouble()
            return (kotlin.math.abs(dr) + kotlin.math.abs(dg) + kotlin.math.abs(db)) / (3.0 * 255.0)
        }
    }
}
