package com.pixijoy.ai.ui.board.social

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardFlowArt

internal enum class CommunityCat {
    All,
    Ai,
    Pets,
    Nature,
    People,
    Art,
    Quotes,
    Travel,
    Food,
    Festivals,
    Love,
    Minimal,
    Funny
}

internal data class CommunityPost(
    val id: Int,
    @DrawableRes val art: Int,
    @StringRes val titleRes: Int,
    val likes: Int,
    val recency: Int,
    val editorPick: Boolean,
    val cats: Set<CommunityCat>
)

internal fun communityCatalog(): List<CommunityPost> = listOf(
    CommunityPost(0, BoardFlowArt.discoverFeatured, R.string.board_f2_featured_title, 12400, 80, true, setOf(CommunityCat.Ai, CommunityCat.Art)),
    CommunityPost(1, BoardFlowArt.differentHero, R.string.board_diff_style_anime, 9100, 70, true, setOf(CommunityCat.Ai, CommunityCat.Art)),
    CommunityPost(2, BoardFlowArt.styleFantasy, R.string.board_diff_style_fantasy, 8800, 95, true, setOf(CommunityCat.Ai, CommunityCat.Art)),
    CommunityPost(3, BoardFlowArt.stylePixel, R.string.board_diff_style_pixel, 6400, 92, false, setOf(CommunityCat.Ai, CommunityCat.Art, CommunityCat.Funny)),
    CommunityPost(4, BoardFlowArt.stickerPets, R.string.board_f2_item_pets, 10200, 90, false, setOf(CommunityCat.Pets, CommunityCat.Funny)),
    CommunityPost(5, R.drawable.board_c00_funny_dog, R.string.board_f2_item_pets, 7600, 60, false, setOf(CommunityCat.Pets, CommunityCat.Funny)),
    CommunityPost(6, BoardFlowArt.stickerHero, R.string.board_f13_create_sticker, 5400, 50, false, setOf(CommunityCat.Pets, CommunityCat.People)),
    CommunityPost(7, BoardFlowArt.trendTravel, R.string.board_f2_item_travel_vibes, 11100, 75, true, setOf(CommunityCat.Travel, CommunityCat.Nature)),
    CommunityPost(8, R.drawable.board_c07_mountains, R.string.board_f2_item_travel, 8200, 55, false, setOf(CommunityCat.Travel, CommunityCat.Nature)),
    CommunityPost(9, R.drawable.board_c07_beach, R.string.board_f2_item_travel, 6900, 48, false, setOf(CommunityCat.Travel, CommunityCat.Nature)),
    CommunityPost(10, R.drawable.board_c07_city, R.string.board_f2_item_city, 7300, 88, false, setOf(CommunityCat.Travel)),
    CommunityPost(11, BoardFlowArt.trendPortrait, R.string.board_f2_item_ai_portraits, 9800, 72, true, setOf(CommunityCat.People, CommunityCat.Ai, CommunityCat.Love)),
    CommunityPost(12, R.drawable.board_c09_portrait, R.string.board_f2_item_love, 8700, 65, false, setOf(CommunityCat.People, CommunityCat.Love)),
    CommunityPost(13, R.drawable.board_c08_people, R.string.board_f27_cat_people, 4100, 40, false, setOf(CommunityCat.People)),
    CommunityPost(14, BoardFlowArt.styleOil, R.string.board_diff_style_oil, 5200, 85, true, setOf(CommunityCat.Art)),
    CommunityPost(15, BoardFlowArt.styleWatercolor, R.string.board_diff_style_watercolor, 4700, 84, false, setOf(CommunityCat.Art, CommunityCat.Minimal)),
    CommunityPost(16, BoardFlowArt.styleSketch, R.string.board_diff_style_sketch, 3900, 83, false, setOf(CommunityCat.Art, CommunityCat.Minimal)),
    CommunityPost(17, BoardFlowArt.memeGoodVibes, R.string.board_f2_template_title, 15000, 45, false, setOf(CommunityCat.Quotes, CommunityCat.Funny)),
    CommunityPost(18, BoardFlowArt.memeMonday, R.string.board_f2_meme_monday, 9900, 35, false, setOf(CommunityCat.Quotes, CommunityCat.Funny)),
    CommunityPost(19, BoardFlowArt.funnyHero, R.string.board_f2_meme_expectation, 8100, 58, false, setOf(CommunityCat.Quotes, CommunityCat.Funny)),
    CommunityPost(20, BoardFlowArt.stickerFunny, R.string.board_f2_item_funny_memes, 6100, 93, false, setOf(CommunityCat.Funny)),
    CommunityPost(21, R.drawable.board_c12_card, R.string.board_f2_item_diwali, 7700, 78, false, setOf(CommunityCat.Festivals, CommunityCat.People)),
    CommunityPost(22, BoardFlowArt.festiveChristmas, R.string.board_f2_item_christmas, 8600, 94, true, setOf(CommunityCat.Festivals)),
    CommunityPost(23, BoardFlowArt.festiveHoli, R.string.board_f2_item_holi, 7200, 89, false, setOf(CommunityCat.Festivals)),
    CommunityPost(24, BoardFlowArt.festiveHalloween, R.string.board_f2_item_halloween, 6800, 91, false, setOf(CommunityCat.Festivals, CommunityCat.Funny)),
    CommunityPost(25, BoardFlowArt.stickerLove, R.string.board_f2_item_love, 9300, 87, false, setOf(CommunityCat.Love)),
    CommunityPost(26, BoardFlowArt.templateFamily, R.string.board_f2_item_family, 5600, 97, false, setOf(CommunityCat.Love, CommunityCat.People)),
    CommunityPost(27, R.drawable.board_c07_cafe, R.string.board_diff_style_vintage, 3300, 30, false, setOf(CommunityCat.Minimal, CommunityCat.Art)),
    CommunityPost(28, R.drawable.board_c07_studio, R.string.board_diff_style_line, 2800, 82, false, setOf(CommunityCat.Minimal)),
    CommunityPost(29, BoardFlowArt.collageHero, R.string.board_f2_cat_aesthetic, 4400, 76, false, setOf(CommunityCat.Art, CommunityCat.Minimal))
)

internal fun communityLabel(cat: CommunityCat): Int = when (cat) {
    CommunityCat.All -> R.string.board_f27_cat_all
    CommunityCat.Ai -> R.string.board_f27_cat_ai
    CommunityCat.Pets -> R.string.board_f27_cat_pets
    CommunityCat.Nature -> R.string.board_f27_cat_nature
    CommunityCat.People -> R.string.board_f27_cat_people
    CommunityCat.Art -> R.string.board_f27_cat_art
    CommunityCat.Quotes -> R.string.board_f27_cat_quotes
    CommunityCat.Travel -> R.string.board_f27_cat_travel
    CommunityCat.Food -> R.string.board_f27_cat_food
    CommunityCat.Festivals -> R.string.board_f27_cat_festivals
    CommunityCat.Love -> R.string.board_f27_cat_love
    CommunityCat.Minimal -> R.string.board_f27_cat_minimal
    CommunityCat.Funny -> R.string.board_f27_cat_funny
}

internal fun communityFromIndex(index: Int): CommunityCat =
    CommunityCat.entries.getOrElse(index) { CommunityCat.All }

/**
 * @param sort 0 trending, 1 most liked, 2 recent, 3 editors
 */
internal fun communityFilter(cat: CommunityCat, sort: Int): List<CommunityPost> {
    val base = communityCatalog().filter { cat == CommunityCat.All || cat in it.cats }
    return when (sort) {
        1 -> base.sortedByDescending { it.likes }
        2 -> base.sortedByDescending { it.recency }
        3 -> base.filter { it.editorPick }.sortedByDescending { it.likes }
        else -> base.sortedByDescending { it.likes / 100 + it.recency }
    }
}
