package com.pixijoy.ai.ui.board

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import kotlinx.coroutines.flow.map

/** Lightweight prefs writes from board UI without injecting repositories. */
object BoardNavPrefs {
    val HAS_USER_CREATIONS = booleanPreferencesKey("has_user_creations")
    val KIDS_SPACE = booleanPreferencesKey("kids_space")

    suspend fun markUserCreations(context: Context) {
        runCatching {
            context.pixiJoyPrefsDataStore.edit { it[HAS_USER_CREATIONS] = true }
        }
    }

    suspend fun setKidsSpace(context: Context, enabled: Boolean) {
        runCatching {
            context.pixiJoyPrefsDataStore.edit { it[KIDS_SPACE] = enabled }
        }
    }
}

@Composable
fun rememberKidsSpace(): Boolean {
    val context = LocalContext.current
    val kids by context.pixiJoyPrefsDataStore.data
        .map { it[BoardNavPrefs.KIDS_SPACE] ?: false }
        .collectAsState(initial = false)
    return kids
}
