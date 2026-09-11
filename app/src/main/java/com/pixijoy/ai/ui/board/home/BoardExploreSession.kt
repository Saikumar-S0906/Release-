package com.pixijoy.ai.ui.board.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardFlowArt

/** What happens when an Explore card is used. */
internal enum class ExploreAction {
    Template,
    Style,
    Sticker,
    Beautiful,
    Move
}

internal enum class ExplorePack {
    Templates,
    Styles,
    Stickers,
    Portraits,
    Memes,
    Travel,
    Festive,
    Love,
    Movies,
    Cartoon,
    Aesthetic
}

internal enum class ExploreTheme {
    Birthday, Love, Family, Festival, Travel, Pets, Funny, Aesthetic
}

internal enum class ExploreLook {
    Photo, Illustration, Cartoon, ThreeD, Watercolor
}

internal enum class ExploreSort {
    Popular, Trending, Latest
}

internal data class ExploreCard(
    @DrawableRes val art: Int,
    @StringRes val titleRes: Int,
    val action: ExploreAction,
    val styleIndex: Int = 0,
    val popularity: Int = 50,
    val recency: Int = 50,
    val trending: Int = 50,
    val themes: Set<ExploreTheme> = emptySet(),
    val look: ExploreLook = ExploreLook.Photo
)

internal data class ExploreSearchHit(
    val id: Int,
    val pack: ExplorePack,
    val card: ExploreCard
)

/**
 * Explore IA: chips and popular categories share this pack so titles, art,
 * and tap destinations stay aligned.
 */
internal object BoardExploreSession {
    var chip: Int = 0
    var pack: ExplorePack = ExplorePack.Memes
    @DrawableRes
    var selectedArt: Int = BoardFlowArt.discoverFeatured
    @StringRes
    var selectedTitleRes: Int = R.string.board_f2_featured_title
    var selectedStyleIndex: Int = 0
    var selectedAction: ExploreAction = ExploreAction.Template
    var returnId: String = "b2_discover"

    fun select(card: ExploreCard, returnTo: String = "b2_discover") {
        selectedArt = card.art
        selectedTitleRes = card.titleRes
        selectedStyleIndex = card.styleIndex
        selectedAction = card.action
        returnId = returnTo
    }

    fun selectFeatured() {
        pack = ExplorePack.Styles
        selectedArt = BoardFlowArt.discoverFeatured
        selectedTitleRes = R.string.board_f2_featured_title
        selectedAction = ExploreAction.Style
        selectedStyleIndex = 0
        returnId = "b2_discover"
    }

    fun featuredCard(): ExploreCard = ExploreCard(
        art = selectedArt,
        titleRes = selectedTitleRes,
        action = selectedAction,
        styleIndex = selectedStyleIndex
    )
}

internal fun exploreTitle(pack: ExplorePack): Int = when (pack) {
    ExplorePack.Templates -> R.string.flow2_chip_templates
    ExplorePack.Styles -> R.string.flow2_chip_styles
    ExplorePack.Stickers -> R.string.flow2_chip_stickers
    ExplorePack.Portraits -> R.string.board_f2_item_ai_portraits
    ExplorePack.Memes -> R.string.board_f2_item_funny_memes
    ExplorePack.Travel -> R.string.board_f2_item_travel
    ExplorePack.Festive -> R.string.board_f2_item_festive
    ExplorePack.Love -> R.string.board_f2_item_love
    ExplorePack.Movies -> R.string.board_f2_cat_movies
    ExplorePack.Cartoon -> R.string.board_f2_cat_cartoon
    ExplorePack.Aesthetic -> R.string.board_f2_cat_aesthetic
}

internal fun exploreSubtitle(pack: ExplorePack): Int = when (pack) {
    ExplorePack.Templates -> R.string.board_f2_cat_templates_sub
    ExplorePack.Styles -> R.string.board_f2_cat_styles_sub
    ExplorePack.Stickers -> R.string.board_f2_cat_stickers_sub
    ExplorePack.Portraits -> R.string.board_f2_cat_portraits_sub
    ExplorePack.Memes -> R.string.board_f2_cat_memes_sub
    ExplorePack.Travel -> R.string.board_f2_cat_travel_sub
    ExplorePack.Festive -> R.string.board_f2_cat_festive_sub
    ExplorePack.Love -> R.string.board_f2_cat_love_sub
    ExplorePack.Movies -> R.string.board_f2_cat_movies_sub
    ExplorePack.Cartoon -> R.string.board_f2_cat_cartoon_sub
    ExplorePack.Aesthetic -> R.string.board_f2_cat_aesthetic_sub
}

internal fun exploreHero(pack: ExplorePack): Int = when (pack) {
    ExplorePack.Templates -> BoardFlowArt.homeTemplate
    ExplorePack.Styles -> BoardFlowArt.differentHero
    ExplorePack.Stickers -> BoardFlowArt.stickerHero
    ExplorePack.Portraits -> BoardFlowArt.trendPortrait
    ExplorePack.Memes -> BoardFlowArt.memeCategoryHero
    ExplorePack.Travel -> BoardFlowArt.trendTravel
    ExplorePack.Festive -> R.drawable.board_c07_festival
    ExplorePack.Love -> R.drawable.board_c09_portrait
    ExplorePack.Movies -> BoardFlowArt.motionHero
    ExplorePack.Cartoon -> BoardFlowArt.differentHero
    ExplorePack.Aesthetic -> BoardFlowArt.collageHero
}

private fun ranked(
    art: Int,
    title: Int,
    action: ExploreAction,
    styleIndex: Int = 0,
    pop: Int,
    rec: Int,
    trend: Int,
    themes: Set<ExploreTheme> = emptySet(),
    look: ExploreLook = ExploreLook.Photo
) = ExploreCard(art, title, action, styleIndex, pop, rec, trend, themes, look)

internal fun exploreCards(pack: ExplorePack): List<ExploreCard> = when (pack) {
    ExplorePack.Templates -> listOf(
        ranked(BoardFlowArt.memeGoodVibes, R.string.board_f2_template_title, ExploreAction.Template, pop = 96, rec = 40, trend = 80, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.memeMonday, R.string.board_f2_meme_monday, ExploreAction.Template, pop = 82, rec = 30, trend = 70, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.funnyHero, R.string.board_f2_meme_expectation, ExploreAction.Template, pop = 78, rec = 35, trend = 86, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.memeCategoryHero, R.string.board_f2_item_funny_memes, ExploreAction.Template, pop = 70, rec = 22, trend = 60, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.templateBirthday, R.string.board_f2_item_birthday, ExploreAction.Template, pop = 88, rec = 92, trend = 95, themes = setOf(ExploreTheme.Birthday), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.templateFamily, R.string.board_f2_item_family, ExploreAction.Template, pop = 74, rec = 98, trend = 55, themes = setOf(ExploreTheme.Family, ExploreTheme.Love), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.collageHero, R.string.board_f2_cat_aesthetic, ExploreAction.Template, pop = 64, rec = 84, trend = 48, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Photo)
    )
    ExplorePack.Styles -> listOf(
        ranked(BoardFlowArt.differentHero, R.string.board_diff_style_anime, ExploreAction.Style, 0, pop = 94, rec = 60, trend = 90, look = ExploreLook.Cartoon),
        ranked(R.drawable.board_c14_remix, R.string.board_diff_style_3d, ExploreAction.Style, 1, pop = 80, rec = 70, trend = 88, look = ExploreLook.ThreeD),
        ranked(BoardFlowArt.styleOil, R.string.board_diff_style_oil, ExploreAction.Style, 2, pop = 72, rec = 50, trend = 40, look = ExploreLook.Illustration),
        ranked(BoardFlowArt.styleWatercolor, R.string.board_diff_style_watercolor, ExploreAction.Style, 3, pop = 76, rec = 88, trend = 58, look = ExploreLook.Watercolor),
        ranked(BoardFlowArt.styleSketch, R.string.board_diff_style_sketch, ExploreAction.Style, 4, pop = 68, rec = 86, trend = 52, look = ExploreLook.Illustration),
        ranked(BoardFlowArt.stylePixel, R.string.board_diff_style_pixel, ExploreAction.Style, 5, pop = 84, rec = 90, trend = 93, look = ExploreLook.Cartoon),
        ranked(BoardFlowArt.styleFantasy, R.string.board_diff_style_fantasy, ExploreAction.Style, 6, pop = 90, rec = 94, trend = 82, look = ExploreLook.Illustration),
        ranked(R.drawable.board_c07_space, R.string.board_diff_style_cyber, ExploreAction.Style, 7, pop = 62, rec = 44, trend = 74, look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_festival, R.string.board_diff_style_pop, ExploreAction.Style, 8, pop = 58, rec = 36, trend = 66, themes = setOf(ExploreTheme.Festival), look = ExploreLook.Cartoon),
        ranked(R.drawable.board_c07_cafe, R.string.board_diff_style_vintage, ExploreAction.Style, 9, pop = 54, rec = 28, trend = 34, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Photo)
    )
    ExplorePack.Stickers -> listOf(
        ranked(BoardFlowArt.stickerHero, R.string.board_f13_create_sticker, ExploreAction.Sticker, 0, pop = 90, rec = 55, trend = 70, look = ExploreLook.Photo),
        ranked(R.drawable.board_c04_cutout, R.string.board_f13_style_outline, ExploreAction.Sticker, 1, pop = 72, rec = 48, trend = 50, look = ExploreLook.Illustration),
        ranked(BoardFlowArt.stickerPets, R.string.board_f2_item_pets, ExploreAction.Sticker, 2, pop = 88, rec = 92, trend = 94, themes = setOf(ExploreTheme.Pets, ExploreTheme.Funny), look = ExploreLook.Cartoon),
        ranked(BoardFlowArt.stickerFunny, R.string.board_f2_item_funny_memes, ExploreAction.Sticker, 2, pop = 80, rec = 90, trend = 86, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Cartoon),
        ranked(BoardFlowArt.stickerLove, R.string.board_f2_item_love, ExploreAction.Sticker, 0, pop = 76, rec = 88, trend = 64, themes = setOf(ExploreTheme.Love), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.stickerBirthday, R.string.board_f2_item_birthday, ExploreAction.Sticker, 0, pop = 84, rec = 96, trend = 80, themes = setOf(ExploreTheme.Birthday), look = ExploreLook.Illustration)
    )
    ExplorePack.Portraits -> BoardFlowArt.rematchBeautiful.take(8).mapIndexed { i, art ->
        val titles = listOf(
            R.string.board_f2_item_ai_portraits,
            R.string.flow5_natural,
            R.string.flow5_vivid,
            R.string.flow5_aesthetic,
            R.string.flow5_professional,
            R.string.board_f2_item_love,
            R.string.board_diff_style_vintage,
            R.string.board_f2_cat_aesthetic
        )
        ranked(
            art,
            titles[i % 8],
            ExploreAction.Beautiful,
            pop = 90 - i * 4,
            rec = 40 + i * 6,
            trend = 70 - (i - 3) * (i - 3),
            themes = if (i == 5) setOf(ExploreTheme.Love) else setOf(ExploreTheme.Aesthetic),
            look = ExploreLook.Photo
        )
    }
    ExplorePack.Memes -> listOf(
        ranked(BoardFlowArt.memeGoodVibes, R.string.board_f2_template_title, ExploreAction.Template, pop = 95, rec = 42, trend = 78, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.memeMonday, R.string.board_f2_meme_monday, ExploreAction.Template, pop = 83, rec = 32, trend = 72, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.funnyHero, R.string.board_f2_meme_expectation, ExploreAction.Template, pop = 79, rec = 38, trend = 90, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(R.drawable.board_c00_funny_dog, R.string.board_f2_item_pets, ExploreAction.Template, pop = 86, rec = 50, trend = 84, themes = setOf(ExploreTheme.Pets, ExploreTheme.Funny), look = ExploreLook.Photo),
        ranked(BoardFlowArt.memeCategoryHero, R.string.board_f2_item_funny_memes, ExploreAction.Template, pop = 71, rec = 24, trend = 62, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.stickerFunny, R.string.board_f2_item_funny_memes, ExploreAction.Sticker, 2, pop = 66, rec = 91, trend = 75, themes = setOf(ExploreTheme.Funny), look = ExploreLook.Cartoon)
    )
    ExplorePack.Travel -> listOf(
        ranked(BoardFlowArt.trendTravel, R.string.board_f2_item_travel_vibes, ExploreAction.Template, pop = 92, rec = 60, trend = 88, themes = setOf(ExploreTheme.Travel), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_beach, R.string.board_f2_item_travel, ExploreAction.Template, pop = 80, rec = 70, trend = 64, themes = setOf(ExploreTheme.Travel), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_city, R.string.board_f2_item_city, ExploreAction.Template, pop = 74, rec = 82, trend = 90, themes = setOf(ExploreTheme.Travel), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_mountains, R.string.board_f2_item_travel, ExploreAction.Template, pop = 85, rec = 55, trend = 70, themes = setOf(ExploreTheme.Travel), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_cafe, R.string.board_f2_cat_aesthetic, ExploreAction.Template, pop = 60, rec = 40, trend = 36, themes = setOf(ExploreTheme.Aesthetic, ExploreTheme.Travel), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_scene, R.string.board_f2_item_travel_vibes, ExploreAction.Template, pop = 58, rec = 96, trend = 52, themes = setOf(ExploreTheme.Travel), look = ExploreLook.Photo)
    )
    ExplorePack.Festive -> listOf(
        ranked(R.drawable.board_c12_card, R.string.board_f2_item_diwali, ExploreAction.Template, pop = 90, rec = 70, trend = 80, themes = setOf(ExploreTheme.Festival, ExploreTheme.Family), look = ExploreLook.Photo),
        ranked(BoardFlowArt.festiveChristmas, R.string.board_f2_item_christmas, ExploreAction.Template, pop = 86, rec = 94, trend = 92, themes = setOf(ExploreTheme.Festival), look = ExploreLook.Photo),
        ranked(BoardFlowArt.festiveNewYear, R.string.board_f2_item_new_year, ExploreAction.Template, pop = 78, rec = 99, trend = 88, themes = setOf(ExploreTheme.Festival), look = ExploreLook.Photo),
        ranked(BoardFlowArt.festiveHoli, R.string.board_f2_item_holi, ExploreAction.Template, pop = 84, rec = 90, trend = 70, themes = setOf(ExploreTheme.Festival), look = ExploreLook.Photo),
        ranked(BoardFlowArt.festiveEid, R.string.board_f2_item_eid, ExploreAction.Template, pop = 80, rec = 88, trend = 60, themes = setOf(ExploreTheme.Festival, ExploreTheme.Family), look = ExploreLook.Photo),
        ranked(BoardFlowArt.festiveHalloween, R.string.board_f2_item_halloween, ExploreAction.Template, pop = 72, rec = 85, trend = 96, themes = setOf(ExploreTheme.Festival), look = ExploreLook.Photo)
    )
    ExplorePack.Love -> listOf(
        ranked(R.drawable.board_c09_portrait, R.string.board_f2_item_love, ExploreAction.Template, pop = 91, rec = 50, trend = 74, themes = setOf(ExploreTheme.Love), look = ExploreLook.Photo),
        ranked(BoardFlowArt.templateFamily, R.string.board_f2_item_family, ExploreAction.Template, pop = 77, rec = 97, trend = 58, themes = setOf(ExploreTheme.Family, ExploreTheme.Love), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.trendPortrait, R.string.board_f2_item_ai_portraits, ExploreAction.Beautiful, pop = 85, rec = 62, trend = 90, themes = setOf(ExploreTheme.Love, ExploreTheme.Aesthetic), look = ExploreLook.Photo),
        ranked(BoardFlowArt.stickerLove, R.string.board_f2_item_love, ExploreAction.Sticker, 0, pop = 70, rec = 89, trend = 66, themes = setOf(ExploreTheme.Love), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.collageHero, R.string.board_f2_cat_aesthetic, ExploreAction.Template, pop = 63, rec = 80, trend = 44, themes = setOf(ExploreTheme.Aesthetic, ExploreTheme.Love), look = ExploreLook.Photo),
        ranked(BoardFlowArt.baAfter, R.string.flow5_aesthetic, ExploreAction.Beautiful, pop = 82, rec = 54, trend = 72, themes = setOf(ExploreTheme.Aesthetic, ExploreTheme.Love), look = ExploreLook.Photo)
    )
    ExplorePack.Movies -> listOf(
        ranked(BoardFlowArt.motionHero, R.string.make_it_move, ExploreAction.Move, pop = 93, rec = 60, trend = 91, look = ExploreLook.Photo),
        ranked(BoardFlowArt.posterMove, R.string.home_intent_move_desc, ExploreAction.Move, pop = 75, rec = 70, trend = 80, look = ExploreLook.Photo),
        ranked(R.drawable.board_c11_talk, R.string.create_extra_talking, ExploreAction.Move, pop = 81, rec = 88, trend = 70, look = ExploreLook.Photo),
        ranked(BoardFlowArt.cameraViewfinder, R.string.board_f17_video, ExploreAction.Move, pop = 64, rec = 96, trend = 55, look = ExploreLook.Photo)
    )
    ExplorePack.Cartoon -> exploreCards(ExplorePack.Styles).filter { it.styleIndex in listOf(1, 5, 8, 0, 6) }
    ExplorePack.Aesthetic -> listOf(
        ranked(BoardFlowArt.collageHero, R.string.board_f2_cat_aesthetic, ExploreAction.Template, pop = 88, rec = 70, trend = 60, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Photo),
        ranked(BoardFlowArt.trendPortrait, R.string.flow5_aesthetic, ExploreAction.Beautiful, pop = 84, rec = 55, trend = 90, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_studio, R.string.board_diff_style_line, ExploreAction.Style, 10, pop = 60, rec = 80, trend = 48, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Illustration),
        ranked(BoardFlowArt.baAfter, R.string.flow5_vivid, ExploreAction.Beautiful, pop = 78, rec = 50, trend = 72, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Photo),
        ranked(R.drawable.board_c07_cafe, R.string.board_diff_style_vintage, ExploreAction.Style, 9, pop = 66, rec = 40, trend = 36, themes = setOf(ExploreTheme.Aesthetic), look = ExploreLook.Photo),
        ranked(BoardFlowArt.templateFamily, R.string.flow2_chip_templates, ExploreAction.Template, pop = 54, rec = 98, trend = 42, themes = setOf(ExploreTheme.Family, ExploreTheme.Aesthetic), look = ExploreLook.Illustration)
    )
}

internal fun exploreSorted(pack: ExplorePack, sort: ExploreSort): List<ExploreCard> {
    val base = exploreCards(pack)
    return when (sort) {
        ExploreSort.Popular -> base.sortedByDescending { it.popularity }
        ExploreSort.Trending -> base.sortedByDescending { it.trending }
        ExploreSort.Latest -> base.sortedByDescending { it.recency }
    }
}

internal fun exploreSortFromTab(tab: Int): ExploreSort =
    ExploreSort.entries.getOrElse(tab) { ExploreSort.Popular }

internal fun packForCategory(@StringRes titleRes: Int): ExplorePack = when (titleRes) {
    R.string.board_f2_item_ai_portraits -> ExplorePack.Portraits
    R.string.board_f2_item_funny_memes -> ExplorePack.Memes
    R.string.board_f2_cat_stickers -> ExplorePack.Stickers
    R.string.board_f2_item_travel -> ExplorePack.Travel
    R.string.board_f2_item_festive -> ExplorePack.Festive
    R.string.board_f2_item_love -> ExplorePack.Love
    R.string.board_f2_cat_movies -> ExplorePack.Movies
    R.string.board_f2_cat_cartoon -> ExplorePack.Cartoon
    R.string.board_f2_cat_aesthetic -> ExplorePack.Aesthetic
    else -> ExplorePack.Templates
}

internal fun packForChip(chip: Int): ExplorePack? = when (chip) {
    1 -> ExplorePack.Templates
    2 -> ExplorePack.Styles
    3 -> ExplorePack.Stickers
    else -> null
}

internal fun exploreCta(action: ExploreAction): Int = when (action) {
    ExploreAction.Template -> R.string.board_f2_use_template
    ExploreAction.Style -> R.string.board_f2_use_style
    ExploreAction.Sticker -> R.string.board_f13_create_sticker
    ExploreAction.Beautiful -> R.string.make_it_beautiful
    ExploreAction.Move -> R.string.make_it_move
}

internal fun exploreCollectionCards(): List<ExploreCard> = listOf(
    ExplorePack.Templates,
    ExplorePack.Travel,
    ExplorePack.Love,
    ExplorePack.Stickers,
    ExplorePack.Festive,
    ExplorePack.Aesthetic
).mapNotNull { exploreSorted(it, ExploreSort.Popular).firstOrNull() }

internal fun exploreTrendingCards(): List<ExploreCard> =
    exploreSearchIndex()
        .map { it.card }
        .distinctBy { it.art to it.titleRes }
        .sortedByDescending { it.trending }
        .take(8)

internal fun exploreSearchCards(): List<ExploreCard> = exploreSearchIndex().map { it.card }

internal fun exploreSearchIndex(): List<ExploreSearchHit> =
    listOf(
        ExplorePack.Templates,
        ExplorePack.Styles,
        ExplorePack.Stickers,
        ExplorePack.Portraits,
        ExplorePack.Memes,
        ExplorePack.Travel,
        ExplorePack.Festive,
        ExplorePack.Love,
        ExplorePack.Movies,
        ExplorePack.Cartoon,
        ExplorePack.Aesthetic
    ).flatMap { pack -> exploreCards(pack).map { pack to it } }
        .distinctBy { it.second.titleRes to it.second.art }
        .mapIndexed { i, (pack, card) -> ExploreSearchHit(i, pack, card) }

/**
 * @param contentType 0 all, 1 templates, 2 stickers, 3 styles, 4 frames, 5 collages
 * @param theme 0 all, 1 birthday, 2 love, 3 family, 4 festival, 5 travel, 6 pets
 * @param look 0 all, 1 photo, 2 illustration, 3 cartoon, 4 3d, 5 watercolor
 * @param sort 0 relevance, 1 popular, 2 newest
 */
internal fun filterExploreSearch(
    query: String,
    contentType: Int,
    theme: Int,
    look: Int,
    sort: Int,
    titleOf: (Int) -> String
): List<ExploreSearchHit> {
    var list = exploreSearchIndex()
    list = when (contentType) {
        1 -> list.filter { it.card.action == ExploreAction.Template || it.card.action == ExploreAction.Beautiful }
        2 -> list.filter { it.card.action == ExploreAction.Sticker }
        3 -> list.filter { it.card.action == ExploreAction.Style }
        4 -> list.filter { it.card.action == ExploreAction.Template && it.card.look == ExploreLook.Illustration }
        5 -> list.filter {
            it.card.art == BoardFlowArt.collageHero || it.pack == ExplorePack.Aesthetic
        }
        else -> list
    }
    val themeWanted = when (theme) {
        1 -> ExploreTheme.Birthday
        2 -> ExploreTheme.Love
        3 -> ExploreTheme.Family
        4 -> ExploreTheme.Festival
        5 -> ExploreTheme.Travel
        6 -> ExploreTheme.Pets
        else -> null
    }
    if (themeWanted != null) {
        list = list.filter { themeWanted in it.card.themes }
    }
    val lookWanted = when (look) {
        1 -> ExploreLook.Photo
        2 -> ExploreLook.Illustration
        3 -> ExploreLook.Cartoon
        4 -> ExploreLook.ThreeD
        5 -> ExploreLook.Watercolor
        else -> null
    }
    if (lookWanted != null) {
        list = list.filter { it.card.look == lookWanted }
    }
    val q = query.trim().lowercase()
    if (q.isNotEmpty()) {
        list = list.filter { hit ->
            val title = titleOf(hit.card.titleRes).lowercase()
            title.contains(q) || exploreQueryKeys(hit).any { key -> key.contains(q) || q.contains(key) }
        }
    }
    return when (sort) {
        1 -> list.sortedByDescending { it.card.popularity }
        2 -> list.sortedByDescending { it.card.recency }
        else -> list.sortedByDescending { hit ->
            val title = titleOf(hit.card.titleRes).lowercase()
            val exact = if (q.isNotEmpty() && title.contains(q)) 40 else 0
            hit.card.popularity + exact
        }
    }
}

internal fun exploreMoreLike(hit: ExploreSearchHit): List<ExploreSearchHit> =
    exploreSearchIndex()
        .filter { it.id != hit.id && (it.pack == hit.pack || it.card.action == hit.card.action) }
        .sortedByDescending { it.card.popularity }

private fun exploreQueryKeys(hit: ExploreSearchHit): List<String> {
    val keys = mutableListOf(hit.pack.name.lowercase(), hit.card.action.name.lowercase())
    hit.card.themes.forEach { keys += it.name.lowercase() }
    keys += hit.card.look.name.lowercase()
    when (hit.card.titleRes) {
        R.string.board_diff_style_anime -> keys += "anime"
        R.string.board_diff_style_3d -> keys += listOf("3d", "three")
        R.string.board_diff_style_pixel -> keys += "pixel"
        R.string.board_diff_style_sketch -> keys += "sketch"
        R.string.board_f2_item_birthday -> keys += "birthday"
        R.string.board_f2_item_pets -> keys += listOf("pets", "pet", "dog")
        R.string.board_f2_item_travel, R.string.board_f2_item_travel_vibes -> keys += "travel"
        R.string.board_f2_item_love -> keys += listOf("love", "wedding")
        R.string.board_f2_item_holi -> keys += "holi"
        R.string.board_f2_item_eid -> keys += "eid"
        R.string.board_f2_item_christmas -> keys += "christmas"
        R.string.board_f2_item_diwali -> keys += "diwali"
        R.string.board_f2_item_halloween -> keys += "halloween"
        R.string.make_it_move -> keys += listOf("move", "video", "movie")
    }
    return keys
}

internal fun exploreTags(pack: ExplorePack): List<String> = when (pack) {
    ExplorePack.Memes, ExplorePack.Templates -> listOf("#funny", "#meme", "#template")
    ExplorePack.Styles, ExplorePack.Cartoon -> listOf("#style", "#art", "#ai")
    ExplorePack.Stickers -> listOf("#sticker", "#emoji", "#pack")
    ExplorePack.Portraits -> listOf("#portrait", "#glow", "#ai")
    ExplorePack.Travel -> listOf("#travel", "#vacation", "#scene")
    ExplorePack.Festive -> listOf("#festive", "#celebrate", "#card")
    ExplorePack.Love -> listOf("#love", "#together", "#soft")
    ExplorePack.Movies -> listOf("#motion", "#gif", "#move")
    ExplorePack.Aesthetic -> listOf("#aesthetic", "#collage", "#vintage")
}
