package com.pixijoy.ai.core.agent

import android.content.Context
import android.net.Uri
import com.pixijoy.ai.R
import com.pixijoy.ai.creative.AgentCaptionGenerator
import com.pixijoy.ai.creative.AgentIntentParser
import com.pixijoy.ai.creative.ReelExporter
import com.pixijoy.ai.creative.TalkingPhotoAnimator
import com.pixijoy.ai.data.ImageProcessor
import com.pixijoy.ai.domain.model.AgentProgress
import com.pixijoy.ai.domain.model.AgentResult
import com.pixijoy.ai.domain.model.AgentStep
import com.pixijoy.ai.domain.model.AgentStepStatus
import com.pixijoy.ai.domain.model.AgentStepType
import com.pixijoy.ai.ml.ImageEnhancer
import com.pixijoy.ai.ml.BackgroundRemover
import com.pixijoy.ai.ml.BackgroundStudioProcessor
import com.pixijoy.ai.util.ImageExporter
import com.pixijoy.ai.util.SocialPlatform
import com.pixijoy.ai.util.SocialShareHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreativeAgentOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentParser: AgentIntentParser,
    private val captionGenerator: AgentCaptionGenerator,
    private val imageProcessor: ImageProcessor,
    private val imageEnhancer: ImageEnhancer,
    private val backgroundRemover: BackgroundRemover,
    private val backgroundStudio: BackgroundStudioProcessor,
    private val talkingPhotoAnimator: TalkingPhotoAnimator,
    private val reelExporter: ReelExporter,
    private val imageExporter: ImageExporter,
    private val socialShareHelper: SocialShareHelper
) {
    fun execute(prompt: String, photoUri: Uri): Flow<AgentProgress> = flow {
        val steps = AgentStepType.entries.map { AgentStep(it) }.toMutableList()
        fun update(type: AgentStepType, status: AgentStepStatus, message: String = "") {
            val index = steps.indexOfFirst { it.type == type }
            if (index >= 0) steps[index] = AgentStep(type, status, message)
        }
        suspend fun emitProgress(result: AgentResult? = null, error: String? = null) {
            emit(AgentProgress(steps.toList(), result, error))
        }

        try {
            update(AgentStepType.PARSE_INTENT, AgentStepStatus.RUNNING)
            emitProgress()
            val intent = intentParser.parse(prompt)
            update(AgentStepType.PARSE_INTENT, AgentStepStatus.DONE, context.getString(R.string.agent_step_parse_done))
            emitProgress()

            update(AgentStepType.SELECT_PHOTO, AgentStepStatus.RUNNING)
            emitProgress()
            val source = imageProcessor.loadBitmap(photoUri)
            update(AgentStepType.SELECT_PHOTO, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.ENHANCE, AgentStepStatus.RUNNING)
            emitProgress()
            val enhanced = imageEnhancer.enhance(source)
            update(AgentStepType.ENHANCE, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.REMOVE_BACKGROUND, AgentStepStatus.RUNNING)
            emitProgress()
            val cutout = runCatching { backgroundRemover.removeBackground(enhanced) }.getOrElse { enhanced }
            update(AgentStepType.REMOVE_BACKGROUND, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.THEME_BACKGROUND, AgentStepStatus.RUNNING)
            emitProgress()
            val themed = runCatching {
                backgroundStudio.replaceBackground(cutout, intent.themePrompt)
            }.getOrElse { enhanced }
            update(AgentStepType.THEME_BACKGROUND, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.GENERATE_CAPTION, AgentStepStatus.RUNNING)
            emitProgress()
            val caption = captionGenerator.generate(intent)
            update(AgentStepType.GENERATE_CAPTION, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.CREATE_ANIMATION, AgentStepStatus.RUNNING)
            emitProgress()
            val storyCard = imageProcessor.createStoryCard(themed, caption.primary, caption.secondary)
            val animFrames = talkingPhotoAnimator.animate(storyCard)
            update(AgentStepType.CREATE_ANIMATION, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.ADD_VOICE, AgentStepStatus.RUNNING)
            emitProgress()
            update(AgentStepType.ADD_MUSIC, AgentStepStatus.RUNNING)
            emitProgress()

            update(AgentStepType.EXPORT_VIDEO, AgentStepStatus.RUNNING)
            emitProgress()
            val videoFile = File(context.cacheDir, "agent_reel_${System.currentTimeMillis()}.mp4")
            reelExporter.export(
                frames = animFrames,
                outputFile = videoFile,
                narration = caption.spoken,
                musicMood = intent.musicMood
            )
            animFrames.forEach { if (it != storyCard) it.recycle() }
            update(AgentStepType.ADD_VOICE, AgentStepStatus.DONE)
            update(AgentStepType.ADD_MUSIC, AgentStepStatus.DONE)
            val videoUri = imageExporter.saveVideoToGallery(videoFile)
            update(AgentStepType.EXPORT_VIDEO, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.CREATE_STICKER, AgentStepStatus.RUNNING)
            emitProgress()
            val stickerBitmap = imageProcessor.createWhatsAppSticker(themed)
            val stickerUri = imageExporter.saveToGallery(stickerBitmap, addWatermark = false)
            update(AgentStepType.CREATE_STICKER, AgentStepStatus.DONE)
            emitProgress()

            update(AgentStepType.PREPARE_SHARE, AgentStepStatus.RUNNING)
            emitProgress()
            val shareIntent = videoUri?.let { uri ->
                val platform = if (intent.platform == "whatsapp") SocialPlatform.WHATSAPP else SocialPlatform.GENERIC
                socialShareHelper.createShareIntent(uri, platform, "video/mp4")
            }
            update(AgentStepType.PREPARE_SHARE, AgentStepStatus.DONE)
            emitProgress(
                AgentResult(
                    videoUri = videoUri,
                    stickerUri = stickerUri,
                    caption = caption,
                    shareIntent = shareIntent,
                    previewBitmap = storyCard
                )
            )
            videoFile.delete()
        } catch (e: Exception) {
            emitProgress(error = e.message ?: context.getString(R.string.agent_failed))
        }
    }
}
