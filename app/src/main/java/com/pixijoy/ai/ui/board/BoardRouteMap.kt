package com.pixijoy.ai.ui.board

import com.pixijoy.ai.domain.model.PhotoTool

/**
 * Maps legacy ui/flow route keys and PhotoTool IDs onto board screenIds.
 * Production navigation uses these IDs exclusively.
 */
object BoardRouteMap {
    fun createIntent(intent: String): String = when (intent) {
        "beautiful" -> "b5_choose"
        "funny" -> "b6_choose"
        "different" -> "b_diff_choose"
        "move" -> "b14_input"
        "tell" -> "b7_sheet"
        else -> "b3_chooser"
    }

    fun media(flow: String): String = when (flow) {
        "move" -> "b14_input"
        "editor" -> "b15_open"
        "collage" -> "b16_select"
        "export" -> "b17_open"
        else -> "b13_photo"
    }

    fun library(flow: String): String = when (flow) {
        "collaborate" -> "b_collab_select"
        "creations" -> "b18_browse"
        "search" -> "b2_search"
        else -> "b8_share"
    }

    fun me(flow: String): String = when (flow) {
        "pro" -> "b10_discover"
        "settings" -> "b11_hub"
        "safety" -> "b20_input"
        "privacy" -> "b21_hub"
        "auth" -> "b22_welcome"
        "credits" -> "b23_dash"
        "subscription" -> "b24_plans"
        "notifications" -> "b25_center"
        else -> "b9_open"
    }

    fun social(flow: String): String = when (flow) {
        "community" -> "b27_feed"
        "offline" -> "b28_banner"
        "a11y" -> "b29_home"
        "lifecycle" -> "b30_home"
        else -> "b12_intro"
    }

    fun createExtra(tool: String): String = when (tool) {
        "empty_home" -> "b_c00_empty"
        "restore" -> "b_c05"
        "face_swap" -> "b_c06"
        "scene" -> "b_c07"
        "people" -> "b_c08"
        "portrait" -> "b_c09"
        "expand" -> "b_c10"
        "talking" -> "b_c11"
        "greeting" -> "b_c12"
        "remix" -> "b_c14"
        else -> "b_c04"
    }

    /** Legacy [PhotoTool] routes → board screen IDs (Phase 3). */
    fun photoTool(tool: PhotoTool): String = when (tool) {
        PhotoTool.CREATE_FOR_ME -> "b7_sheet"
        PhotoTool.PHOTO_EDIT, PhotoTool.ADVANCED_EDIT -> "b15_open"
        PhotoTool.MEME_MAKER, PhotoTool.REACTION_CARD -> "b6_choose"
        PhotoTool.STICKER_STUDIO, PhotoTool.WHATSAPP_STICKER -> "b13_photo"
        PhotoTool.AI_REELS, PhotoTool.GIF_MAKER -> "b14_input"
        PhotoTool.PORTRAIT_STUDIO, PhotoTool.PORTRAIT_BLUR, PhotoTool.ID_PHOTO, PhotoTool.PROFILE_PIC ->
            "b_c09"
        PhotoTool.AI_VARIATIONS -> "b_diff_choose"
        PhotoTool.BACKGROUND_REMOVE, PhotoTool.AI_ERASE -> "b_c04"
        PhotoTool.BACKGROUND_REPLACE -> "b_c07"
        PhotoTool.ENHANCE, PhotoTool.VIRAL_PACK -> "b5_choose"
        PhotoTool.QUOTE_CARD, PhotoTool.STORY_CARD -> "b_c12"
    }
}
