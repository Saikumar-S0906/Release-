package com.pixijoy.ai.ui.flow.extra

import androidx.annotation.StringRes
import com.pixijoy.ai.R

/**
 * C-series Create Tools (additional_flow).
 * Navigate with [com.pixijoy.ai.ui.board.BoardRouteMap.createExtra] → `board_ui/{id}`.
 */
enum class CreateExtraTool(
    val routeKey: String,
    @StringRes val titleRes: Int,
    /** Catalog flow number shown in Flow Index (C00 → 100, C04 → 104, …). */
    val catalogFlow: Int
) {
    EmptyHome("empty_home", R.string.create_extra_empty_home, 100),
    RemoveBg("remove_bg", R.string.create_extra_remove_bg, 104),
    Restore("restore", R.string.create_extra_restore, 105),
    FaceSwap("face_swap", R.string.create_extra_face_swap, 106),
    Scene("scene", R.string.create_extra_scene, 107),
    People("people", R.string.create_extra_people, 108),
    Portrait("portrait", R.string.create_extra_portrait, 109),
    Expand("expand", R.string.create_extra_expand, 110),
    Talking("talking", R.string.create_extra_talking, 111),
    Greeting("greeting", R.string.create_extra_greeting, 112),
    Remix("remix", R.string.create_extra_remix, 114);

    companion object {
        /** Tools shown on Create hub (not C00 empty-home / C14 remix). */
        val createHubTools: List<CreateExtraTool> = listOf(
            RemoveBg, Restore, FaceSwap, Scene, People, Portrait, Expand, Talking, Greeting
        )

        fun fromRoute(key: String): CreateExtraTool =
            entries.find { it.routeKey == key } ?: RemoveBg
    }
}
