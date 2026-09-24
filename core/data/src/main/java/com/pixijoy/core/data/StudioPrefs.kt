package com.pixijoy.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioStore by preferencesDataStore("pixijoy_studio")

class StudioPrefs(private val context: Context) {
    private val privacy = booleanPreferencesKey("privacy_accepted")
    private val lastProject = stringPreferencesKey("last_project")
    private val lastLookPreset = stringPreferencesKey("last_look_preset")

    val privacyAccepted: Flow<Boolean> =
        context.studioStore.data.map { it[privacy] == true }

    val lastProjectId: Flow<String?> =
        context.studioStore.data.map { it[lastProject] }

    val lastLookPresetId: Flow<String?> =
        context.studioStore.data.map { it[lastLookPreset] }

    suspend fun acceptPrivacy() {
        context.studioStore.edit { it[privacy] = true }
    }

    suspend fun setLastProject(id: String) {
        context.studioStore.edit { it[lastProject] = id }
    }

    suspend fun setLastLookPreset(id: String) {
        context.studioStore.edit { it[lastLookPreset] = id }
    }
}
