package com.pixijoy.ai.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {
    fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    }

    /** Downscale without recycling [bitmap] — safe for Compose previews. */
    fun downscaleCopy(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun applyExifOrientation(context: android.content.Context, uri: android.net.Uri, bitmap: Bitmap): Bitmap {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                val degrees = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (degrees == 0f) return@use bitmap
                val matrix = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } ?: bitmap
        }.getOrDefault(bitmap)
    }

    fun copy(bitmap: Bitmap): Bitmap = copySafe(bitmap)

    fun copySafe(bitmap: Bitmap): Bitmap {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: Bitmap.createBitmap(bitmap.width.coerceAtLeast(1), bitmap.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    }

    fun applyAlphaMask(source: Bitmap, mask: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val scaledMask = if (mask.width != width || mask.height != height) {
            Bitmap.createScaledBitmap(mask, width, height, true)
        } else {
            mask
        }
        scaledMask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val alpha = (maskPixels[i] and 0xFF)
            pixels[i] = (pixels[i] and 0x00FFFFFF) or (alpha shl 24)
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        if (scaledMask != mask) scaledMask.recycle()
        return output
    }

    fun compositeOverBackground(foreground: Bitmap, backgroundColor: Int): Bitmap {
        val output = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(foreground, 0f, 0f, null)
        return output
    }

    fun blurRegion(source: Bitmap, mask: Bitmap, radius: Float): Bitmap {
        val width = source.width
        val height = source.height
        val output = copy(source)
        val pixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        val scaledMask = Bitmap.createScaledBitmap(mask, width, height, true)
        scaledMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val blurred = boxBlur(copy(source), radius.toInt().coerceIn(2, 24))
        val blurredPixels = IntArray(width * height)
        blurred.getPixels(blurredPixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val foregroundConfidence = min(255, max(0, 255 - (maskPixels[i] and 0xFF)))
            if (foregroundConfidence < 200) {
                pixels[i] = blurredPixels[i]
            }
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        blurred.recycle()
        scaledMask.recycle()
        return output
    }

    private fun boxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val result = pixels.copyOf()

        fun blurPass(horizontal: Boolean) {
            val output = result.copyOf()
            if (horizontal) {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        var r = 0
                        var g = 0
                        var b = 0
                        var count = 0
                        for (dx in -radius..radius) {
                            val nx = (x + dx).coerceIn(0, width - 1)
                            val color = pixels[y * width + nx]
                            r += (color shr 16) and 0xFF
                            g += (color shr 8) and 0xFF
                            b += color and 0xFF
                            count++
                        }
                        output[y * width + x] = (0xFF shl 24) or
                            ((r / count) shl 16) or
                            ((g / count) shl 8) or
                            (b / count)
                    }
                }
            } else {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        var r = 0
                        var g = 0
                        var b = 0
                        var count = 0
                        for (dy in -radius..radius) {
                            val ny = (y + dy).coerceIn(0, height - 1)
                            val color = result[ny * width + x]
                            r += (color shr 16) and 0xFF
                            g += (color shr 8) and 0xFF
                            b += color and 0xFF
                            count++
                        }
                        output[y * width + x] = (0xFF shl 24) or
                            ((r / count) shl 16) or
                            ((g / count) shl 8) or
                            (b / count)
                    }
                }
            }
            System.arraycopy(output, 0, result, 0, result.size)
            System.arraycopy(result, 0, pixels, 0, pixels.size)
        }

        blurPass(horizontal = true)
        blurPass(horizontal = false)
        val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        blurredBitmap.setPixels(result, 0, width, 0, 0, width, height)
        return blurredBitmap
    }
}
