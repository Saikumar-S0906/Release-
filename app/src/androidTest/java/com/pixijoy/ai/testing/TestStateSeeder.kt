package com.pixijoy.ai.testing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.pixijoy.ai.data.pixiJoyConsentDataStore
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

/**
 * Seeds DataStore via the same singleton delegates the app uses.
 * Do NOT declare a second preferencesDataStore() for pixijoy_prefs / pixijoy_consent.
 */
object TestStateSeeder {
    private val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    private val isPro = booleanPreferencesKey("is_pro")
    private val hdCredits = intPreferencesKey("hd_credits")
    private val aiCredits = intPreferencesKey("ai_credits")
    private val lastCreditReset = longPreferencesKey("last_credit_reset")
    private val privacyAccepted = booleanPreferencesKey("privacy_accepted")
    private val aiCloudConsent = booleanPreferencesKey("ai_cloud_consent")
    private val hasUserCreations = booleanPreferencesKey("has_user_creations")

    fun prepareMainFlow(context: Context) = runBlocking {
        context.pixiJoyPrefsDataStore.edit {
            it[onboardingComplete] = true
            it[isPro] = false
            it[hasUserCreations] = true
        }
        context.pixiJoyConsentDataStore.edit {
            it[privacyAccepted] = true
            it[aiCloudConsent] = true
        }
        resetCreditsToDefault(context)
    }

    fun prepareFreshInstall(context: Context) = runBlocking {
        context.pixiJoyPrefsDataStore.edit { it.clear() }
        context.pixiJoyConsentDataStore.edit { it.clear() }
    }

    fun preparePrivacyOnly(context: Context) = runBlocking {
        context.pixiJoyPrefsDataStore.edit { it.clear() }
        context.pixiJoyConsentDataStore.edit {
            it.clear()
            it[privacyAccepted] = false
        }
    }

    fun prepareExhaustedHdCredits(context: Context) = runBlocking {
        prepareMainFlow(context)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        context.pixiJoyPrefsDataStore.edit {
            it[hdCredits] = 0
            it[aiCredits] = 5
            it[lastCreditReset] = today
        }
    }

    fun prepareExhaustedAiCredits(context: Context) = runBlocking {
        prepareMainFlow(context)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        context.pixiJoyPrefsDataStore.edit {
            it[aiCredits] = 0
            it[hdCredits] = 3
            it[lastCreditReset] = today
        }
    }

    fun prepareProUser(context: Context) = runBlocking {
        prepareMainFlow(context)
        context.pixiJoyPrefsDataStore.edit { it[isPro] = true }
    }

    private suspend fun resetCreditsToDefault(context: Context) {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        context.pixiJoyPrefsDataStore.edit {
            it[hdCredits] = 3
            it[aiCredits] = 5
            it[lastCreditReset] = today
        }
    }
}
