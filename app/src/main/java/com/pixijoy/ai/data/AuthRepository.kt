package com.pixijoy.ai.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixijoy.ai.core.network.GuestAuthRequest
import com.pixijoy.ai.core.network.PixiJoyBackendApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixijoy_auth")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendApi: PixiJoyBackendApi
) {
    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
    }

    val authToken: Flow<String?> = context.authDataStore.data.map { it[Keys.AUTH_TOKEN] }
    val userId: Flow<String?> = context.authDataStore.data.map { it[Keys.USER_ID] }

    suspend fun ensureGuestSession() {
        val prefs = context.authDataStore.data.first()
        if (!prefs[Keys.AUTH_TOKEN].isNullOrBlank()) return
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "pixijoy_device"
        runCatching {
            val response = backendApi.guestAuth(GuestAuthRequest(deviceId))
            context.authDataStore.edit {
                it[Keys.AUTH_TOKEN] = response.token
                it[Keys.USER_ID] = response.userId
            }
        }
    }

    suspend fun currentToken(): String? = context.authDataStore.data.first()[Keys.AUTH_TOKEN]
}
