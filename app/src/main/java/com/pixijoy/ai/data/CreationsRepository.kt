package com.pixijoy.ai.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class SavedCreationType(val id: String) {
    PHOTO("photo"),
    MEME("meme"),
    STICKER("sticker"),
    GIF("gif"),
    REEL("reel"),
    OTHER("other");

    companion object {
        fun fromId(id: String): SavedCreationType =
            entries.find { it.id == id } ?: OTHER
    }
}

data class SavedCreation(
    val id: String,
    val uri: String,
    val type: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val creationType: SavedCreationType get() = SavedCreationType.fromId(type)
    val contentUri: Uri get() = Uri.parse(uri)
}

private val Context.creationsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixijoy_creations")

@Singleton
class CreationsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val key = stringPreferencesKey("saved_creations_json")
    private val listType = object : TypeToken<List<SavedCreation>>() {}.type

    val creations: Flow<List<SavedCreation>> = context.creationsDataStore.data.map { prefs ->
        val json = prefs[key] ?: "[]"
        runCatching { gson.fromJson<List<SavedCreation>>(json, listType) }.getOrElse { emptyList() }
            .sortedByDescending { it.timestamp }
    }

    suspend fun record(uri: Uri, type: SavedCreationType, title: String) {
        val entry = SavedCreation(
            id = "${type.id}_${System.currentTimeMillis()}",
            uri = uri.toString(),
            type = type.id,
            title = title
        )
        context.creationsDataStore.edit { prefs ->
            val current = runCatching {
                gson.fromJson<List<SavedCreation>>(prefs[key] ?: "[]", listType)
            }.getOrElse { emptyList() }
            val updated = (listOf(entry) + current).distinctBy { it.uri }.take(MAX_ITEMS)
            prefs[key] = gson.toJson(updated)
        }
    }

    suspend fun delete(id: String) {
        context.creationsDataStore.edit { prefs ->
            val current = runCatching {
                gson.fromJson<List<SavedCreation>>(prefs[key] ?: "[]", listType)
            }.getOrElse { emptyList() }
            prefs[key] = gson.toJson(current.filterNot { it.id == id })
        }
    }

    companion object {
        private const val MAX_ITEMS = 48
    }
}
