package com.pixijoy.ai.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.pixijoy.ai.BuildConfig
import com.pixijoy.ai.R
import com.pixijoy.ai.core.network.GeminiAnalysisJson
import com.pixijoy.ai.core.network.GeminiApi
import com.pixijoy.ai.core.network.GeminiCaptionJson
import com.pixijoy.ai.core.network.GeminiCommandJson
import com.pixijoy.ai.core.network.GeminiContent
import com.pixijoy.ai.core.network.GeminiGenerationConfig
import com.pixijoy.ai.core.network.GeminiInlineData
import com.pixijoy.ai.core.network.GeminiPart
import com.pixijoy.ai.core.network.GeminiRequest
import com.pixijoy.ai.core.network.ProxyAiRequest
import com.pixijoy.ai.core.network.PixiJoyBackendApi
import com.pixijoy.ai.domain.model.AiCaptionResult
import com.pixijoy.ai.domain.model.AiEditPlan
import com.pixijoy.ai.domain.model.AiPhotoAnalysis
import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.domain.model.NlAction
import com.pixijoy.ai.domain.model.promptLabel
import com.pixijoy.ai.util.BitmapUtils
import com.pixijoy.ai.util.LocaleFormatters
import com.pixijoy.ai.core.network.GeminiVariationsJson
import com.pixijoy.ai.core.network.VariationStyleJson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudAiService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiApi: GeminiApi,
    private val backendApi: PixiJoyBackendApi,
    private val gson: Gson,
    private val localAi: LocalAiSimulator
) {
    val isConfigured: Boolean
        get() = useProxy || useLocalSimulator || BuildConfig.GEMINI_API_KEY.isNotBlank()

    /** True only when calling Google Gemini directly (not mock / local simulator). */
    val isLiveGemini: Boolean
        get() = !useLocalSimulator && !useProxy && BuildConfig.GEMINI_API_KEY.isNotBlank()

    private val useProxy: Boolean
        get() = BuildConfig.PIXIJOY_API_BASE_URL.isNotBlank()

    private val useLocalSimulator: Boolean
        get() = !useProxy && (
            BuildConfig.GEMINI_API_KEY.isBlank() ||
                BuildConfig.GEMINI_API_KEY.startsWith("dummy", ignoreCase = true) ||
                BuildConfig.GEMINI_API_KEY == "your_gemini_api_key_here"
            )

    suspend fun analyzePhoto(bitmap: Bitmap): AiPhotoAnalysis {
        val json = requestJson(bitmap = bitmap, prompt = AiPrompts.PHOTO_ANALYSIS)
        val parsed = gson.fromJson(json, GeminiAnalysisJson::class.java)
        return AiPhotoAnalysis(
            sceneDescription = parsed.sceneDescription,
            detectedMood = parsed.detectedMood,
            memeTopText = parsed.memeTopText,
            memeBottomText = parsed.memeBottomText,
            suggestedOutputs = parsed.suggestedOutputs,
            isCloudPowered = true
        )
    }

    suspend fun parseCommand(bitmap: Bitmap, command: String): AiEditPlan {
        val json = requestJson(bitmap = bitmap, prompt = AiPrompts.nlCommand(command, LocaleFormatters.deviceLanguageTag()))
        val parsed = gson.fromJson(json, GeminiCommandJson::class.java)
        return AiEditPlan(
            action = parseAction(parsed.action),
            message = parsed.message.ifBlank { context.getString(R.string.ai_processing) },
            backgroundPrompt = parsed.backgroundPrompt,
            memeTopText = parsed.memeTopText,
            memeBottomText = parsed.memeBottomText,
            isCloudPowered = true
        )
    }

    suspend fun generateCaption(bitmap: Bitmap, tone: CaptionTone, sceneDescription: String): AiCaptionResult {
        val json = requestJson(
            bitmap = bitmap,
            prompt = AiPrompts.caption(tone.promptLabel(context), sceneDescription)
        )
        val parsed = gson.fromJson(json, GeminiCaptionJson::class.java)
        return AiCaptionResult(
            topText = parsed.topText,
            bottomText = parsed.bottomText,
            isCloudPowered = true
        )
    }

    suspend fun suggestVariationStyles(bitmap: Bitmap): List<VariationStyleJson> {
        val json = requestJson(bitmap = bitmap, prompt = AiPrompts.VARIATIONS)
        return gson.fromJson(json, GeminiVariationsJson::class.java).variations
    }

    private suspend fun requestJson(bitmap: Bitmap, prompt: String): String {
        if (useLocalSimulator) return localAi.respondToPrompt(prompt)
        val scaled = BitmapUtils.downscaleIfNeeded(bitmap, 1024)
        val base64 = bitmapToBase64(scaled)
        if (scaled != bitmap) scaled.recycle()

        val rawText = if (useProxy) {
            val response = backendApi.proxyGeminiRequest(
                authToken = "",
                request = ProxyAiRequest(
                    prompt = prompt,
                    imageBase64 = base64,
                    locale = LocaleFormatters.deviceLanguageTag()
                )
            )
            response.text
        } else {
            val apiKey = BuildConfig.GEMINI_API_KEY
            require(apiKey.isNotBlank()) { "Gemini API key not configured" }

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/jpeg",
                                    data = base64
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig()
            )

            val response = try {
                geminiApi.generateContent(BuildConfig.GEMINI_MODEL, apiKey, request)
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string().orEmpty()
                throw IllegalStateException(
                    "Gemini HTTP ${e.code()} model=${BuildConfig.GEMINI_MODEL}: ${body.take(300)}",
                    e
                )
            }
            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: throw IllegalStateException("Empty AI response from ${BuildConfig.GEMINI_MODEL}")
        }

        return rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseAction(raw: String): NlAction = when (raw.uppercase()) {
        "ENHANCE" -> NlAction.ENHANCE
        "REMOVE_BACKGROUND" -> NlAction.REMOVE_BACKGROUND
        "MAKE_MEME", "MAKE_FUNNY" -> NlAction.MAKE_MEME
        "MAKE_STICKER" -> NlAction.MAKE_STICKER
        "PORTRAIT_BLUR" -> NlAction.PORTRAIT_BLUR
        "BRIGHTEN" -> NlAction.BRIGHTEN
        "DARKEN" -> NlAction.DARKEN
        "REPLACE_BACKGROUND" -> NlAction.REPLACE_BACKGROUND
        "OBJECT_REMOVE" -> NlAction.OBJECT_REMOVE
        "CREATE_REEL" -> NlAction.CREATE_REEL
        "PORTRAIT_STUDIO" -> NlAction.PORTRAIT_STUDIO
        "GENERATE_VARIATIONS" -> NlAction.GENERATE_VARIATIONS
        "ADVANCED_EDIT" -> NlAction.ADVANCED_EDIT
        else -> NlAction.UNKNOWN
    }
}
