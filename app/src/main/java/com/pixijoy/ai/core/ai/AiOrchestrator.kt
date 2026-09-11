package com.pixijoy.ai.core.ai

import android.content.Context
import com.pixijoy.ai.R
import com.pixijoy.ai.creative.NaturalLanguageEditor
import com.pixijoy.ai.creative.SmartSuggestionEngine
import com.pixijoy.ai.creative.ToneCaptionGenerator
import com.pixijoy.ai.data.ConsentRepository
import com.pixijoy.ai.data.UserPreferencesRepository
import com.pixijoy.ai.domain.model.AiCaptionResult
import com.pixijoy.ai.domain.model.AiEditPlan
import com.pixijoy.ai.domain.model.AiPhotoAnalysis
import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.domain.model.CreationType
import com.pixijoy.ai.domain.model.ImageContext
import com.pixijoy.ai.domain.model.SmartSuggestion
import com.pixijoy.ai.domain.model.localizedTitle
import com.pixijoy.ai.ml.ImageContextAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudAiService: CloudAiService,
    private val consentRepository: ConsentRepository,
    private val contextAnalyzer: ImageContextAnalyzer,
    private val suggestionEngine: SmartSuggestionEngine,
    private val nlEditor: NaturalLanguageEditor,
    private val toneCaptionGenerator: ToneCaptionGenerator,
    private val preferencesRepository: UserPreferencesRepository
) {
    val isCloudAvailable: Boolean get() = cloudAiService.isConfigured

    private suspend fun canUseCloud(): Boolean {
        if (!cloudAiService.isConfigured) return false
        return consentRepository.aiCloudConsent.first()
    }

    suspend fun analyzePhoto(bitmap: android.graphics.Bitmap): AnalysisResult {
        val localContext = contextAnalyzer.analyze(bitmap)
        val localSuggestions = suggestionEngine.suggest(localContext)

        if (!canUseCloud()) {
            val caption = toneCaptionGenerator.generate(localContext, CaptionTone.ENGLISH)
            return AnalysisResult(
                context = localContext,
                suggestions = localSuggestions,
                sceneDescription = null,
                suggestedCaption = caption,
                isCloudPowered = false,
                userMessage = null
            )
        }

        return runCatching {
            if (!preferencesRepository.consumeAiCredit()) {
                return fallbackAnalysis(
                    localContext,
                    localSuggestions,
                    userMessage = context.getString(R.string.ai_limit_reached)
                )
            }
            val cloud = cloudAiService.analyzePhoto(bitmap)
            val mergedSuggestions = mergeCloudSuggestions(cloud, localSuggestions)
            AnalysisResult(
                context = localContext,
                suggestions = mergedSuggestions,
                sceneDescription = cloud.sceneDescription,
                suggestedCaption = cloud.memeTopText to cloud.memeBottomText,
                isCloudPowered = true,
                userMessage = null
            )
        }.getOrElse { error ->
            android.util.Log.e("AiOrchestrator", "Gemini analyze failed", error)
            // Refund credit charged before the failed cloud call.
            runCatching { preferencesRepository.refundAiCredits(1) }
            fallbackAnalysis(
                localContext,
                localSuggestions,
                userMessage = context.getString(R.string.ai_cloud_unavailable)
            )
        }
    }

    suspend fun parseCommand(bitmap: android.graphics.Bitmap?, command: String): AiEditPlan {
        if (bitmap != null && canUseCloud()) {
            return runCatching {
                if (!preferencesRepository.consumeAiCredit()) {
                    return localPlan(command, context.getString(R.string.ai_limit_reached))
                }
                cloudAiService.parseCommand(bitmap, command)
            }.getOrElse {
                runCatching { preferencesRepository.refundAiCredits(1) }
                localPlan(command)
            }
        }
        return localPlan(command)
    }

    suspend fun generateCaption(
        bitmap: android.graphics.Bitmap?,
        imageContext: ImageContext?,
        tone: CaptionTone,
        scene: String?
    ): AiCaptionResult {
        val localContext = imageContext ?: ImageContext()
        if (bitmap != null && canUseCloud() && scene != null) {
            return runCatching {
                if (!preferencesRepository.consumeAiCredit()) {
                    val local = toneCaptionGenerator.generate(localContext, tone)
                    return AiCaptionResult(
                        local.first,
                        local.second,
                        isCloudPowered = false,
                        userMessage = context.getString(R.string.ai_limit_reached)
                    )
                }
                cloudAiService.generateCaption(bitmap, tone, scene)
            }.getOrElse { error ->
                android.util.Log.e("AiOrchestrator", "Gemini caption failed", error)
                runCatching { preferencesRepository.refundAiCredits(1) }
                val local = toneCaptionGenerator.generate(localContext, tone)
                AiCaptionResult(
                    local.first,
                    local.second,
                    isCloudPowered = false,
                    userMessage = context.getString(R.string.ai_cloud_unavailable)
                )
            }
        }
        val local = toneCaptionGenerator.generate(localContext, tone)
        return AiCaptionResult(local.first, local.second, isCloudPowered = false)
    }

    private fun localPlan(command: String, message: String? = null): AiEditPlan {
        val result = nlEditor.parse(command)
        return AiEditPlan(
            action = result.action,
            message = message ?: result.message,
            isCloudPowered = false
        )
    }

    private fun fallbackAnalysis(
        context: ImageContext,
        suggestions: List<SmartSuggestion>,
        userMessage: String?
    ): AnalysisResult {
        val caption = toneCaptionGenerator.generate(context, CaptionTone.ENGLISH)
        return AnalysisResult(
            context = context,
            suggestions = suggestions,
            sceneDescription = null,
            suggestedCaption = caption,
            isCloudPowered = false,
            userMessage = userMessage
        )
    }

    private fun mergeCloudSuggestions(
        cloud: AiPhotoAnalysis,
        local: List<SmartSuggestion>
    ): List<SmartSuggestion> {
        val outputMap = mapOf(
            "meme" to CreationType.MEME,
            "sticker" to CreationType.STICKER,
            "gif" to CreationType.GIF,
            "story" to CreationType.STORY,
            "profile_pic" to CreationType.PROFILE,
            "whatsapp_sticker" to CreationType.WA_STICKER,
            "viral_pack" to CreationType.VIRAL_PACK,
            "reel" to CreationType.REEL,
            "enhance" to CreationType.ENHANCE,
            "quote" to CreationType.QUOTE
        )
        val cloudSuggestions = cloud.suggestedOutputs.mapNotNull { key ->
            outputMap[key.lowercase()]?.let { type ->
                SmartSuggestion(
                    type,
                    context.getString(R.string.ai_recommends, type.localizedTitle(context)),
                    priority = 0
                )
            }
        }
        return (cloudSuggestions + local).distinctBy { it.creationType }.sortedBy { it.priority }
    }

    data class AnalysisResult(
        val context: ImageContext,
        val suggestions: List<SmartSuggestion>,
        val sceneDescription: String?,
        val suggestedCaption: Pair<String, String>,
        val isCloudPowered: Boolean,
        val userMessage: String? = null
    )
}
