package com.pixijoy.photo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.heifwriter.HeifWriter
import com.pixijoy.core.common.AppResult
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

class BitmapPhotoEngine(
    private val vision: com.pixijoy.ml.VisionFacade = com.pixijoy.ml.UnavailableVision
) : PhotoEngine {
    override fun render(sources: List<String>, ops: List<PhotoOp>, maxEdge: Int): Bitmap =
        renderLayers(sources, ops.toLayers(), maxEdge)

    fun renderLayers(sources: List<String>, layers: List<LayeredOp>, maxEdge: Int): Bitmap {
        require(sources.isNotEmpty()) { "No media" }
        var current = decode(sources.first(), maxEdge)
        for (layer in layers) {
            if (!layer.visible) continue
            val next = applyOp(current, layer.op, sources, maxEdge)
            val composed = if (layer.op.isGeometry()) {
                next
            } else {
                val mask = rasterizeLayerMask(current, next, layer)
                blendBitmaps(current, next, layer.opacity, layer.blend, mask)
            }
            if (next !== current && next !== composed && !next.isRecycled) next.recycle()
            if (composed !== current && !current.isRecycled) current.recycle()
            current = composed
        }
        return current
    }

    override fun export(
        context: Context,
        sources: List<String>,
        ops: List<PhotoOp>,
        displayName: String,
        format: ExportFormat,
        quality: Int,
        keepExif: Boolean,
        stripGps: Boolean
    ): AppResult<String> = exportLayers(
        context,
        sources,
        ops.toLayers(),
        displayName,
        format,
        quality,
        keepExif,
        stripGps
    )

    fun exportLayers(
        context: Context,
        sources: List<String>,
        layers: List<LayeredOp>,
        displayName: String,
        format: ExportFormat,
        quality: Int,
        keepExif: Boolean = false,
        stripGps: Boolean = ExportExifPolicy.DEFAULT_STRIP_GPS
    ): AppResult<String> {
        val bitmap = renderLayers(sources, layers, maxEdge = 8192)
        val requested = format
        val effective = ExportFormatSupport.resolve(requested)
        val outName = if (effective == requested) {
            displayName
        } else {
            displayName.substringBeforeLast('.', displayName) + "." + effective.extension
        }
        val q = if (effective == ExportFormat.PNG) 100 else quality.coerceIn(60, 100)
        val exifSource = sources.firstOrNull()
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, outName)
                put(MediaStore.Images.Media.MIME_TYPE, effective.mime)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/PixiJoy"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return AppResult.Err("Could not create a Pictures file.")
            var finalFormat = effective
            var wrote = writeEncoded(resolver, uri, bitmap, finalFormat, q, context)
            if (!wrote && effective == ExportFormat.HEIC) {
                // Runtime HEIC failure → JPEG fallback (chip already documents HEIC→JPEG when unavailable).
                finalFormat = ExportFormat.JPEG
                val jpegName = outName.substringBeforeLast('.', outName) + ".jpg"
                values.clear()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, jpegName)
                values.put(MediaStore.Images.Media.MIME_TYPE, finalFormat.mime)
                resolver.update(uri, values, null, null)
                wrote = writeEncoded(resolver, uri, bitmap, finalFormat, q, context)
            }
            if (!wrote) {
                resolver.delete(uri, null, null)
                return AppResult.Err("Could not write ${finalFormat.extension}.")
            }
            // EXIF: JPEG/WebP/HEIC support rewrite; PNG typically has none — still try strip GPS.
            if (finalFormat != ExportFormat.PNG) {
                resolver.openFileDescriptor(uri, "rw")?.use { pfd: ParcelFileDescriptor ->
                    applyExportExif(exifSource, pfd, keepExif = keepExif, stripGps = stripGps)
                }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            AppResult.Ok(uri.toString())
        } catch (t: Throwable) {
            AppResult.Err("Export failed.", t)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }


    private fun writeEncoded(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        context: Context
    ): Boolean = when (format) {
        ExportFormat.HEIC -> writeHeic(context, bitmap, uri, quality)
        ExportFormat.AVIF -> false
        ExportFormat.JPEG, ExportFormat.PNG, ExportFormat.WEBP -> {
            val compressFormat = when (format) {
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.WEBP_LOSSY
            }
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(compressFormat, quality, out)
            } == true
        }
    }

    private fun writeHeic(context: Context, bitmap: Bitmap, uri: android.net.Uri, quality: Int): Boolean {
        val tmp = File(context.cacheDir, "pixijoy-export-${System.currentTimeMillis()}.heic")
        return try {
            val writer = HeifWriter.Builder(
                tmp.absolutePath,
                bitmap.width,
                bitmap.height,
                HeifWriter.INPUT_MODE_BITMAP
            ).setQuality(quality.coerceIn(60, 100)).build()
            writer.start()
            writer.addBitmap(bitmap)
            writer.stop(0)
            writer.close()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tmp).use { input -> input.copyTo(out) }
            } != null
        } catch (_: Throwable) {
            false
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    private fun rasterizeLayerMask(current: Bitmap, next: Bitmap, layer: LayeredOp): ByteArray? {
        if (layer.mask.isEmpty() && layer.maskFill == MaskFill.None) return null
        val fill = when (layer.maskFill) {
            MaskFill.None -> null
            MaskFill.Subject, MaskFill.Background -> {
                val bmp = vision.subjectMask(current) ?: return rasterizeMask(next.width, next.height, layer.mask)
                val scaled = if (bmp.width == next.width && bmp.height == next.height) bmp
                else Bitmap.createScaledBitmap(bmp, next.width, next.height, true)
                val px = IntArray(scaled.width * scaled.height)
                scaled.getPixels(px, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                if (scaled !== bmp && !scaled.isRecycled) scaled.recycle()
                if (!bmp.isRecycled) bmp.recycle()
                maskBytesFromArgb(px, invert = layer.maskFill == MaskFill.Background)
            }
        }
        return rasterizeMask(next.width, next.height, layer.mask, fill)
    }

    private fun decode(path: String, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val longEdge = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        while (longEdge / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, opts)
            ?: error("Could not decode $path")
    }

    private fun applyOp(
        src: Bitmap,
        op: PhotoOp,
        sources: List<String>,
        maxEdge: Int
    ): Bitmap = when (op) {
        is PhotoOp.Crop -> {
            val px = op.toPixels(src.width, src.height)
            Bitmap.createBitmap(src, px.left, px.top, px.width, px.height)
        }
        is PhotoOp.Rotate -> {
            val matrix = Matrix().apply { postRotate(op.degrees.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }
        is PhotoOp.FlipH -> {
            val matrix = Matrix().apply { postScale(-1f, 1f, src.width / 2f, src.height / 2f) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }
        is PhotoOp.Grade -> applyGradeBitmap(src, op.exposure, op.contrast)
        is PhotoOp.Hsl -> applyHsl(src, op.hue, op.saturation)
        is PhotoOp.Restore -> applyRestore(src)
        is PhotoOp.Expand -> applyExpand(src, op.fraction)
        is PhotoOp.Cutout -> applyCutout(src)
        is PhotoOp.Heal -> applyHeal(src, op.stamps)
        is PhotoOp.Fill -> applyFill(src, op.stamps)
        is PhotoOp.Erase -> applyErase(src, op.stamps)
        is PhotoOp.TextLayer -> applyText(src, op)
        is PhotoOp.Sticker -> applySticker(src, op)
        is PhotoOp.Collage -> {
            val bitmaps = sources.map { decode(it, maxEdge / 2) }
            val collage = applyCollage(bitmaps, op.columns, maxEdge)
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            collage
        }
        is PhotoOp.Curves -> applyCurves(src, op.y)
        is PhotoOp.Blur -> applyBlurPasses(src, op.radius)
        is PhotoOp.Sharpen -> applyUnsharp(src, op.amount)
        is PhotoOp.Vignette -> applyVignette(src, op.amount)
        is PhotoOp.Subject -> {
            val mask = vision.subjectMask(src)
            if (mask != null) {
                val cut = applyMaskAlpha(src, mask)
                if (!mask.isRecycled) mask.recycle()
                cut
            } else {
                applyCutout(src)
            }
        }
    }
}
