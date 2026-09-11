package com.pixijoy.ai.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.pixijoy.ai.R
import com.pixijoy.ai.domain.model.MemeTextStyle
import com.pixijoy.ai.domain.model.SocialFormat
import com.pixijoy.ai.domain.model.StickerPlacement
import com.pixijoy.ai.util.BitmapUtils
import com.pixijoy.ai.util.CanvasFontHelper
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class MemeComposer @Inject constructor() {

    fun compose(
        base: Bitmap,
        style: MemeTextStyle,
        format: SocialFormat,
        stickers: List<StickerPlacement> = emptyList()
    ): Bitmap = composeWithFaceZones(base, style, format, stickers, null)

    fun composeWithFaceZones(
        base: Bitmap,
        style: MemeTextStyle,
        format: SocialFormat,
        stickers: List<StickerPlacement> = emptyList(),
        faceZones: com.pixijoy.ai.creative.FaceTextZones? = null
    ): Bitmap {
        val canvasBitmap = formatToCanvas(base, format)
        val output = Bitmap.createBitmap(
            canvasBitmap.width,
            canvasBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawBitmap(canvasBitmap, 0f, 0f, null)
        if (canvasBitmap != base) canvasBitmap.recycle()

        drawMemeText(canvas, output.width, output.height, style, faceZones)
        drawStickers(canvas, output.width, output.height, stickers)
        return output
    }

    fun createReactionCard(context: Context, before: Bitmap, after: Bitmap, caption: String): Bitmap {
        val cardWidth = 1080
        val cardHeight = 1350
        val output = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.parseColor("#0D0D0F"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val halfWidth = cardWidth / 2 - 24
        val imageHeight = 900

        val beforeScaled = Bitmap.createScaledBitmap(before, halfWidth, imageHeight, true)
        val afterScaled = Bitmap.createScaledBitmap(after, halfWidth, imageHeight, true)
        canvas.drawBitmap(beforeScaled, 16f, 120f, paint)
        canvas.drawBitmap(afterScaled, (halfWidth + 32).toFloat(), 120f, paint)
        beforeScaled.recycle()
        afterScaled.recycle()

        val labelTypeface = CanvasFontHelper.labelTypeface(context)
        drawLabel(canvas, context.getString(R.string.before).uppercase(), 16f, 90f, labelTypeface)
        drawLabel(canvas, context.getString(R.string.after).uppercase(), (halfWidth + 32).toFloat(), 90f, labelTypeface)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = CanvasFontHelper.memeTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(caption, cardWidth / 2f, 1100f, captionPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C3AED")
            textSize = 32f
            typeface = CanvasFontHelper.labelTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.made_with_pixijoy), cardWidth / 2f, 1280f, brandPaint)
        return output
    }

    fun createStoryCard(context: Context, bitmap: Bitmap, headline: String, subline: String): Bitmap {
        val width = 1080
        val height = 1920
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.parseColor("#0D0D0F"))

        val imageArea = Rect(60, 280, width - 60, 1400)
        val scaled = scaleCenterCrop(bitmap, imageArea.width(), imageArea.height())
        canvas.drawBitmap(scaled, imageArea.left.toFloat(), imageArea.top.toFloat(), null)
        scaled.recycle()

        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = CanvasFontHelper.memeTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(headline, width / 2f, 200f, headlinePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EC4899")
            textSize = 40f
            typeface = CanvasFontHelper.labelTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subline, width / 2f, 1520f, subPaint)

        val swipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 180
            textSize = 28f
            typeface = CanvasFontHelper.labelTypeface(context)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.share_platforms_hint), width / 2f, 1800f, swipePaint)
        return output
    }

    private fun formatToCanvas(bitmap: Bitmap, format: SocialFormat): Bitmap {
        val ratio = format.aspectRatio ?: return BitmapUtils.copy(bitmap)
        val srcRatio = bitmap.width.toFloat() / bitmap.height
        return if (srcRatio > ratio) {
            val newWidth = (bitmap.height * ratio).toInt()
            val x = (bitmap.width - newWidth) / 2
            Bitmap.createBitmap(bitmap, x, 0, newWidth, bitmap.height)
        } else {
            val newHeight = (bitmap.width / ratio).toInt()
            val y = (bitmap.height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, y, bitmap.width, newHeight)
        }
    }

    private fun drawMemeText(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: MemeTextStyle,
        faceZones: com.pixijoy.ai.creative.FaceTextZones? = null
    ) {
        val baseSize = min(width, height) * 0.08f * style.fontScale
        if (style.topText.isNotBlank()) {
            val topY = faceZones?.let { it.topTextY * height } ?: (baseSize + 24f)
            drawImpactText(canvas, style.topText, width / 2f, topY, baseSize, style)
        }
        if (style.bottomText.isNotBlank()) {
            val bottomY = faceZones?.let { it.bottomTextY * height } ?: (height - baseSize)
            drawImpactText(canvas, style.bottomText, width / 2f, bottomY, baseSize, style)
        }
    }

    private fun drawImpactText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        y: Float,
        textSize: Float,
        style: MemeTextStyle
    ) {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.strokeColor.toArgb()
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            this.style = Paint.Style.STROKE
            strokeWidth = textSize * 0.08f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textColor.toArgb()
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text.uppercase(), centerX, y, strokePaint)
        canvas.drawText(text.uppercase(), centerX, y, fillPaint)
    }

    private fun drawStickers(
        canvas: Canvas,
        width: Int,
        height: Int,
        stickers: List<StickerPlacement>
    ) {
        stickers.forEach { sticker ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 72f * sticker.scale
                textAlign = Paint.Align.CENTER
            }
            val x = width * sticker.offsetX
            val y = height * sticker.offsetY
            canvas.drawText(sticker.emoji, x, y, paint)
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float, typeface: Typeface) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            this.typeface = typeface
        }
        canvas.drawText(text, x, y, paint)
    }

    private fun scaleCenterCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = max(
            targetWidth.toFloat() / source.width,
            targetHeight.toFloat() / source.height
        )
        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val x = (scaledWidth - targetWidth) / 2
        val y = (scaledHeight - targetHeight) / 2
        val cropped = Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
        if (scaled != source) scaled.recycle()
        return cropped
    }
}
