package com.pixijoy.ai.ui.board.library

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixijoy.ai.R
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Demo library row with stable [id] for favorite / trash / rename. */
data class BoardCreation(
    val id: Int,
    @StringRes val titleRes: Int,
    @StringRes val dateRes: Int,
    @DrawableRes val image: Int,
    val status: Int,
    val isVideo: Boolean = false,
    val isCollage: Boolean = false
)

/**
 * In-memory creations library with DataStore-backed favorites, trash, renames, albums.
 * Seed catalog is demo art; mutations survive process death via prefs.
 */
object BoardLibraryStore {
    private object Keys {
        val FAVORITES = stringPreferencesKey("board_lib_favorites")
        val TRASH = stringPreferencesKey("board_lib_trash")
        val TITLES = stringPreferencesKey("board_lib_titles")
        val ALBUMS = stringPreferencesKey("board_lib_albums")
        val COPIES = stringPreferencesKey("board_lib_copies")
    }

    private val mutex = Mutex()
    private var hydrated = false

    private val catalog: List<BoardCreation> = listOf(
        BoardCreation(0, R.string.board_f18_item_good_vibes, R.string.board_f18_date_today, BoardFlowArt.baAfter, 0),
        BoardCreation(1, R.string.board_f18_item_mountain, R.string.board_f18_date_yesterday, BoardFlowArt.trendTravel, 0),
        BoardCreation(2, R.string.board_f18_item_funny, R.string.board_f18_date_week, BoardFlowArt.funnyHero, 0, isVideo = true),
        BoardCreation(3, R.string.board_f18_item_collage, R.string.board_f18_date_week, BoardFlowArt.collageHero, 0, isCollage = true),
        BoardCreation(4, R.string.board_f18_item_draft, R.string.board_f18_date_today, BoardFlowArt.editorHero, 1),
        BoardCreation(5, R.string.board_f18_item_processing, R.string.board_f18_date_today, BoardFlowArt.motionHero, 2, isVideo = true),
        BoardCreation(6, R.string.board_f18_item_sticker, R.string.board_f18_date_month, BoardFlowArt.stickerHero, 0),
        BoardCreation(7, R.string.board_f18_item_meme, R.string.board_f18_date_month, BoardFlowArt.memeGoodVibes, 0)
    )

    private val copies = mutableListOf<BoardCreation>()
    private val favorites = mutableSetOf<Int>()
    private val trash = mutableSetOf<Int>()
    private val customTitles = mutableMapOf<Int, String>()
    private val albumById = mutableMapOf<Int, Int>()

    /** Bumped so Compose readers recompose after mutations. */
    var revision by mutableIntStateOf(0)
        private set

    private fun bump() {
        revision++
    }

    private fun allEntries(): List<BoardCreation> = catalog + copies

    fun byId(id: Int): BoardCreation? = allEntries().find { it.id == id }

    fun selected(): BoardCreation? {
        val id = BoardFlow18Session.selectedId
        return visible().find { it.id == id } ?: visible().firstOrNull()
    }

    fun visible(): List<BoardCreation> {
        val trashSnap = trash.toSet()
        val copiesSnap = copies.toList()
        return (catalog + copiesSnap).filter { it.id !in trashSnap }
    }

    fun trashItems(): List<BoardCreation> {
        val trashSnap = trash.toSet()
        val copiesSnap = copies.toList()
        return (catalog + copiesSnap).filter { it.id in trashSnap }
    }

    fun isFavorite(id: Int): Boolean = id in favorites.toSet()

    fun albumOf(id: Int): Int = albumById[id] ?: 0

    fun displayTitle(context: Context, item: BoardCreation): String =
        customTitles[item.id]?.takeIf { it.isNotBlank() } ?: context.getString(item.titleRes)

    suspend fun hydrate(context: Context) {
        mutex.withLock {
            if (hydrated) return
            val prefs = context.pixiJoyPrefsDataStore.data.first()
            favorites.clear()
            favorites.addAll(parseIntSet(prefs[Keys.FAVORITES]))
            trash.clear()
            trash.addAll(parseIntSet(prefs[Keys.TRASH]))
            customTitles.clear()
            customTitles.putAll(parseStringMap(prefs[Keys.TITLES]))
            albumById.clear()
            albumById.putAll(parseIntMap(prefs[Keys.ALBUMS]))
            copies.clear()
            copies.addAll(parseCopies(prefs[Keys.COPIES]))
            hydrated = true
            bump()
        }
    }

    private suspend fun persist(context: Context) {
        context.pixiJoyPrefsDataStore.edit { prefs ->
            prefs[Keys.FAVORITES] = favorites.sorted().joinToString(",")
            prefs[Keys.TRASH] = trash.sorted().joinToString(",")
            prefs[Keys.TITLES] = customTitles.entries.joinToString("|") { "${it.key}=${it.value.replace('|', ' ').replace('=', ' ')}" }
            prefs[Keys.ALBUMS] = albumById.entries.joinToString("|") { "${it.key}=${it.value}" }
            prefs[Keys.COPIES] = copies.joinToString("|") { c ->
                "${c.id},${c.titleRes},${c.dateRes},${c.image},${c.status},${if (c.isVideo) 1 else 0},${if (c.isCollage) 1 else 0}"
            }
        }
    }

    suspend fun toggleFavorite(context: Context, id: Int): Boolean {
        mutex.withLock {
            if (id in favorites) favorites.remove(id) else favorites.add(id)
            persist(context)
            bump()
            return id in favorites
        }
    }

    fun download(context: Context, id: Int): Boolean {
        val item = byId(id) ?: return false
        return BoardDrawableIo.saveToGallery(context, item.image, filePrefix = "PixiJoy_Creation")
    }

    suspend fun rename(context: Context, id: Int, title: String) {
        mutex.withLock {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) customTitles.remove(id) else customTitles[id] = trimmed
            persist(context)
            bump()
        }
    }

    suspend fun moveToTrash(context: Context, id: Int) {
        mutex.withLock {
            trash.add(id)
            persist(context)
            bump()
        }
    }

    suspend fun restore(context: Context, ids: Collection<Int>) {
        mutex.withLock {
            trash.removeAll(ids.toSet())
            persist(context)
            bump()
        }
    }

    suspend fun deleteForever(context: Context, ids: Collection<Int>) {
        mutex.withLock {
            val set = ids.toSet()
            trash.removeAll(set)
            favorites.removeAll(set)
            customTitles.keys.removeAll(set)
            albumById.keys.removeAll(set)
            copies.removeAll { it.id in set }
            persist(context)
            bump()
        }
    }

    suspend fun setAlbum(context: Context, id: Int, albumIndex: Int) {
        mutex.withLock {
            albumById[id] = albumIndex
            persist(context)
            bump()
        }
    }

    suspend fun saveCopy(context: Context, id: Int): BoardCreation? {
        mutex.withLock {
            val source = byId(id) ?: return null
            val nextId = ((catalog + copies).maxOfOrNull { it.id } ?: 0) + 1
            val copy = source.copy(
                id = nextId,
                titleRes = source.titleRes,
                dateRes = R.string.board_f18_date_today
            )
            copies.add(copy)
            customTitles[nextId] = displayTitle(context, source) + " copy"
            persist(context)
            bump()
            return copy
        }
    }

    private fun parseIntSet(raw: String?): Set<Int> =
        raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet().orEmpty()

    private fun parseIntMap(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('|').mapNotNull { part ->
            val bits = part.split('=')
            if (bits.size != 2) return@mapNotNull null
            val k = bits[0].toIntOrNull() ?: return@mapNotNull null
            val v = bits[1].toIntOrNull() ?: return@mapNotNull null
            k to v
        }.toMap()
    }

    private fun parseStringMap(raw: String?): Map<Int, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('|').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val k = part.substring(0, eq).toIntOrNull() ?: return@mapNotNull null
            k to part.substring(eq + 1)
        }.toMap()
    }

    private fun parseCopies(raw: String?): List<BoardCreation> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('|').mapNotNull { part ->
            val bits = part.split(',')
            if (bits.size < 7) return@mapNotNull null
            BoardCreation(
                id = bits[0].toIntOrNull() ?: return@mapNotNull null,
                titleRes = bits[1].toIntOrNull() ?: return@mapNotNull null,
                dateRes = bits[2].toIntOrNull() ?: return@mapNotNull null,
                image = bits[3].toIntOrNull() ?: return@mapNotNull null,
                status = bits[4].toIntOrNull() ?: 0,
                isVideo = bits[5] == "1",
                isCollage = bits[6] == "1"
            )
        }
    }
}
