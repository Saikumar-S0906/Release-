package com.pixijoy.ai.domain.model

import androidx.annotation.StringRes
import com.pixijoy.ai.R

enum class CreationType(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val tool: PhotoTool
) {
    MEME("😂", R.string.creation_meme, R.string.creation_meme_desc, PhotoTool.MEME_MAKER),
    STICKER("😀", R.string.creation_sticker, R.string.creation_sticker_desc, PhotoTool.STICKER_STUDIO),
    GIF("🎬", R.string.creation_gif, R.string.creation_gif_desc, PhotoTool.GIF_MAKER),
    STORY("📱", R.string.creation_story, R.string.creation_story_desc, PhotoTool.STORY_CARD),
    REEL("🎥", R.string.creation_reel, R.string.creation_reel_desc, PhotoTool.AI_REELS),
    PROFILE("🖼️", R.string.creation_profile, R.string.creation_profile_desc, PhotoTool.PROFILE_PIC),
    ENHANCE("✨", R.string.creation_enhance, R.string.creation_enhance_desc, PhotoTool.ENHANCE),
    REMOVE_BG("🪄", R.string.creation_remove_bg, R.string.creation_remove_bg_desc, PhotoTool.BACKGROUND_REMOVE),
    VIRAL_PACK("🔥", R.string.creation_viral_pack, R.string.creation_viral_pack_desc, PhotoTool.VIRAL_PACK),
    QUOTE("💬", R.string.creation_quote, R.string.creation_quote_desc, PhotoTool.QUOTE_CARD),
    WA_STICKER("💚", R.string.creation_wa_sticker, R.string.creation_wa_sticker_desc, PhotoTool.WHATSAPP_STICKER),
    EDIT("📸", R.string.creation_edit, R.string.creation_edit_desc, PhotoTool.PHOTO_EDIT)
}

enum class CaptionTone(@StringRes val labelRes: Int) {
    ENGLISH(R.string.tone_english),
    HINDI(R.string.tone_hindi),
    TELUGU(R.string.tone_telugu),
    GENZ(R.string.tone_genz),
    SAVAGE(R.string.tone_savage),
    OFFICE(R.string.tone_office),
    DAD_JOKE(R.string.tone_dad_joke)
}

data class ImageContext(
    val labels: List<String> = emptyList(),
    val faceCount: Int = 0,
    val isSelfie: Boolean = false,
    val isGroup: Boolean = false,
    val isOutdoor: Boolean = false,
    val isFood: Boolean = false,
    val isBirthday: Boolean = false,
    val isPet: Boolean = false
)

data class SmartSuggestion(
    val creationType: CreationType,
    val reason: String,
    val priority: Int
)

data class NlEditResult(
    val action: NlAction,
    val message: String
)

enum class NlAction {
    ENHANCE,
    REMOVE_BACKGROUND,
    REPLACE_BACKGROUND,
    MAKE_MEME,
    MAKE_FUNNY,
    MAKE_STICKER,
    PORTRAIT_BLUR,
    BRIGHTEN,
    DARKEN,
    OBJECT_REMOVE,
    CREATE_REEL,
    PORTRAIT_STUDIO,
    GENERATE_VARIATIONS,
    ADVANCED_EDIT,
    UNKNOWN
}

enum class PhotoFilter(@StringRes val labelRes: Int) {
    NONE(R.string.filter_none),
    VIVID(R.string.filter_vivid),
    WARM(R.string.filter_warm),
    COOL(R.string.filter_cool),
    BW(R.string.filter_bw),
    CINEMATIC(R.string.filter_cinematic),
    VINTAGE(R.string.filter_vintage),
    PORTRAIT(R.string.filter_portrait),
    AI_GLOW(R.string.filter_ai_glow),
    AI_HDR(R.string.filter_ai_hdr)
}

data class StickerEmotion(
    val emoji: String,
    @StringRes val labelRes: Int
)

object StickerEmotions {
    val pack = listOf(
        StickerEmotion("😀", R.string.emotion_happy),
        StickerEmotion("😂", R.string.emotion_laughing),
        StickerEmotion("😡", R.string.emotion_angry),
        StickerEmotion("😭", R.string.emotion_crying),
        StickerEmotion("😍", R.string.emotion_love),
        StickerEmotion("😎", R.string.emotion_cool),
        StickerEmotion("🤔", R.string.emotion_thinking),
        StickerEmotion("🥳", R.string.emotion_party),
        StickerEmotion("🤣", R.string.emotion_rofl),
        StickerEmotion("😴", R.string.emotion_sleeping),
        StickerEmotion("🙏", R.string.emotion_thanks),
        StickerEmotion("👍", R.string.emotion_good)
    )
}
