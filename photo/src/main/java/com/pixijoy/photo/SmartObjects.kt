package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * F6 smart object — linked raster layer that can be replaced without losing
 * transform. Replace [source] and re-composite via [drawOnto].
 */
data class SmartObject(
    val id: String,
    val source: Bitmap,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 0.35f,
    val rotationDeg: Float = 0f
) {
    fun withSource(next: Bitmap): SmartObject = copy(source = next)

    fun drawOnto(canvas: Canvas, destW: Int, destH: Int) {
        val cx = x.coerceIn(0f, 1f) * destW
        val cy = y.coerceIn(0f, 1f) * destH
        val w = source.width * scale.coerceIn(0.05f, 2f)
        val h = source.height * scale.coerceIn(0.05f, 2f)
        val matrix = Matrix().apply {
            setTranslate(-source.width / 2f, -source.height / 2f)
            postScale(w / source.width, h / source.height)
            postRotate(rotationDeg)
            postTranslate(cx, cy)
        }
        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
}

/**
 * F6 advanced type — richer text layer with tracking / leading (rasterized).
 */
data class AdvancedType(
    val text: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val sizePx: Float = 64f,
    val trackingEm: Float = 0f,
    val leadingPx: Float = 0f,
    val color: Int = Color.WHITE
) {
    fun drawOnto(canvas: Canvas, destW: Int, destH: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = oliviaColor()
            textSize = sizePx.coerceIn(12f, 256f)
            textAlign = Paint.Align.CENTER
            letterSpacing = trackingEm.coerceIn(-0.2f, 0.5f)
        }
        val lines = text.split('\n')
        val lead = if (leadingPx > 0f) leadingPx else paint.fontSpacing
        var baseline = y.coerceIn(0f, 1f) * destH - (lines.size - 1) * lead * 0.5f
        val cx = x.coerceIn(0f, 1f) * destW
        for (line in lines) {
            canvas.drawText(line, cx, baseline, paint)
            baseline += lead
        }
    }

    private fun oliviaColor(): Int = color
}

/**
 * F6 print / PDF — multi-page PDF from one or more bitmaps (phone print path).
 */
object PdfExport {
    fun write(
        pages: List<Bitmap>,
        outFile: File,
        pageWidthPt: Int = 612,
        pageHeightPt: Int = 792
    ): Boolean {
        if (pages.isEmpty()) return false
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { index, bmp ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, index + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas
                val dest = RectF(36f, 36f, pageWidthPt - 36f, pageHeightPt - 36f)
                val src = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
                val matrix = Matrix()
                matrix.setRectToRect(src, dest, Matrix.ScaleToFit.CENTER)
                canvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
                doc.finishPage(page)
            }
            FileOutputStream(outFile).use { doc.writeTo(it) }
            return true
        } catch (_: Exception) {
            return false
        } finally {
            doc.close()
        }
    }
}
