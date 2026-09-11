package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.pixijoy.ai.util.BitmapUtils
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class BackgroundRemover @Inject constructor() {
    private val segmenter = Segmentation.getClient(
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
    )

    suspend fun removeBackground(
        bitmap: Bitmap,
        params: MatteRefineParams = MatteRefineParams()
    ): Bitmap {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val mask = segmenter.process(inputImage).await()
        return applyMask(bitmap, mask, params)
    }

    suspend fun extractMask(bitmap: Bitmap): Bitmap {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val mask = segmenter.process(inputImage).await()
        return maskToBitmap(mask)
    }

    /**
     * Re-refine an existing alpha cutout without re-running ML Kit.
     * Uses RGB from [cutout] and rebuilds alpha from current alpha + [params].
     */
    fun refineCutoutAlpha(cutout: Bitmap, params: MatteRefineParams): Bitmap {
        if (cutout.isRecycled || cutout.width <= 0 || cutout.height <= 0) return cutout
        val w = cutout.width
        val h = cutout.height
        val pixels = IntArray(w * h)
        cutout.getPixels(pixels, 0, w, 0, 0, w, h)
        val alphaMask = IntArray(w * h) { i ->
            val a = (pixels[i] ushr 24) and 0xFF
            Color.argb(a, 255, 255, 255)
        }
        var maskBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        maskBmp.setPixels(alphaMask, 0, w, 0, 0, w, h)
        maskBmp = refineMask(maskBmp, params)
        val rgbSource = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val opaque = IntArray(w * h) { i -> (pixels[i] and 0x00FFFFFF) or (0xFF shl 24) }
        rgbSource.setPixels(opaque, 0, w, 0, 0, w, h)
        val out = BitmapUtils.applyAlphaMask(rgbSource, maskBmp)
        if (!maskBmp.isRecycled) maskBmp.recycle()
        if (!rgbSource.isRecycled) rgbSource.recycle()
        return out
    }

    private fun applyMask(
        bitmap: Bitmap,
        mask: SegmentationMask,
        params: MatteRefineParams
    ): Bitmap {
        val maskBitmap = maskToBitmap(mask)
        val refined = refineMask(maskBitmap, params)
        if (maskBitmap !== refined && !maskBitmap.isRecycled) {
            runCatching { maskBitmap.recycle() }
        }
        return BitmapUtils.applyAlphaMask(bitmap, refined)
    }

    private fun maskToBitmap(mask: SegmentationMask): Bitmap {
        val width = mask.width
        val height = mask.height
        val buffer = mask.buffer
        buffer.rewind()
        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            val confidence = buffer.getFloat()
            val alpha = (confidence.coerceIn(0f, 1f) * 255).toInt()
            pixels[i] = Color.argb(alpha, 255, 255, 255)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun refineMask(mask: Bitmap, params: MatteRefineParams): Bitmap {
        val width = mask.width
        val height = mask.height
        var pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)

        val p = params.precisionClamped
        val low = (40f - p * 30f).roundToInt().coerceIn(5, 50)
        val high = (200f + p * 45f).roundToInt().coerceIn(180, 250)

        for (i in pixels.indices) {
            val alpha = pixels[i] and 0xFF
            val smoothed = when {
                alpha < low -> 0
                alpha > high -> 255
                else -> alpha
            }
            pixels[i] = Color.argb(smoothed, 255, 255, 255)
        }

        val smoothRadius = (params.smoothClamped * 4f).roundToInt().coerceIn(0, 4)
        if (smoothRadius > 0) {
            pixels = boxBlurAlpha(pixels, width, height, smoothRadius)
        }
        val featherRadius = (params.featherClamped * 8f).roundToInt().coerceIn(0, 8)
        if (featherRadius > 0) {
            pixels = boxBlurAlpha(pixels, width, height, featherRadius)
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /** Separable box blur on alpha channel only (stored in ARGB white masks). */
    private fun boxBlurAlpha(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        if (radius <= 0) return src
        val tmp = IntArray(src.size)
        val out = IntArray(src.size)
        val span = radius * 2 + 1
        // Horizontal
        for (y in 0 until h) {
            var sum = 0
            for (kx in -radius..radius) {
                val x = kx.coerceIn(0, w - 1)
                sum += src[y * w + x] and 0xFF
            }
            for (x in 0 until w) {
                tmp[y * w + x] = Color.argb((sum / span).coerceIn(0, 255), 255, 255, 255)
                val leave = (x - radius).coerceIn(0, w - 1)
                val enter = (x + radius + 1).coerceIn(0, w - 1)
                sum += (src[y * w + enter] and 0xFF) - (src[y * w + leave] and 0xFF)
            }
        }
        // Vertical
        for (x in 0 until w) {
            var sum = 0
            for (ky in -radius..radius) {
                val y = ky.coerceIn(0, h - 1)
                sum += tmp[y * w + x] and 0xFF
            }
            for (y in 0 until h) {
                out[y * w + x] = Color.argb((sum / span).coerceIn(0, 255), 255, 255, 255)
                val leave = (y - radius).coerceIn(0, h - 1)
                val enter = (y + radius + 1).coerceIn(0, h - 1)
                sum += (tmp[enter * w + x] and 0xFF) - (tmp[leave * w + x] and 0xFF)
            }
        }
        return out
    }
}
