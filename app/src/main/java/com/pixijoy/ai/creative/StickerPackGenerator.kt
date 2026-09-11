package com.pixijoy.ai.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.pixijoy.ai.domain.model.StickerEmotion
import com.pixijoy.ai.domain.model.StickerEmotions
import com.pixijoy.ai.ml.BackgroundRemover
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton

data class StickerPackResult(
    val stickers: List<Bitmap>,
    val labels: List<String>
)

@Singleton
class StickerPackGenerator @Inject constructor(
    private val backgroundRemover: BackgroundRemover
) {
    suspend fun generatePack(
        source: Bitmap,
        emotions: List<StickerEmotion> = StickerEmotions.pack,
        labels: List<String> = emotions.map { it.emoji },
        styleEmoji: String = "😀"
    ): StickerPackResult {
        val cutout = try {
            backgroundRemover.removeBackground(source)
        } catch (_: Exception) {
            BitmapUtils.copy(source)
        }
        val size = 512
        val scaledCutout = Bitmap.createScaledBitmap(
            cutout,
            (size * 0.7f).toInt(),
            (size * 0.7f).toInt(),
            true
        )
        val stickers = emotions.mapIndexed { index, emotion ->
            val label = labels.getOrElse(index) { emotion.emoji }
            createSticker(scaledCutout, emotion, label, size, styleEmoji)
        }
        if (scaledCutout != cutout) scaledCutout.recycle()
        cutout.recycle()
        return StickerPackResult(stickers, labels)
    }

    private fun createSticker(cutout: Bitmap, emotion: StickerEmotion, label: String, size: Int, styleEmoji: String): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT)

        val x = (size - cutout.width) / 2f
        val y = (size - cutout.height) / 2f + 20f
        canvas.drawBitmap(cutout, x, y, null)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C3AED")
        }
        canvas.drawCircle(size - 70f, 70f, 42f, badgePaint)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(styleEmoji.ifBlank { emotion.emoji }, size - 70f, 86f, emojiPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText(label.uppercase(), size / 2f, size - 24f, labelPaint)
        return output
    }
}
