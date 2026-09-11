package com.pixijoy.ai.ui.board.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardCommunityCatalogTest {

    @Test
    fun pets_areOnlyPetPosts() {
        val posts = communityFilter(CommunityCat.Pets, 0)
        assertTrue(posts.isNotEmpty())
        assertTrue(posts.all { CommunityCat.Pets in it.cats })
    }

    @Test
    fun travel_differsFromPets() {
        val pets = communityFilter(CommunityCat.Pets, 0).map { it.id }.toSet()
        val travel = communityFilter(CommunityCat.Travel, 0).map { it.id }.toSet()
        assertTrue(travel.isNotEmpty())
        assertTrue(pets.intersect(travel).isEmpty())
    }

    @Test
    fun food_isHonestlyEmpty() {
        assertTrue(communityFilter(CommunityCat.Food, 0).isEmpty())
    }

    @Test
    fun sort_mostLiked_putsHighestFirst() {
        val posts = communityFilter(CommunityCat.All, 1)
        assertTrue(posts.size >= 2)
        assertTrue(posts.first().likes >= posts[1].likes)
        assertEquals(posts.maxOf { it.likes }, posts.first().likes)
    }

    @Test
    fun sort_recent_putsNewestFirst() {
        val posts = communityFilter(CommunityCat.All, 2)
        assertEquals(posts.maxOf { it.recency }, posts.first().recency)
    }

    @Test
    fun sort_editors_onlyPicks() {
        val posts = communityFilter(CommunityCat.All, 3)
        assertTrue(posts.isNotEmpty())
        assertTrue(posts.all { it.editorPick })
    }

    @Test
    fun trending_orderDiffersFromMostLiked() {
        val trending = communityFilter(CommunityCat.All, 0).map { it.id }
        val liked = communityFilter(CommunityCat.All, 1).map { it.id }
        assertFalse(trending == liked)
    }
}
