package com.pixijoy.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore delegates for the app + instrumented tests.
 * Creating a second preferencesDataStore() for the same name crashes at runtime.
 */
val Context.pixiJoyPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixijoy_prefs")
val Context.pixiJoyConsentDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixijoy_consent")
