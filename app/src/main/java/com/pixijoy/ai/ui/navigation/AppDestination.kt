package com.pixijoy.ai.ui.navigation

import android.net.Uri
import com.pixijoy.ai.domain.model.PhotoTool

sealed class AppDestination(val route: String) {
    data object Onboarding : AppDestination("onboarding")
    data object Home : AppDestination("home")
    data object Privacy : AppDestination("privacy")
    data object Terms : AppDestination("terms")
    data object Settings : AppDestination("settings")

    /** Debug hubs (Settings → Debug tools when BuildConfig.DEBUG). */
    data object FlowIndex : AppDestination("flow_index")
    data object ResponsiveQa : AppDestination("responsive_qa")
    data object BoardUiIndex : AppDestination("board_ui_index")

    /** Canonical product destination space. */
    data object BoardScreen : AppDestination("board_ui/{screenId}") {
        const val SCREEN_ARG = "screenId"
        fun createRoute(screenId: String) = "board_ui/$screenId"
    }

    // --- Legacy deep-link redirects (Phase 3) → board via PixiJoyAppRoot ---
    data object Editor : AppDestination("editor/{toolId}") {
        fun createRoute(tool: PhotoTool) = "editor/${tool.id}"
        const val TOOL_ARG = "toolId"
    }
    data object CreativeStudio : AppDestination("creative/{toolId}") {
        fun createRoute(tool: PhotoTool) = "creative/${tool.id}"
        const val TOOL_ARG = "toolId"
    }
    data object CreateForMe : AppDestination("create_for_me")
    data object PhotoEdit : AppDestination("photo_edit")
    data object MemeGenerator : AppDestination("meme_generator")
    data object StickerCreate : AppDestination("sticker_create")
    data object Pro : AppDestination("pro")
    data object V2Hub : AppDestination("v2_hub")
    data object AiReels : AppDestination("ai_reels")
    data object VideoEdit : AppDestination("video_edit")
    data object Avatar : AppDestination("avatar")
    data object TalkingPhoto : AppDestination("talking_photo")
    data object VoiceStudio : AppDestination("voice_studio")
    data object MusicStudio : AppDestination("music_studio")
    data object Community : AppDestination("community")
    data object CreativeAgent : AppDestination("creative_agent")
    data object AdvancedEditor : AppDestination("advanced_editor")
    data object PortraitStudio : AppDestination("portrait_studio")
    data object Variations : AppDestination("variations")
    data object Templates : AppDestination("templates")
    data object Share : AppDestination("share/{contentUri}") {
        fun createRoute(uri: Uri) = "share/${Uri.encode(uri.toString())}"
        const val URI_ARG = "contentUri"
    }
    data object AiResult : AppDestination("ai_result")
}
