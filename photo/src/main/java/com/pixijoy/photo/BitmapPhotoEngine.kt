package com.pixijoy.photo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import com.pixijoy.core.common.AppResult
import kotlin.math.max

class BitmapPhotoEngine : PhotoEngine {
    override fun render(sources: List<String>, ops: List<PhotoOp>, maxEdge: Int): Bitmap {
        require(sources.isNotEmpty()) { "No media" }
        var current = decode(sources.first(), maxEdge)
        for (op in ops) {
            val next = applyOp(current, op, sources, maxEdge)
            if (next !== current && !current.isRecycled) current.recycle()
            current = next
        }
        return current
    }

    override fun export(
        context: Context,
        sources: List<String>,
        ops: List<PhotoOp>,
        displayName: String,
        format: ExportFormat,
        quality: Int
    ): AppResult<String> {
        val bitmap = render(sources, ops, maxEdge = 8192)
        val compressFormat = when (format) {
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP_LOSSY
        }
        val q = if (format == ExportFormat.PNG) 100 else quality.coerceIn(60, 100)
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mime)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/PixiJoy"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return AppResult.Err("Could not create a Pictures file.")
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(compressFormat, q, out)) {
                    return AppResult.Err("Could not write ${format.extension}.")
                }
            } ?: return AppResult.Err("Could not open export stream.")
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
        is PhotoOp.Erase -> applyErase(src, op.stamps)
        is PhotoOp.TextLayer -> applyText(src, op)
        is PhotoOp.Sticker -> applySticker(src, op)
        is PhotoOp.Collage -> {
            val bitmaps = sources.map { decode(it, maxEdge / 2) }
            val collage = applyCollage(bitmaps, op.columns, maxEdge)
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            collage
        }
    }
}
