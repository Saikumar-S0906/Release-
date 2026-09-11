package com.pixijoy.ai.domain.model

data class AiPhotoAnalysis(
    val sceneDescription: String,
    val detectedMood: String,
    val memeTopText: String,
    val memeBottomText: String,
    val suggestedOutputs: List<String>,
    val isCloudPowered: Boolean
)

data class AiEditPlan(
    val action: NlAction,
    val message: String,
    val backgroundPrompt: String? = null,
    val memeTopText: String? = null,
    val memeBottomText: String? = null,
    val isCloudPowered: Boolean = false
)

data class AiCaptionResult(
    val topText: String,
    val bottomText: String,
    val isCloudPowered: Boolean,
    val userMessage: String? = null
)

enum class AiFeature {
    PHOTO_ANALYSIS,
    NL_COMMAND,
    CAPTION_GENERATION,
    BACKGROUND_REPLACE,
    OBJECT_REMOVAL,
    IMAGE_GENERATION
}
