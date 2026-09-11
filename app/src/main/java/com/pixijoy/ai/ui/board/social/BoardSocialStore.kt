package com.pixijoy.ai.ui.board.social

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import com.pixijoy.ai.ui.board.toastSavedOnDevice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Device-local social actions (like / follow / bookmark / chat draft).
 * Not synced to a community backend — UI should toast [toastSavedOnDevice].
 */
object BoardSocialStore {
    private object Keys {
        val LIKES = stringPreferencesKey("board_social_likes")
        val FOLLOWS = stringPreferencesKey("board_social_follows")
        val BOOKMARKS = stringPreferencesKey("board_social_bookmarks")
    }

    private val mutex = Mutex()
    private var hydrated = false
    private val likes = mutableSetOf<String>()
    private val follows = mutableSetOf<String>()
    private val bookmarks = mutableSetOf<String>()

    var revision by mutableIntStateOf(0)
        private set

    private fun bump() {
        revision++
    }

    suspend fun hydrate(context: Context) {
        mutex.withLock {
            if (hydrated) return
            val prefs = context.pixiJoyPrefsDataStore.data.first()
            likes.clear()
            likes.addAll(parseSet(prefs[Keys.LIKES]))
            follows.clear()
            follows.addAll(parseSet(prefs[Keys.FOLLOWS]))
            bookmarks.clear()
            bookmarks.addAll(parseSet(prefs[Keys.BOOKMARKS]))
            hydrated = true
            bump()
        }
    }

    private suspend fun persist(context: Context) {
        context.pixiJoyPrefsDataStore.edit { prefs ->
            prefs[Keys.LIKES] = likes.sorted().joinToString(",")
            prefs[Keys.FOLLOWS] = follows.sorted().joinToString(",")
            prefs[Keys.BOOKMARKS] = bookmarks.sorted().joinToString(",")
        }
    }

    fun isLiked(id: String): Boolean = id in likes
    fun isFollowed(id: String): Boolean = id in follows
    fun isBookmarked(id: String): Boolean = id in bookmarks

    suspend fun toggleLike(context: Context, id: String): Boolean {
        mutex.withLock {
            if (id in likes) likes.remove(id) else likes.add(id)
            persist(context)
            bump()
            context.toastSavedOnDevice()
            return id in likes
        }
    }

    suspend fun toggleFollow(context: Context, id: String): Boolean {
        mutex.withLock {
            if (id in follows) follows.remove(id) else follows.add(id)
            persist(context)
            bump()
            context.toastSavedOnDevice()
            return id in follows
        }
    }

    suspend fun toggleBookmark(context: Context, id: String): Boolean {
        mutex.withLock {
            if (id in bookmarks) bookmarks.remove(id) else bookmarks.add(id)
            persist(context)
            bump()
            context.toastSavedOnDevice()
            return id in bookmarks
        }
    }

    private fun parseSet(raw: String?): Set<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()
}
