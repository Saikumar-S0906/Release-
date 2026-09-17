package com.pixijoy.photo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Environment
import android.provider.MediaStore
import com.pixijoy.core.common.AppResult
import kotlin.math.max
import kotlin.math.pow

class BitmapPhotoEngine : PhotoEngine {
    override fun render(sourcePath: String, ops: List<PhotoOp>, maxEdge: Int): Bitmap {
        var current = decode(sourcePath, maxEdge)
        for (op in ops) {
            val next = applyOp(current, op)
            if (next !== current && !current.isRecycled) current.recycle()
            current = next
        }
        return current
    }

    override fun exportJpeg(
        context: Context,
        sourcePath: String,
        ops: List<PhotoOp>,
        displayName: String
    ): AppResult<String> {
        val bitmap = render(sourcePath, ops, maxEdge = 8192)
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
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
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                    return AppResult.Err("Could not write JPEG.")
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

    private fun applyOp(src: Bitmap, op: PhotoOp): Bitmap = when (op) {
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
        is PhotoOp.Grade -> applyGrade(src, op.exposure, op.contrast)
    }

    private fun applyGrade(src: Bitmap, exposureEv: Float, contrast: Float): Bitmap {
        val gain = 2f.pow(exposureEv)
        val c = contrast
        val translate = (1f - c) * 0.18f * 255f
        val matrix = ColorMatrix(
            floatArrayOf(
                c * gain, 0f, 0f, 0f, translate,
                0f, c * gain, 0f, 0f, translate,
                0f, 0f, c * gain, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
