package com.pixijoy.ai.domain.model

import androidx.annotation.DrawableRes

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixijoy.ai.R

enum class PhotoTool(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val icon: Int,
    val category: ToolCategory,
    val isPro: Boolean = false,
    val isCreativeStudio: Boolean = false
) {
    VIRAL_PACK(
        id = "viral_pack",
        titleRes = R.string.tool_viral_pack,
        descriptionRes = R.string.tool_viral_pack_desc,
        icon = R.drawable.pj_ic_sparkles,
        category = ToolCategory.VIRAL_CREATE,
        isCreativeStudio = true
    ),
    MEME_MAKER(
        id = "meme_maker",
        titleRes = R.string.tool_meme_maker,
        descriptionRes = R.string.tool_meme_maker_desc,
        icon = R.drawable.pj_ic_text,
        category = ToolCategory.VIRAL_CREATE,
        isCreativeStudio = true
    ),
    QUOTE_CARD(
        id = "quote_card",
        titleRes = R.string.tool_quote_card,
        descriptionRes = R.string.tool_quote_card_desc,
        icon = R.drawable.pj_ic_quote,
        category = ToolCategory.VIRAL_CREATE,
        isCreativeStudio = true
    ),
    PROFILE_PIC(
        id = "profile_pic",
        titleRes = R.string.tool_profile_pic,
        descriptionRes = R.string.tool_profile_pic_desc,
        icon = R.drawable.pj_ic_account,
        category = ToolCategory.VIRAL_CREATE,
        isCreativeStudio = true
    ),
    WHATSAPP_STICKER(
        id = "wa_sticker",
        titleRes = R.string.tool_wa_sticker,
        descriptionRes = R.string.tool_wa_sticker_desc,
        icon = R.drawable.pj_ic_bolt,
        category = ToolCategory.SOCIAL_EXPORT,
        isCreativeStudio = true
    ),
    STICKER_STUDIO(
        id = "sticker_studio",
        titleRes = R.string.tool_sticker_studio,
        descriptionRes = R.string.tool_sticker_studio_desc,
        icon = R.drawable.pj_ic_gallery,
        category = ToolCategory.VIRAL_CREATE,
        isCreativeStudio = true
    ),
    GIF_MAKER(
        id = "gif_maker",
        titleRes = R.string.tool_gif_maker,
        descriptionRes = R.string.tool_gif_maker_desc,
        icon = R.drawable.pj_ic_gif,
        category = ToolCategory.VIRAL_CREATE,
        isCreativeStudio = true
    ),
    AI_REELS(
        id = "ai_reels",
        titleRes = R.string.tool_ai_reels,
        descriptionRes = R.string.tool_ai_reels_desc,
        icon = R.drawable.pj_ic_movie,
        category = ToolCategory.VIRAL_CREATE
    ),
    STORY_CARD(
        id = "story_card",
        titleRes = R.string.tool_story_card,
        descriptionRes = R.string.tool_story_card_desc,
        icon = R.drawable.pj_ic_share,
        category = ToolCategory.SOCIAL_EXPORT,
        isCreativeStudio = true
    ),
    REACTION_CARD(
        id = "reaction_card",
        titleRes = R.string.tool_reaction_card,
        descriptionRes = R.string.tool_reaction_card_desc,
        icon = R.drawable.pj_ic_flash,
        category = ToolCategory.SOCIAL_EXPORT,
        isCreativeStudio = true
    ),
    BACKGROUND_REMOVE(
        id = "bg_remove",
        titleRes = R.string.tool_bg_remove,
        descriptionRes = R.string.tool_bg_remove_desc,
        icon = R.drawable.pj_ic_layers,
        category = ToolCategory.AI_STUDIO
    ),
    ENHANCE(
        id = "enhance",
        titleRes = R.string.tool_enhance,
        descriptionRes = R.string.tool_enhance_desc,
        icon = R.drawable.pj_ic_magic,
        category = ToolCategory.AI_STUDIO
    ),
    PORTRAIT_BLUR(
        id = "portrait_blur",
        titleRes = R.string.tool_portrait_blur,
        descriptionRes = R.string.tool_portrait_blur_desc,
        icon = R.drawable.pj_ic_blur,
        category = ToolCategory.AI_STUDIO
    ),
    BACKGROUND_REPLACE(
        id = "bg_replace",
        titleRes = R.string.tool_bg_replace,
        descriptionRes = R.string.tool_bg_replace_desc,
        icon = R.drawable.pj_ic_palette,
        category = ToolCategory.AI_STUDIO,
        isPro = true
    ),
    ID_PHOTO(
        id = "id_photo",
        titleRes = R.string.tool_id_photo,
        descriptionRes = R.string.tool_id_photo_desc,
        icon = R.drawable.pj_ic_badge,
        category = ToolCategory.AI_STUDIO
    ),
    PHOTO_EDIT(
        id = "photo_edit",
        titleRes = R.string.tool_photo_edit,
        descriptionRes = R.string.tool_photo_edit_desc,
        icon = R.drawable.pj_ic_crop,
        category = ToolCategory.AI_STUDIO
    ),
    AI_ERASE(
        id = "ai_erase",
        titleRes = R.string.tool_ai_erase,
        descriptionRes = R.string.tool_ai_erase_desc,
        icon = R.drawable.pj_ic_heal,
        category = ToolCategory.AI_STUDIO
    ),
    PORTRAIT_STUDIO(
        id = "portrait_studio",
        titleRes = R.string.portrait_studio_title,
        descriptionRes = R.string.portrait_studio_desc,
        icon = R.drawable.pj_ic_face,
        category = ToolCategory.AI_STUDIO
    ),
    AI_VARIATIONS(
        id = "ai_variations",
        titleRes = R.string.variations_title,
        descriptionRes = R.string.variations_desc,
        icon = R.drawable.pj_ic_brush,
        category = ToolCategory.AI_STUDIO
    ),
    ADVANCED_EDIT(
        id = "advanced_edit",
        titleRes = R.string.advanced_editor_title,
        descriptionRes = R.string.advanced_editor_desc,
        icon = R.drawable.pj_ic_layers,
        category = ToolCategory.AI_STUDIO
    ),
    CREATE_FOR_ME(
        id = "create_for_me",
        titleRes = R.string.tool_create_for_me,
        descriptionRes = R.string.tool_create_for_me_desc,
        icon = R.drawable.pj_ic_sparkles,
        category = ToolCategory.VIRAL_CREATE
    );

    companion object {
        fun fromId(id: String): PhotoTool? = entries.find { it.id == id }

        fun byCategory(category: ToolCategory): List<PhotoTool> =
            entries.filter { it.category == category }
    }
}
