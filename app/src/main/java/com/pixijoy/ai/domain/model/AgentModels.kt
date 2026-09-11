package com.pixijoy.ai.domain.model

import android.content.Intent
import android.net.Uri

enum class AgentStepStatus { PENDING, RUNNING, DONE, FAILED }

enum class AgentStepType {
    PARSE_INTENT,
    SELECT_PHOTO,
    ENHANCE,
    REMOVE_BACKGROUND,
    THEME_BACKGROUND,
    GENERATE_CAPTION,
    CREATE_ANIMATION,
    ADD_VOICE,
    ADD_MUSIC,
    EXPORT_VIDEO,
    CREATE_STICKER,
    PREPARE_SHARE
}

data class AgentStep(
    val type: AgentStepType,
    val status: AgentStepStatus = AgentStepStatus.PENDING,
    val message: String = ""
)

data class AgentIntent(
    val occasion: String = "general",
    val recipient: String? = null,
    val platform: String = "whatsapp",
    val tone: String = "funny",
    val language: CaptionTone = CaptionTone.TELUGU,
    val themePrompt: String = "celebration gradient",
    val musicMood: MusicMood = MusicMood.BIRTHDAY
)

data class AgentCaption(
    val primary: String,
    val secondary: String,
    val spoken: String
)

data class AgentResult(
    val videoUri: Uri?,
    val stickerUri: Uri?,
    val caption: AgentCaption,
    val shareIntent: Intent?,
    val previewBitmap: android.graphics.Bitmap?
)

data class AgentProgress(
    val steps: List<AgentStep>,
    val result: AgentResult? = null,
    val error: String? = null
)

enum class MusicMood { BIRTHDAY, CHILL, EPIC, ROMANTIC }

enum class MemeSound { BRUH, VINE_BOOM, LAUGH, WOW, SAD }


enum class AvatarStyle(@androidx.annotation.StringRes val labelRes: Int) {
    CARTOON(com.pixijoy.ai.R.string.avatar_cartoon),
    ANIME(com.pixijoy.ai.R.string.avatar_anime),
    SKETCH(com.pixijoy.ai.R.string.avatar_sketch),
    GLOW(com.pixijoy.ai.R.string.avatar_glow)
}

data class TrendItem(
    val id: String,
    val emoji: String,
    @androidx.annotation.StringRes val titleRes: Int = 0,
    @androidx.annotation.StringRes val descRes: Int = 0,
    val prompt: String,
    val tool: PhotoTool,
    val title: String? = null,
    val description: String? = null
)
