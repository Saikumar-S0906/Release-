package com.pixijoy.ai.core.network

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    @SerializedName("inline_data") val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mime_type") val mimeType: String,
    val data: String
)

data class GeminiGenerationConfig(
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json",
    val temperature: Float = 0.7f
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiAnalysisJson(
    val sceneDescription: String = "",
    val detectedMood: String = "",
    val memeTopText: String = "",
    val memeBottomText: String = "",
    val suggestedOutputs: List<String> = emptyList()
)

data class GeminiCommandJson(
    val action: String = "UNKNOWN",
    val message: String = "",
    val backgroundPrompt: String? = null,
    val memeTopText: String? = null,
    val memeBottomText: String? = null
)

data class GeminiCaptionJson(
    val topText: String = "",
    val bottomText: String = ""
)

data class GeminiVariationsJson(
    val variations: List<VariationStyleJson> = emptyList()
)

data class VariationStyleJson(
    val label: String = "",
    val style: String = ""
)
