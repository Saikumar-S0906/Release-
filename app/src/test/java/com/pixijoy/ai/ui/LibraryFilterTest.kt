package com.pixijoy.ai.ui

import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {
    private val photo = Project("p", "Photo", MediaKind.PHOTO, 1, 1, "", "", emptyList())
    private val movie = Project("m", "Movie", MediaKind.MOVIE, 1, 1, "", "", emptyList())
    private val all = listOf(photo, movie)

    @Test
    fun allKeepsBoth() {
        assertEquals(2, all.filterBy(LibraryFilter.ALL).size)
    }

    @Test
    fun photoAndMovieSplit() {
        assertEquals(listOf(photo), all.filterBy(LibraryFilter.PHOTO))
        assertEquals(listOf(movie), all.filterBy(LibraryFilter.MOVIE))
    }
}
