package com.pixijoy.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * R3 C2: RAW/DNG open + metadata + editable JPEG preview sidecar.
 * Full non-destructive develop track is a later slice.
 */
data class RawPreviewInfo(
    val path: String,
    val width: Int,
    val height: Int,
    val mime: String,
    val isRaw: Boolean,
    val previewPath: String? = null,
    val make: String? = null,
    val model: String? = null,
    val iso: String? = null,
    val exposureTime: String? = null
)

object RawPreview {
    private val RAW_EXT = setOf("dng", "nef", "cr2", "arw", "orf", "rw2")

    fun looksLikeRaw(path: String): Boolean {
        val ext = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return ext in RAW_EXT
    }

    fun isRawMime(mime: String): Boolean {
        val m = mime.lowercase()
        return m.contains("dng") || m.contains("raw") || m == "image/x-adobe-dng"
    }

    fun infoFromProbe(path: String, width: Int, height: Int, mime: String): RawPreviewInfo =
        RawPreviewInfo(
            path = path,
            width = width,
            height = height,
            mime = mime,
            isRaw = looksLikeRaw(path) || isRawMime(mime)
        )

    fun open(file: File): RawPreviewInfo {
        val mimeGuess = when (file.extension.lowercase()) {
            "dng" -> "image/x-adobe-dng"
            "nef" -> "image/x-nikon-nef"
            "cr2" -> "image/x-canon-cr2"
            "arw" -> "image/x-sony-arw"
            else -> "image/*"
        }
        var width = 0
        var height = 0
        var make: String? = null
        var model: String? = null
        var iso: String? = null
        var exposureTime: String? = null
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            make = exif.getAttribute(ExifInterface.TAG_MAKE)
            model = exif.getAttribute(ExifInterface.TAG_MODEL)
            iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        }
        if (width <= 0 || height <= 0) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            width = bounds.outWidth
            height = bounds.outHeight
        }
        val preview = File(file.parentFile, "preview.jpg")
        val previewPath = preview.takeIf { it.isFile }?.absolutePath
        return RawPreviewInfo(
            path = file.absolutePath,
            width = width,
            height = height,
            mime = mimeGuess,
            isRaw = true,
            previewPath = previewPath,
            make = make,
            model = model,
            iso = iso,
            exposureTime = exposureTime
        )
    }

    /**
     * Writes `{parent}/preview.jpg` from embedded Exif thumbnail or BitmapFactory decode.
     * @return true if a usable preview was written or already exists
     */
    fun ensurePreviewJpeg(rawFile: File, maxEdge: Int = 2048): Boolean {
        val out = File(rawFile.parentFile, "preview.jpg")
        if (out.isFile && out.length() > 32) return true
        val thumb = runCatching {
            ExifInterface(rawFile.absolutePath).thumbnail
        }.getOrNull()
        if (thumb != null && thumb.isNotEmpty()) {
            val bmp = BitmapFactory.decodeByteArray(thumb, 0, thumb.size)
            if (bmp != null) {
                writeJpeg(scaleDown(bmp, maxEdge), out)
                if (!bmp.isRecycled) bmp.recycle()
                return out.isFile
            }
            // raw thumb bytes may already be JPEG
            out.writeBytes(thumb)
            if (out.length() > 32) return true
        }
        val decoded = decodeDownsampled(rawFile, maxEdge)
        if (decoded != null) {
            writeJpeg(decoded, out)
            if (!decoded.isRecycled) decoded.recycle()
            return out.isFile
        }
        return false
    }

    fun decodeForEdit(path: String, maxEdge: Int): Bitmap? {
        val file = File(path)
        if (!file.isFile) return null
        if (looksLikeRaw(path)) {
            val preview = File(file.parentFile, "preview.jpg")
            if (preview.isFile) {
                decodeDownsampled(preview, maxEdge)?.let { return it }
            }
            ensurePreviewJpeg(file, maxEdge)
            File(file.parentFile, "preview.jpg").takeIf { it.isFile }?.let {
                decodeDownsampled(it, maxEdge)?.let { return it }
            }
        }
        return decodeDownsampled(file, maxEdge)
    }

    private fun decodeDownsampled(file: File, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        while (longEdge / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun scaleDown(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = maxOf(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val scale = maxEdge.toFloat() / longEdge
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun writeJpeg(bmp: Bitmap, out: File) {
        FileOutputStream(out).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
    }
}
