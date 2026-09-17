package com.pixijoy.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioStore by preferencesDataStore("pixijoy_studio")

class StudioPrefs(private val context: Context) {
    private val privacy = booleanPreferencesKey("privacy_accepted")

    val privacyAccepted: Flow<Boolean> =
        context.studioStore.data.map { it[privacy] == true }

    suspend fun acceptPrivacy() {
        context.studioStore.edit { it[privacy] = true }
    }
}
