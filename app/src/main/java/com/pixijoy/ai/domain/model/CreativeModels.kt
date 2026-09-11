package com.pixijoy.ai.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.pixijoy.ai.R

data class MemeTextStyle(
    val topText: String = "",
    val bottomText: String = "",
    val textColor: Color = Color.White,
    val strokeColor: Color = Color.Black,
    val fontScale: Float = 1f
)

data class StickerPlacement(
    val emoji: String,
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float = 1f
)

enum class SocialFormat(
    val id: String,
    @StringRes val labelRes: Int,
    val aspectWidth: Float,
    val aspectHeight: Float,
    val platform: String
) {
    ORIGINAL("original", R.string.format_original, 0f, 0f, "Any"),
    INSTAGRAM_STORY("ig_story", R.string.format_ig_story, 9f, 16f, "Instagram"),
    INSTAGRAM_POST("ig_post", R.string.format_ig_post, 1f, 1f, "Instagram"),
    WHATSAPP_STATUS("whatsapp", R.string.format_whatsapp, 9f, 16f, "WhatsApp"),
    SNAPCHAT("snapchat", R.string.format_snapchat, 9f, 16f, "Snapchat"),
    TIKTOK("tiktok", R.string.format_tiktok, 9f, 16f, "TikTok");

    val aspectRatio: Float?
        get() = if (aspectWidth == 0f) null else aspectWidth / aspectHeight
}

data class MemeTemplate(
    val id: String,
    val name: String,
    val topText: String,
    val bottomText: String,
    val emoji: String
)

data class DailyChallenge(
    val title: String,
    val description: String,
    val template: MemeTemplate,
    val rewardCredits: Int = 1,
    val hashtag: String
)

object MemeTemplates {
    val trending = listOf(
        MemeTemplate("nobody", "Nobody Meme", "NOBODY:", "ME:", "😐"),
        MemeTemplate("pov", "POV", "POV:", "You finally removed the background", "👀"),
        MemeTemplate("drake", "Drake Style", "Editing in Photoshop", "PixiJoy AI in 1 tap", "🔥"),
        MemeTemplate("brain", "Galaxy Brain", "Paying for Photoshop", "Free AI edits", "🧠"),
        MemeTemplate("stonks", "Stonks", "Manual editing", "AI auto enhance", "📈"),
        MemeTemplate("this_is_fine", "This Is Fine", "Bad photo quality", "After PixiJoy", "☕"),
        MemeTemplate("success", "Success Kid", "Before coffee", "After PixiJoy edit", "💪"),
        MemeTemplate("reaction", "Reaction", "When the AI", "Actually works", "😱")
    )

    val india = listOf(
        MemeTemplate("exam", "Exam Mood", "EXAM KAL HAI", "ABHI BHI MEME BANA RAHA", "📚"),
        MemeTemplate("cricket", "Cricket Fan", "MATCH CHAL RAHA HAI", "MEME BHI BANANA HAI", "🏏"),
        MemeTemplate("chai", "Chai Lover", "CHAI KE BINA", "KAAM NAHI HOTA", "☕"),
        MemeTemplate("whatsapp", "WhatsApp Uncle", "GOOD MORNING", "FORWARDED 47 TIMES", "👴"),
        MemeTemplate("budget", "Budget Life", "SALARY AAYI", "KHARCHA HO GAYI", "💸"),
        MemeTemplate("desi", "Desi Parents", "BETA ENGINEER BANO", "ME: MEME BANATA HU", "🇮🇳"),
        MemeTemplate("wedding", "Wedding Season", "SHADI KAB HAI", "JAB APP VIRAL HO", "💒"),
        MemeTemplate("monday", "Monday Blues", "MONDAY MORNING", "FRIDAY NIGHT VIBES", "😴")
    )

    val all: List<MemeTemplate> = trending + india
}

object StickerPacks {
    val reactions = listOf("😂", "🔥", "💀", "😍", "🤯", "👑", "💯", "✨", "🎉", "😎", "🥶", "🫡", "💅", "🤡", "👀", "❤️‍🔥")
    val meme = listOf("💀", "🗿", "🤡", "👁️", "🫠", "🧢", "📸", "🎭", "🚀", "⚡", "🎯", "💣", "🍿", "🎪", "🦄", "🌈")
    val social = listOf("📲", "📸", "🎬", "🎵", "💬", "📤", "🔔", "⭐", "🏆", "💎", "🎁", "🎊", "🤳", "📱", "🌍", "🔗")
}
