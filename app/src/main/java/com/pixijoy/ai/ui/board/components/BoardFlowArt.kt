package com.pixijoy.ai.ui.board.components

import androidx.annotation.DrawableRes
import com.pixijoy.ai.R

/**
 * Shared L2/L3 image packs for rematch / media / library / social / trust hosts.
 * C-tools use [com.pixijoy.ai.ui.board.ctools.BoardToolArt] instead.
 *
 * Phase 4: denser unique cells + B3 AI-state vectors (design photo pack still pending).
 */
object BoardFlowArt {
    @DrawableRes val beautifulHero = R.drawable.board_b5_photo
    @DrawableRes val funnyHero = R.drawable.board_b6_meme
    @DrawableRes val differentHero = R.drawable.board_b_diff_style
    @DrawableRes val tellHero = R.drawable.board_c11_talk
    @DrawableRes val stickerHero = R.drawable.board_b13_sticker
    @DrawableRes val motionHero = R.drawable.board_b14_motion
    @DrawableRes val collageHero = R.drawable.board_b16_collage
    @DrawableRes val editorHero = R.drawable.board_b15_edit
    @DrawableRes val homeTemplate = R.drawable.board_b2_template
    @DrawableRes val homeHero = R.drawable.board_b2_hero
    /** Home intent tiles — distinct art from style/option packs (no duplicate heroes). */
    @DrawableRes val homeBeautifulTile = R.drawable.board_ba_after
    @DrawableRes val homeFunnyTile = R.drawable.board_c00_funny_dog
    @DrawableRes val homeDifferentTile = R.drawable.board_c14_remix
    @DrawableRes val homeMoveTile = R.drawable.board_poster_move
    @DrawableRes val cameraViewfinder = R.drawable.board_b3_camera
    @DrawableRes val discoverFeatured = R.drawable.board_b2_featured
    @DrawableRes val memeCategoryHero = R.drawable.board_b2_cat_memes
    @DrawableRes val memeGoodVibes = R.drawable.board_b2_meme_good_vibes
    @DrawableRes val memeMonday = R.drawable.board_b2_meme_monday
    @DrawableRes val trendPortrait = R.drawable.board_b2_trend_portrait
    @DrawableRes val trendTravel = R.drawable.board_b2_trend_travel
    @DrawableRes val stylePixel = R.drawable.board_f2_style_pixel
    @DrawableRes val styleSketch = R.drawable.board_f2_style_sketch
    @DrawableRes val styleFantasy = R.drawable.board_f2_style_fantasy
    @DrawableRes val styleOil = R.drawable.board_f2_style_oil
    @DrawableRes val styleWatercolor = R.drawable.board_f2_style_watercolor
    @DrawableRes val templateBirthday = R.drawable.board_f2_template_birthday
    @DrawableRes val templateFamily = R.drawable.board_f2_template_family
    @DrawableRes val festiveChristmas = R.drawable.board_f2_festive_christmas
    @DrawableRes val festiveHoli = R.drawable.board_f2_festive_holi
    @DrawableRes val festiveEid = R.drawable.board_f2_festive_eid
    @DrawableRes val festiveHalloween = R.drawable.board_f2_festive_halloween
    @DrawableRes val festiveNewYear = R.drawable.board_f2_festive_newyear
    @DrawableRes val stickerPets = R.drawable.board_f2_sticker_pets
    @DrawableRes val stickerFunny = R.drawable.board_f2_sticker_funny
    @DrawableRes val stickerLove = R.drawable.board_f2_sticker_love
    @DrawableRes val stickerBirthday = R.drawable.board_f2_sticker_birthday
    @DrawableRes val creationThumb = R.drawable.board_b18_creation
    @DrawableRes val feedThumb = R.drawable.board_b27_feed
    @DrawableRes val profileAvatar = R.drawable.board_b9_avatar
    @DrawableRes val proHero = R.drawable.board_b10_pro
    @DrawableRes val safetyHero = R.drawable.board_b20_safety
    @DrawableRes val privacyHero = R.drawable.board_b21_privacy
    @DrawableRes val creditsHero = R.drawable.board_b23_credits
    @DrawableRes val offlineHero = R.drawable.board_b28_offline
    @DrawableRes val settingsHero = R.drawable.board_b11_settings
    @DrawableRes val notifHero = R.drawable.board_b25_notif
    @DrawableRes val posterMove = R.drawable.board_poster_move
    @DrawableRes val baBefore = R.drawable.board_ba_before
    @DrawableRes val baAfter = R.drawable.board_ba_after
    /** Transparent mark (no baked navy tile) for light/dark chrome. */
    @DrawableRes val logo = R.drawable.board_logo_pixijoy_clear

    /** B3 AI-state illustrations (vector stubs until design photo pack). */
    @DrawableRes val aiAnalyzing = R.drawable.board_b3_ai_analyzing
    @DrawableRes val aiSuccess = R.drawable.board_b3_ai_success
    @DrawableRes val aiFail = R.drawable.board_b3_ai_fail
    @DrawableRes val aiEmpty = R.drawable.board_b3_ai_empty
    @DrawableRes val aiBusy = R.drawable.board_b3_ai_busy
    @DrawableRes val imageFail = R.drawable.board_image_fail

    val homeRecents = listOf(
        creationThumb,
        beautifulHero,
        funnyHero,
        stickerHero,
        trendPortrait,
        R.drawable.board_c07_festival
    )

    val discoverCategories = listOf(
        R.string.board_f2_item_ai_portraits to trendPortrait,
        R.string.board_f2_item_funny_memes to funnyHero,
        R.string.board_f2_cat_stickers to stickerHero,
        R.string.board_f2_item_travel to trendTravel,
        R.string.board_f2_item_festive to R.drawable.board_c07_festival,
        R.string.board_f2_item_love to R.drawable.board_c09_portrait,
        R.string.board_f2_cat_movies to motionHero,
        R.string.board_f2_cat_cartoon to differentHero,
        R.string.board_f2_cat_aesthetic to collageHero
    )

    val memeTemplates = listOf(
        memeGoodVibes,
        memeMonday,
        funnyHero,
        R.drawable.board_c00_funny_dog,
        memeCategoryHero,
        baAfter,
        R.drawable.board_c12_card,
        stickerHero
    )

    val trendingCards = listOf(
        trendPortrait,
        funnyHero,
        trendTravel,
        R.drawable.board_c07_city,
        discoverFeatured
    )

    /** Unique Beautiful style cells — no repeated hero as “different” styles. */
    val rematchBeautiful = listOf(
        beautifulHero,
        R.drawable.board_c09_portrait,
        baAfter,
        R.drawable.board_c07_beach,
        trendPortrait,
        R.drawable.board_c07_studio,
        R.drawable.board_c05_after,
        R.drawable.board_c04_cutout
    )

    val rematchFunny = listOf(
        funnyHero,
        R.drawable.board_c00_funny_dog,
        memeGoodVibes,
        memeMonday,
        memeCategoryHero,
        R.drawable.board_c12_card,
        stickerHero,
        baAfter
    )

    val rematchDifferent = listOf(
        differentHero,
        R.drawable.board_c14_remix,
        R.drawable.board_c07_scene,
        R.drawable.board_c07_space,
        R.drawable.board_c07_festival,
        R.drawable.board_c11_talk,
        motionHero,
        R.drawable.board_c08_people
    )

    val rematchTell = listOf(
        tellHero,
        posterMove,
        R.drawable.board_c09_portrait,
        motionHero,
        R.drawable.board_c06_faces,
        R.drawable.board_c00_hero
    )

    val mediaThumbs = listOf(
        stickerHero,
        motionHero,
        collageHero,
        editorHero,
        posterMove,
        baAfter,
        R.drawable.board_c04_subject,
        R.drawable.board_c10_expand
    )

    val libraryThumbs = listOf(
        creationThumb,
        beautifulHero,
        funnyHero,
        stickerHero,
        motionHero,
        collageHero,
        feedThumb,
        R.drawable.board_c07_beach,
        R.drawable.board_c09_portrait,
        R.drawable.board_c14_remix,
        trendTravel,
        R.drawable.board_c07_mountains
    )

    val feedThumbs = listOf(
        feedThumb,
        creationThumb,
        beautifulHero,
        funnyHero,
        stickerHero,
        R.drawable.board_c00_hero,
        R.drawable.board_c07_festival,
        trendPortrait,
        trendTravel,
        R.drawable.board_c14_remix,
        collageHero,
        R.drawable.board_c09_portrait
    )

    /** Flow 12 journey map milestone thumbs (8 stops). */
    val journeyStops = listOf(
        logo,
        profileAvatar,
        homeHero,
        beautifulHero,
        editorHero,
        creationThumb,
        feedThumb,
        proHero
    )

    /** Distinct creator row faces for community feed. */
    val creatorFaces = listOf(
        profileAvatar,
        R.drawable.board_c00_funny_dog,
        trendPortrait,
        R.drawable.board_c09_portrait,
        funnyHero,
        R.drawable.board_c06_faces
    )

    val sceneThumbs = listOf(
        R.drawable.board_c07_beach,
        R.drawable.board_c07_city,
        R.drawable.board_c07_mountains,
        R.drawable.board_c07_cafe,
        R.drawable.board_c07_studio,
        R.drawable.board_c07_space,
        R.drawable.board_c07_festival,
        R.drawable.board_c07_scene
    )

    /** F19 / SoftEdge recovery illustrations. */
    val f19Recovery = listOf(
        aiAnalyzing,
        aiSuccess,
        aiFail,
        aiBusy,
        offlineHero,
        safetyHero,
        creditsHero,
        aiEmpty
    )
}
