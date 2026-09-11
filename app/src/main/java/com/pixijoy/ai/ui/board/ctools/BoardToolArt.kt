package com.pixijoy.ai.ui.board.ctools

import androidx.annotation.DrawableRes
import com.pixijoy.ai.R

/**
 * Per-tool imagery for C04–C14.
 * [optionImages] aligns 1:1 with [BoardCTool.optionRes] when sized to match.
 */
data class BoardToolArt(
    @DrawableRes val hero: Int,
    @DrawableRes val result: Int,
    @DrawableRes val before: Int? = null,
    @DrawableRes val after: Int? = null,
    @DrawableRes val variationExtras: List<Int> = emptyList(),
    @DrawableRes val optionImages: List<Int> = emptyList()
)

fun BoardCTool.art(): BoardToolArt = when (this) {
    BoardCTool.RemoveBg -> BoardToolArt(
        hero = R.drawable.board_c04_subject,
        result = R.drawable.board_c04_beach,
        before = R.drawable.board_c04_subject,
        after = R.drawable.board_c04_cutout,
        variationExtras = listOf(
            R.drawable.board_c04_cutout,
            R.drawable.board_c04_beach,
            R.drawable.board_ba_after,
            R.drawable.board_c04_subject
        ),
        optionImages = listOf(
            R.drawable.board_c04_cutout,
            R.drawable.board_c04_beach,
            R.drawable.board_ba_after,
            R.drawable.board_c07_scene,
            R.drawable.board_c04_subject,
            R.drawable.board_b5_photo
        )
    )
    BoardCTool.Restore -> BoardToolArt(
        hero = R.drawable.board_c05_before,
        result = R.drawable.board_c05_after,
        before = R.drawable.board_c05_before,
        after = R.drawable.board_c05_after,
        variationExtras = listOf(
            R.drawable.board_c05_after,
            R.drawable.board_ba_after,
            R.drawable.board_c09_portrait,
            R.drawable.board_b5_photo
        ),
        optionImages = listOf(
            R.drawable.board_c05_before,
            R.drawable.board_c05_after,
            R.drawable.board_c09_portrait,
            R.drawable.board_c10_expand
        )
    )
    BoardCTool.FaceSwap -> BoardToolArt(
        hero = R.drawable.board_c06_faces,
        result = R.drawable.board_c06_faces,
        variationExtras = listOf(
            R.drawable.board_c09_portrait,
            R.drawable.board_ba_after,
            R.drawable.board_c08_people
        ),
        optionImages = listOf(
            R.drawable.board_c06_faces,
            R.drawable.board_c09_portrait,
            R.drawable.board_ba_after,
            R.drawable.board_c08_people
        )
    )
    BoardCTool.Scene -> BoardToolArt(
        hero = R.drawable.board_c07_scene,
        result = R.drawable.board_c07_beach,
        variationExtras = listOf(
            R.drawable.board_c07_beach,
            R.drawable.board_c07_city,
            R.drawable.board_c07_mountains,
            R.drawable.board_c07_cafe
        ),
        optionImages = listOf(
            R.drawable.board_c07_beach,
            R.drawable.board_c07_city,
            R.drawable.board_c07_mountains,
            R.drawable.board_c07_cafe,
            R.drawable.board_c07_studio,
            R.drawable.board_c07_space,
            R.drawable.board_c07_festival,
            R.drawable.board_c07_scene
        )
    )
    BoardCTool.People -> BoardToolArt(
        hero = R.drawable.board_c08_people,
        result = R.drawable.board_c08_people,
        variationExtras = listOf(
            R.drawable.board_c06_faces,
            R.drawable.board_c00_hero,
            R.drawable.board_c09_portrait
        ),
        optionImages = listOf(
            R.drawable.board_c08_people,
            R.drawable.board_c00_hero,
            R.drawable.board_c04_cutout,
            R.drawable.board_c06_faces
        )
    )
    BoardCTool.Portrait -> BoardToolArt(
        hero = R.drawable.board_c09_portrait,
        result = R.drawable.board_c09_portrait,
        variationExtras = listOf(
            R.drawable.board_ba_after,
            R.drawable.board_c06_faces,
            R.drawable.board_b5_photo
        ),
        optionImages = listOf(
            R.drawable.board_c09_portrait,
            R.drawable.board_b5_photo,
            R.drawable.board_ba_after,
            R.drawable.board_c06_faces,
            R.drawable.board_c05_after
        )
    )
    BoardCTool.Expand -> BoardToolArt(
        hero = R.drawable.board_c10_expand,
        result = R.drawable.board_c07_beach,
        before = R.drawable.board_c10_expand,
        after = R.drawable.board_c07_beach,
        variationExtras = listOf(
            R.drawable.board_c07_beach,
            R.drawable.board_c07_scene,
            R.drawable.board_c10_expand
        ),
        optionImages = listOf(
            R.drawable.board_c10_expand,
            R.drawable.board_c07_beach,
            R.drawable.board_c07_scene,
            R.drawable.board_c04_beach,
            R.drawable.board_c07_studio,
            R.drawable.board_b5_photo
        )
    )
    BoardCTool.Talking -> BoardToolArt(
        hero = R.drawable.board_c11_talk,
        result = R.drawable.board_c11_talk,
        variationExtras = listOf(
            R.drawable.board_poster_move,
            R.drawable.board_c09_portrait,
            R.drawable.board_b14_motion
        ),
        optionImages = listOf(
            R.drawable.board_c11_talk,
            R.drawable.board_b14_motion,
            R.drawable.board_c09_portrait
        )
    )
    BoardCTool.Greeting -> BoardToolArt(
        hero = R.drawable.board_c12_card,
        result = R.drawable.board_c12_card,
        variationExtras = listOf(
            R.drawable.board_c07_festival,
            R.drawable.board_c00_funny_dog,
            R.drawable.board_c07_scene
        ),
        optionImages = listOf(
            R.drawable.board_c12_card,
            R.drawable.board_c07_festival,
            R.drawable.board_c00_funny_dog,
            R.drawable.board_c07_scene,
            R.drawable.board_c07_city,
            R.drawable.board_b6_meme,
            R.drawable.board_c09_portrait,
            R.drawable.board_c07_space
        )
    )
    BoardCTool.Remix -> BoardToolArt(
        hero = R.drawable.board_c14_remix,
        result = R.drawable.board_c14_remix,
        variationExtras = listOf(
            R.drawable.board_c07_beach,
            R.drawable.board_c00_funny_dog,
            R.drawable.board_b27_feed
        ),
        optionImages = listOf(
            R.drawable.board_c14_remix,
            R.drawable.board_b_diff_style,
            R.drawable.board_c09_portrait,
            R.drawable.board_b5_photo
        )
    )
}
