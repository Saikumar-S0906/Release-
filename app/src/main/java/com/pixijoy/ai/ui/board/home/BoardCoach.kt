package com.pixijoy.ai.ui.board.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One-time Home tip. Dismissed for good after Got it. */
internal object BoardHomeCoach {
    private val KEY = booleanPreferencesKey("board_home_coach_done")
    private val mutex = Mutex()
    var dismissed by mutableStateOf(true)
        private set

    suspend fun hydrate(context: Context) {
        mutex.withLock {
            dismissed = context.pixiJoyPrefsDataStore.data.first()[KEY] == true
        }
    }

    suspend fun dismiss(context: Context) {
        mutex.withLock {
            dismissed = true
            context.pixiJoyPrefsDataStore.edit { it[KEY] = true }
        }
    }
}
