package com.pixijoy.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixijoy.ai.creative.AutoCaptionGenerator
import com.pixijoy.ai.creative.CaptionSuggestion
import com.pixijoy.ai.creative.FaceAwareTextPlacer
import com.pixijoy.ai.creative.FaceTextZones
import com.pixijoy.ai.creative.MemeComposer
import com.pixijoy.ai.creative.ProfilePicComposer
import com.pixijoy.ai.creative.QuoteCardComposer
import com.pixijoy.ai.creative.ViralPack
import com.pixijoy.ai.creative.ViralPackExporter
import com.pixijoy.ai.domain.model.MemeTextStyle
import com.pixijoy.ai.domain.model.PhotoTool
import com.pixijoy.ai.domain.model.SocialFormat
import com.pixijoy.ai.domain.model.StickerPlacement
import com.pixijoy.ai.ml.BackgroundRemover
import com.pixijoy.ai.ml.BackgroundStudioProcessor
import com.pixijoy.ai.ml.IdPhotoProcessor
import com.pixijoy.ai.ml.ImageEnhancer
import com.pixijoy.ai.ml.ObjectEraser
import com.pixijoy.ai.ml.PortraitBlurProcessor
import com.pixijoy.ai.data.ImageSessionRepository
import com.pixijoy.ai.util.BitmapUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessResult(
    val original: Bitmap,
    val processed: Bitmap,
    val tool: PhotoTool
)

@Singleton
class ImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backgroundRemover: BackgroundRemover,
    private val backgroundStudio: BackgroundStudioProcessor,
    private val objectEraser: ObjectEraser,
    private val idPhotoProcessor: IdPhotoProcessor,
    private val imageEnhancer: ImageEnhancer,
    private val portraitBlurProcessor: PortraitBlurProcessor,
    private val imageSession: ImageSessionRepository,
    private val memeComposer: MemeComposer,
    private val quoteCardComposer: QuoteCardComposer,
    private val profilePicComposer: ProfilePicComposer,
    private val viralPackExporter: ViralPackExporter,
    private val faceAwareTextPlacer: FaceAwareTextPlacer,
    private val autoCaptionGenerator: AutoCaptionGenerator
) {
    suspend fun loadBitmap(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("Unable to decode image")
        }

        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > MAX_DIMENSION * 2) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalArgumentException("Unable to open image")

        val oriented = BitmapUtils.applyExifOrientation(context, uri, decoded)
        BitmapUtils.downscaleIfNeeded(oriented, MAX_DIMENSION)
    }

    suspend fun process(tool: PhotoTool, bitmap: Bitmap): ProcessResult = withContext(Dispatchers.Default) {
        val processed = when (tool) {
            PhotoTool.BACKGROUND_REMOVE -> backgroundRemover.removeBackground(bitmap)
            PhotoTool.ENHANCE -> imageEnhancer.enhance(bitmap)
            PhotoTool.PORTRAIT_BLUR -> portraitBlurProcessor.applyBlur(bitmap)
            PhotoTool.BACKGROUND_REPLACE -> {
                val prompt = imageSession.pendingBackgroundPrompt ?: "professional studio"
                backgroundStudio.replaceBackground(bitmap, prompt)
            }
            // Paint-to-cover only — never auto-wipe faces on load.
            PhotoTool.AI_ERASE -> BitmapUtils.copy(bitmap)
            PhotoTool.ID_PHOTO -> idPhotoProcessor.create(bitmap)
            else -> bitmap
        }
        ProcessResult(original = bitmap, processed = processed, tool = tool)
    }

    suspend fun composeCreative(
        base: Bitmap,
        style: MemeTextStyle,
        format: SocialFormat,
        stickers: List<StickerPlacement>,
        useFaceAware: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        if (useFaceAware) {
            val zones = faceAwareTextPlacer.detectTextZones(base)
            memeComposer.composeWithFaceZones(base, style, format, stickers, zones)
        } else {
            memeComposer.compose(base, style, format, stickers)
        }
    }

    suspend fun detectFaceZones(bitmap: Bitmap): FaceTextZones =
        withContext(Dispatchers.Default) { faceAwareTextPlacer.detectTextZones(bitmap) }

    fun suggestCaptions(): List<CaptionSuggestion> = autoCaptionGenerator.suggestCaptions()

    suspend fun createQuoteCard(quote: String, author: String, gradientIndex: Int): Bitmap =
        withContext(Dispatchers.Default) { quoteCardComposer.create(context, quote, author, gradientIndex) }

    suspend fun createProfilePic(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) { profilePicComposer.create(bitmap) }

    suspend fun createWhatsAppSticker(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) { profilePicComposer.createWhatsAppSticker(bitmap) }

    suspend fun createReactionCard(before: Bitmap, after: Bitmap, caption: String): Bitmap =
        withContext(Dispatchers.Default) { memeComposer.createReactionCard(context, before, after, caption) }

    suspend fun createStoryCard(bitmap: Bitmap, headline: String, subline: String): Bitmap =
        withContext(Dispatchers.Default) { memeComposer.createStoryCard(context, bitmap, headline, subline) }

    suspend fun createViralPack(
        original: Bitmap,
        enhanced: Bitmap,
        memeStyle: MemeTextStyle,
        storyHeadline: String,
        storySubline: String,
        reactionCaption: String
    ): ViralPack = withContext(Dispatchers.Default) {
        viralPackExporter.createPack(original, enhanced, memeStyle, storyHeadline, storySubline, reactionCaption)
    }

    fun eraseAtPoint(bitmap: Bitmap, x: Float, y: Float, radiusPx: Float): Bitmap =
        objectEraser.eraseAtPoint(bitmap, x, y, radiusPx)

    suspend fun createEnhancedPair(bitmap: Bitmap): Pair<Bitmap, Bitmap> = withContext(Dispatchers.Default) {
        val enhanced = imageEnhancer.enhance(bitmap)
        bitmap to enhanced
    }

    companion object {
        private const val MAX_DIMENSION = 2048
    }
}
