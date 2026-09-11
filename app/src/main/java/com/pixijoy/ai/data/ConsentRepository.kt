package com.pixijoy.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
        val AI_CLOUD_CONSENT = booleanPreferencesKey("ai_cloud_consent")
        val AI_CONSENT_PROMPTED = booleanPreferencesKey("ai_consent_prompted")
        val ANALYTICS_CONSENT = booleanPreferencesKey("analytics_consent")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIF_IN_APP = booleanPreferencesKey("notif_in_app")
        val NOTIF_EMAIL = booleanPreferencesKey("notif_email")
        val NOTIF_LIKES = booleanPreferencesKey("notif_likes")
        val NOTIF_COMMENTS = booleanPreferencesKey("notif_comments")
        val NOTIF_FOLLOWS = booleanPreferencesKey("notif_follows")
        val NOTIF_SYSTEM = booleanPreferencesKey("notif_system")
    }

    val privacyAccepted: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.PRIVACY_ACCEPTED] ?: false
    }

    val aiCloudConsent: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.AI_CLOUD_CONSENT] ?: false
    }

    /** True after the first-run AI consent dialog was accepted or declined. */
    val aiConsentPrompted: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.AI_CONSENT_PROMPTED] ?: false
    }

    val analyticsConsent: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.ANALYTICS_CONSENT] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val notifInApp: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIF_IN_APP] ?: true
    }

    val notifEmail: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIF_EMAIL] ?: false
    }

    val notifLikes: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIF_LIKES] ?: true
    }

    val notifComments: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIF_COMMENTS] ?: true
    }

    val notifFollows: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIF_FOLLOWS] ?: true
    }

    val notifSystem: Flow<Boolean> = context.pixiJoyConsentDataStore.data.map {
        it[Keys.NOTIF_SYSTEM] ?: true
    }

    suspend fun acceptPrivacy() {
        context.pixiJoyConsentDataStore.edit { it[Keys.PRIVACY_ACCEPTED] = true }
    }

    suspend fun setAiCloudConsent(granted: Boolean) {
        context.pixiJoyConsentDataStore.edit {
            it[Keys.AI_CLOUD_CONSENT] = granted
            it[Keys.AI_CONSENT_PROMPTED] = true
        }
    }

    suspend fun markAiConsentPrompted() {
        context.pixiJoyConsentDataStore.edit { it[Keys.AI_CONSENT_PROMPTED] = true }
    }

    suspend fun setAnalyticsConsent(granted: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.ANALYTICS_CONSENT] = granted }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotifInApp(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIF_IN_APP] = enabled }
    }

    suspend fun setNotifEmail(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIF_EMAIL] = enabled }
    }

    suspend fun setNotifLikes(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIF_LIKES] = enabled }
    }

    suspend fun setNotifComments(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIF_COMMENTS] = enabled }
    }

    suspend fun setNotifFollows(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIF_FOLLOWS] = enabled }
    }

    suspend fun setNotifSystem(enabled: Boolean) {
        context.pixiJoyConsentDataStore.edit { it[Keys.NOTIF_SYSTEM] = enabled }
    }

    /** One-shot category snapshot for settings UIs. */
    suspend fun notifCategories(): NotifCategoryPrefs {
        val prefs = context.pixiJoyConsentDataStore.data.first()
        return NotifCategoryPrefs(
            inApp = prefs[Keys.NOTIF_IN_APP] ?: true,
            email = prefs[Keys.NOTIF_EMAIL] ?: false,
            likes = prefs[Keys.NOTIF_LIKES] ?: true,
            comments = prefs[Keys.NOTIF_COMMENTS] ?: true,
            follows = prefs[Keys.NOTIF_FOLLOWS] ?: true,
            system = prefs[Keys.NOTIF_SYSTEM] ?: true
        )
    }

    suspend fun deleteAllData() {
        context.pixiJoyConsentDataStore.edit { it.clear() }
    }
}

data class NotifCategoryPrefs(
    val inApp: Boolean,
    val email: Boolean,
    val likes: Boolean,
    val comments: Boolean,
    val follows: Boolean,
    val system: Boolean
)
