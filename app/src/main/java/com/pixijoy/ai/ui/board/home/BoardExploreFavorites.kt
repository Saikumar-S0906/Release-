package com.pixijoy.ai.ui.board.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Explore template/style hearts — local DataStore, same device as library favorites. */
internal object BoardExploreFavorites {
    private val KEY = stringPreferencesKey("board_explore_fav_arts")
    private val mutex = Mutex()
    private var hydrated = false
    private val arts = mutableSetOf<Int>()

    var revision by mutableIntStateOf(0)
        private set

    fun isFavorite(art: Int): Boolean = art in arts

    fun favoriteCards(): List<ExploreCard> =
        exploreSearchIndex()
            .map { it.card }
            .filter { it.art in arts }
            .distinctBy { it.art }

    suspend fun hydrate(context: Context) {
        mutex.withLock {
            if (hydrated) return
            val raw = context.pixiJoyPrefsDataStore.data.first()[KEY].orEmpty()
            arts.clear()
            if (raw.isNotBlank()) {
                raw.split(',').mapNotNull { it.toIntOrNull() }.forEach { arts.add(it) }
            }
            hydrated = true
            revision++
        }
    }

    suspend fun toggle(context: Context, art: Int): Boolean {
        mutex.withLock {
            if (art in arts) arts.remove(art) else arts.add(art)
            context.pixiJoyPrefsDataStore.edit { it[KEY] = arts.sorted().joinToString(",") }
            revision++
            return art in arts
        }
    }
}
