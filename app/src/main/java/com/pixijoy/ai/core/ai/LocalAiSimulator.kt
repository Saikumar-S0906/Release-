package com.pixijoy.ai.core.ai

import com.google.gson.Gson
import com.pixijoy.ai.core.ai.NlKeywordMatcher
import com.pixijoy.ai.core.network.GeminiAnalysisJson
import com.pixijoy.ai.domain.model.NlAction
import com.pixijoy.ai.core.network.GeminiCaptionJson
import com.pixijoy.ai.core.network.GeminiCommandJson
import com.pixijoy.ai.core.network.GeminiVariationsJson
import com.pixijoy.ai.core.network.VariationStyleJson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline AI responses used when GEMINI_API_KEY is dummy/blank or backend proxy is mocked.
 * Replace with real Gemini key to use live cloud inference.
 */
@Singleton
class LocalAiSimulator @Inject constructor(
    private val gson: Gson
) {
    fun analyzePhoto(): String = gson.toJson(
        GeminiAnalysisJson(
            sceneDescription = "A cheerful moment perfect for viral content",
            detectedMood = "celebration",
            memeTopText = "WHEN THE PHOTO",
            memeBottomText = "HITS DIFFERENT",
            suggestedOutputs = listOf("meme", "sticker", "gif", "story", "whatsapp_sticker", "reel")
        )
    )

    fun parseCommand(command: String): String {
        val text = command.lowercase()
        val matched = NlKeywordMatcher.matchAction(command)
        val action = when (matched) {
            NlAction.REMOVE_BACKGROUND -> "REMOVE_BACKGROUND"
            NlAction.MAKE_MEME -> "MAKE_MEME"
            NlAction.MAKE_STICKER -> "MAKE_STICKER"
            NlAction.ENHANCE -> "ENHANCE"
            NlAction.PORTRAIT_BLUR -> "PORTRAIT_BLUR"
            NlAction.OBJECT_REMOVE -> "OBJECT_REMOVE"
            NlAction.CREATE_REEL -> "CREATE_REEL"
            NlAction.BRIGHTEN -> "BRIGHTEN"
            NlAction.DARKEN -> "DARKEN"
            NlAction.PORTRAIT_STUDIO -> "PORTRAIT_STUDIO"
            NlAction.GENERATE_VARIATIONS -> "GENERATE_VARIATIONS"
            NlAction.ADVANCED_EDIT -> "ADVANCED_EDIT"
            null -> "MAKE_MEME"
            else -> "MAKE_MEME"
        }
        val bg = when {
            text.contains("dubai") -> "Dubai skyline at golden hour"
            text.contains("lamborghini") -> "luxury sports car showroom"
            text.contains("mars") -> "futuristic Mars colony"
            text.contains("beach") -> "tropical beach sunset"
            text.contains("office") -> "modern professional office"
            text.contains("birthday") -> "pink birthday balloons confetti"
            else -> null
        }
        return gson.toJson(
            GeminiCommandJson(
                action = action,
                message = "PixiJoy AI understood your request",
                backgroundPrompt = bg,
                memeTopText = if (action == "MAKE_MEME") "WHEN AI" else null,
                memeBottomText = if (action == "MAKE_MEME") "DOES THE WORK" else null
            )
        )
    }

    fun caption(tone: String): String = gson.toJson(
        when (tone.lowercase()) {
            "telugu" -> GeminiCaptionJson("ఇది ఫన్నీ గా ఉంది", "PIXIJOY AI")
            "hindi" -> GeminiCaptionJson("JAB PHOTO", "TAB PIXIJOY")
            else -> GeminiCaptionJson("WHEN THE AI", "COOKS FOR YOU")
        }
    )

    fun variations(): String = gson.toJson(
        GeminiVariationsJson(
            variations = listOf(
                VariationStyleJson("Cinematic warm glow", "warm"),
                VariationStyleJson("Cool cyberpunk night", "cool"),
                VariationStyleJson("Vintage film grain", "vintage"),
                VariationStyleJson("Vibrant festival pop", "vivid")
            )
        )
    )

    fun respondToPrompt(prompt: String): String = when {
        prompt.contains("variation", ignoreCase = true) -> variations()
        prompt.contains("caption", ignoreCase = true) -> caption("English")
        prompt.contains("command", ignoreCase = true) || prompt.contains("User command", ignoreCase = true) -> {
            val cmd = prompt.substringAfter("User command: \"", "").substringBefore("\"").ifBlank { prompt }
            parseCommand(cmd)
        }
        else -> analyzePhoto()
    }
}
