package com.pixijoy.ai.ui.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.DrawableRes
import com.pixijoy.ai.ui.board.components.BoardFlowArt

/**
 * Rasterize Flow 16 collage layouts (2–9 cells) from drawable thumbs into one bitmap.
 */
object BoardCollageRasterizer {
    fun render(
        context: Context,
        @DrawableRes thumbs: List<Int>,
        layoutIndex: Int,
        outSide: Int = 1080
    ): Bitmap {
        val bitmaps = thumbs.mapNotNull { res ->
            android.graphics.BitmapFactory.decodeResource(context.resources, res)
        }
        if (bitmaps.isEmpty()) {
            return android.graphics.BitmapFactory.decodeResource(context.resources, BoardFlowArt.collageHero)
                ?: Bitmap.createBitmap(outSide, outSide, Bitmap.Config.ARGB_8888)
        }
        val out = Bitmap.createBitmap(outSide, outSide, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.rgb(20, 20, 28))
        val gap = outSide * 0.012f
        val count = bitmaps.size.coerceIn(2, 9)
        val cells = bitmaps.take(count)
        when {
            layoutIndex == 5 -> drawCircleEmphasis(canvas, cells, outSide)
            layoutIndex == 2 || count == 3 -> drawColumns(canvas, cells.take(3), outSide, gap)
            count == 2 -> drawColumns(canvas, cells.take(2), outSide, gap)
            count <= 4 -> drawGrid(canvas, cells, outSide, gap, cols = 2)
            count <= 6 -> drawGrid(canvas, cells, outSide, gap, cols = 3)
            else -> drawGrid(canvas, cells, outSide, gap, cols = 3)
        }
        cells.forEach { b -> if (!b.isRecycled) runCatching { b.recycle() } }
        return out
    }

    private fun drawColumns(canvas: Canvas, cells: List<Bitmap>, side: Int, gap: Float) {
        val n = cells.size.coerceAtLeast(1)
        val cellW = (side - gap * (n + 1)) / n
        cells.forEachIndexed { i, bmp ->
            val left = gap + i * (cellW + gap)
            drawCover(canvas, bmp, RectF(left, gap, left + cellW, side - gap))
        }
    }

    private fun drawGrid(canvas: Canvas, cells: List<Bitmap>, side: Int, gap: Float, cols: Int) {
        val rows = ((cells.size + cols - 1) / cols).coerceAtLeast(1)
        val cellW = (side - gap * (cols + 1)) / cols
        val cellH = (side - gap * (rows + 1)) / rows
        cells.forEachIndexed { i, bmp ->
            val c = i % cols
            val r = i / cols
            val left = gap + c * (cellW + gap)
            val top = gap + r * (cellH + gap)
            drawCover(canvas, bmp, RectF(left, top, left + cellW, top + cellH))
        }
    }

    private fun drawCircleEmphasis(canvas: Canvas, cells: List<Bitmap>, side: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        cells.take(4).forEachIndexed { i, bmp ->
            val diameter = if (i == 0) side * 0.48f else side * 0.22f
            val cx = when (i) {
                0 -> side / 2f
                1 -> side * 0.22f
                2 -> side * 0.78f
                else -> side * 0.5f
            }
            val cy = when (i) {
                0 -> side * 0.42f
                1 -> side * 0.78f
                2 -> side * 0.78f
                else -> side * 0.18f
            }
            val scaled = Bitmap.createScaledBitmap(bmp, diameter.toInt(), diameter.toInt(), true)
            val circle = Bitmap.createBitmap(diameter.toInt(), diameter.toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(circle)
            c.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            c.drawBitmap(scaled, 0f, 0f, paint)
            paint.xfermode = null
            canvas.drawBitmap(circle, cx - diameter / 2f, cy - diameter / 2f, null)
            scaled.recycle()
            circle.recycle()
        }
    }

    private fun drawCover(canvas: Canvas, bmp: Bitmap, dest: RectF) {
        val src = coverSrc(bmp, dest.width() / dest.height())
        canvas.drawBitmap(bmp, src, dest, null)
    }

    private fun coverSrc(bmp: Bitmap, targetAspect: Float): Rect {
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val srcAspect = bw / bh
        return if (srcAspect > targetAspect) {
            val w = bh * targetAspect
            val left = ((bw - w) / 2f).toInt()
            Rect(left, 0, (left + w).toInt(), bmp.height)
        } else {
            val h = bw / targetAspect
            val top = ((bh - h) / 2f).toInt()
            Rect(0, top, bmp.width, (top + h).toInt())
        }
    }
}
