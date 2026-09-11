package com.pixijoy.ai.ui.board.home

import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Explore IA: chips, packs, and featured must show matching content and actions. */
class BoardExploreCatalogTest {

    @Test
    fun chips_mapToPacks() {
        assertNull(packForChip(0))
        assertEquals(ExplorePack.Templates, packForChip(1))
        assertEquals(ExplorePack.Styles, packForChip(2))
        assertEquals(ExplorePack.Stickers, packForChip(3))
    }

    @Test
    fun featured_opensStyleNotTemplate() {
        BoardExploreSession.selectFeatured()
        assertEquals(ExploreAction.Style, BoardExploreSession.selectedAction)
        assertEquals(ExplorePack.Styles, BoardExploreSession.pack)
        assertEquals(ExploreAction.Style, BoardExploreSession.featuredCard().action)
    }

    @Test
    fun templates_areLayoutsOrMemes() {
        val cards = exploreCards(ExplorePack.Templates)
        assertTrue(cards.isNotEmpty())
        assertTrue(cards.all { it.action == ExploreAction.Template })
        val arts = cards.map { it.art }
        assertFalse(arts.contains(R.drawable.board_c00_funny_dog))
        assertTrue(arts.contains(BoardFlowArt.templateBirthday))
        assertTrue(arts.contains(BoardFlowArt.memeGoodVibes))
    }

    @Test
    fun styles_useMatchingLooks() {
        val cards = exploreCards(ExplorePack.Styles)
        assertEquals(10, cards.size)
        assertTrue(cards.all { it.action == ExploreAction.Style })
        val byIndex = cards.associateBy { it.styleIndex }
        assertEquals(BoardFlowArt.styleOil, byIndex.getValue(2).art)
        assertEquals(BoardFlowArt.styleWatercolor, byIndex.getValue(3).art)
        assertEquals(BoardFlowArt.styleSketch, byIndex.getValue(4).art)
        assertEquals(BoardFlowArt.stylePixel, byIndex.getValue(5).art)
        assertEquals(BoardFlowArt.styleFantasy, byIndex.getValue(6).art)
        val arts = cards.map { it.art }
        assertFalse(arts.contains(R.drawable.board_c00_funny_dog))
        assertFalse(arts.contains(R.drawable.board_c04_cutout))
        assertFalse(arts.contains(R.drawable.board_c12_card))
    }

    @Test
    fun stickers_areCutoutsWithStyleIndex() {
        val cards = exploreCards(ExplorePack.Stickers)
        assertTrue(cards.all { it.action == ExploreAction.Sticker })
        assertTrue(cards.all { it.styleIndex >= 0 })
        val arts = cards.map { it.art }
        assertTrue(arts.contains(BoardFlowArt.stickerHero))
        assertTrue(arts.contains(BoardFlowArt.stickerPets))
        assertFalse(arts.contains(BoardFlowArt.funnyHero))
        assertFalse(arts.contains(BoardFlowArt.trendPortrait))
    }

    @Test
    fun festive_labelsMatchHolidayArt() {
        val byTitle = exploreCards(ExplorePack.Festive).associateBy { it.titleRes }
        assertEquals(R.drawable.board_c12_card, byTitle.getValue(R.string.board_f2_item_diwali).art)
        assertEquals(BoardFlowArt.festiveChristmas, byTitle.getValue(R.string.board_f2_item_christmas).art)
        assertEquals(BoardFlowArt.festiveHoli, byTitle.getValue(R.string.board_f2_item_holi).art)
        assertEquals(BoardFlowArt.festiveEid, byTitle.getValue(R.string.board_f2_item_eid).art)
        assertEquals(BoardFlowArt.festiveHalloween, byTitle.getValue(R.string.board_f2_item_halloween).art)
        assertEquals(BoardFlowArt.festiveNewYear, byTitle.getValue(R.string.board_f2_item_new_year).art)
    }

    @Test
    fun popularCategories_mapToPacks() {
        assertEquals(ExplorePack.Portraits, packForCategory(R.string.board_f2_item_ai_portraits))
        assertEquals(ExplorePack.Memes, packForCategory(R.string.board_f2_item_funny_memes))
        assertEquals(ExplorePack.Stickers, packForCategory(R.string.board_f2_cat_stickers))
        assertEquals(ExplorePack.Travel, packForCategory(R.string.board_f2_item_travel))
        assertEquals(ExplorePack.Festive, packForCategory(R.string.board_f2_item_festive))
        assertEquals(ExplorePack.Love, packForCategory(R.string.board_f2_item_love))
        assertEquals(ExplorePack.Movies, packForCategory(R.string.board_f2_cat_movies))
        assertEquals(ExplorePack.Cartoon, packForCategory(R.string.board_f2_cat_cartoon))
        assertEquals(ExplorePack.Aesthetic, packForCategory(R.string.board_f2_cat_aesthetic))
    }

    @Test
    fun travel_cityIsNotLabeledWork() {
        val cards = exploreCards(ExplorePack.Travel)
        assertTrue(cards.any { it.titleRes == R.string.board_f2_item_city })
        assertFalse(cards.any { it.titleRes == R.string.board_f2_item_work })
    }

    @Test
    fun movies_stayOnMovePipeline() {
        val cards = exploreCards(ExplorePack.Movies)
        assertTrue(cards.all { it.action == ExploreAction.Move })
        assertFalse(cards.any { it.titleRes == R.string.home_talk_to_pixijoy })
    }

    @Test
    fun categorySorts_changeTheTopItem() {
        val festive = ExplorePack.Festive
        val popular = exploreSorted(festive, ExploreSort.Popular)
        val trending = exploreSorted(festive, ExploreSort.Trending)
        val latest = exploreSorted(festive, ExploreSort.Latest)
        assertEquals(R.string.board_f2_item_diwali, popular.first().titleRes)
        assertEquals(R.string.board_f2_item_halloween, trending.first().titleRes)
        assertEquals(R.string.board_f2_item_new_year, latest.first().titleRes)
        assertFalse(popular.map { it.titleRes } == trending.map { it.titleRes })
        assertFalse(trending.map { it.titleRes } == latest.map { it.titleRes })
    }

    @Test
    fun search_birthdayQuery_returnsBirthdayTheme() {
        val hits = filterExploreSearch("birthday", 0, 0, 0, 0) { "" }
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { ExploreTheme.Birthday in it.card.themes })
    }

    @Test
    fun search_stickersTab_onlyStickers() {
        val hits = filterExploreSearch("", 2, 0, 0, 0) { "" }
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.card.action == ExploreAction.Sticker })
    }

    @Test
    fun search_stylesTab_onlyStyles() {
        val hits = filterExploreSearch("", 3, 0, 0, 0) { "" }
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.card.action == ExploreAction.Style })
    }

    @Test
    fun search_petsTheme_onlyPets() {
        val hits = filterExploreSearch("", 0, 6, 0, 0) { "" }
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { ExploreTheme.Pets in it.card.themes })
    }

    @Test
    fun search_unknownQuery_isEmpty() {
        val hits = filterExploreSearch("xzzyqq", 0, 0, 0, 0) { "nope" }
        assertTrue(hits.isEmpty())
    }

    @Test
    fun search_tabsAreDistinctSets() {
        val templates = filterExploreSearch("", 1, 0, 0, 0) { "" }.map { it.id }.toSet()
        val stickers = filterExploreSearch("", 2, 0, 0, 0) { "" }.map { it.id }.toSet()
        val styles = filterExploreSearch("", 3, 0, 0, 0) { "" }.map { it.id }.toSet()
        assertTrue(templates.intersect(stickers).isEmpty())
        assertTrue(stickers.intersect(styles).isEmpty())
        assertTrue(templates.intersect(styles).isEmpty())
    }
}
