package com.pixijoy.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class UserStats(
    val onboardingComplete: Boolean = false,
    val isPro: Boolean = false,
    val streakDays: Int = 0,
    val hdCreditsRemaining: Int = 3,
    val aiCreditsRemaining: Int = 5,
    val totalEdits: Int = 0,
    val lastActiveDate: Long = 0L,
    val lastSeenVersionCode: Int = 0,
    val hasPriorLaunch: Boolean = false,
    /** False → Home tab shows C00 empty; true → populated Flow 02 home. */
    val hasUserCreations: Boolean = false,
    /** Kids Space: block public Community and people search. */
    val kidsSpace: Boolean = false
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val HD_CREDITS = intPreferencesKey("hd_credits")
        val AI_CREDITS = intPreferencesKey("ai_credits")
        val TOTAL_EDITS = intPreferencesKey("total_edits")
        val LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
        val LAST_CREDIT_RESET = longPreferencesKey("last_credit_reset")
        val REWARDED_ADS_TODAY = intPreferencesKey("rewarded_ads_today")
        val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version_code")
        val HAS_PRIOR_LAUNCH = booleanPreferencesKey("has_prior_launch")
        val HAS_USER_CREATIONS = booleanPreferencesKey("has_user_creations")
        val KIDS_SPACE = booleanPreferencesKey("kids_space")
    }

    val userStats: Flow<UserStats> = context.pixiJoyPrefsDataStore.data.map { prefs ->
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
        val credits = if (lastReset < today) DAILY_HD_CREDITS else prefs[Keys.HD_CREDITS] ?: DAILY_HD_CREDITS
        val aiCredits = if (lastReset < today) DAILY_AI_CREDITS else prefs[Keys.AI_CREDITS] ?: DAILY_AI_CREDITS

        UserStats(
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            isPro = prefs[Keys.IS_PRO] ?: false,
            streakDays = prefs[Keys.STREAK_DAYS] ?: 0,
            hdCreditsRemaining = credits,
            aiCreditsRemaining = aiCredits,
            totalEdits = prefs[Keys.TOTAL_EDITS] ?: 0,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
            lastSeenVersionCode = prefs[Keys.LAST_SEEN_VERSION] ?: 0,
            hasPriorLaunch = prefs[Keys.HAS_PRIOR_LAUNCH] ?: false,
            hasUserCreations = prefs[Keys.HAS_USER_CREATIONS] ?: false,
            kidsSpace = prefs[Keys.KIDS_SPACE] ?: false
        )
    }

    suspend fun completeOnboarding() {
        context.pixiJoyPrefsDataStore.edit {
            it[Keys.ONBOARDING_COMPLETE] = true
            it[Keys.HAS_PRIOR_LAUNCH] = true
        }
    }

    /** Marks current app version as seen (dismisses What's New). */
    suspend fun markVersionSeen(versionCode: Int) {
        context.pixiJoyPrefsDataStore.edit { it[Keys.LAST_SEEN_VERSION] = versionCode }
    }

    /** Soft flag that this device has opened the app before (for Returning User path). */
    suspend fun markPriorLaunch() {
        context.pixiJoyPrefsDataStore.edit { it[Keys.HAS_PRIOR_LAUNCH] = true }
    }

    suspend fun setProStatus(isPro: Boolean) {
        context.pixiJoyPrefsDataStore.edit { it[Keys.IS_PRO] = isPro }
    }

    suspend fun recordEdit() {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val lastActive = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L
            val currentStreak = prefs[Keys.STREAK_DAYS] ?: 0
            prefs[Keys.STREAK_DAYS] = when {
                lastActive == today -> currentStreak
                lastActive == today - 1 -> currentStreak + 1
                else -> 1
            }
            prefs[Keys.LAST_ACTIVE_DATE] = today
            prefs[Keys.TOTAL_EDITS] = (prefs[Keys.TOTAL_EDITS] ?: 0) + 1

            val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
            if (lastReset < today) {
                prefs[Keys.HD_CREDITS] = DAILY_HD_CREDITS
                prefs[Keys.AI_CREDITS] = DAILY_AI_CREDITS
                prefs[Keys.LAST_CREDIT_RESET] = today
            }
        }
    }

    suspend fun consumeHdCredit(): Boolean {
        var success = false
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val isPro = prefs[Keys.IS_PRO] ?: false
            if (isPro) {
                success = true
                return@edit
            }
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
            var credits = prefs[Keys.HD_CREDITS] ?: DAILY_HD_CREDITS
            if (lastReset < today) {
                credits = DAILY_HD_CREDITS
                prefs[Keys.AI_CREDITS] = DAILY_AI_CREDITS
                prefs[Keys.LAST_CREDIT_RESET] = today
            }
            if (credits > 0) {
                prefs[Keys.HD_CREDITS] = credits - 1
                success = true
            }
        }
        return success
    }

    suspend fun consumeAiCredit(): Boolean = consumeAiCredits(1)

    /**
     * Atomically consumes [amount] AI credits (Pro always succeeds).
     * Used so UI-shown [creditCost] matches the wallet deduction.
     */
    suspend fun consumeAiCredits(amount: Int): Boolean {
        if (amount <= 0) return true
        var success = false
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val isPro = prefs[Keys.IS_PRO] ?: false
            if (isPro) {
                success = true
                return@edit
            }
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
            var credits = prefs[Keys.AI_CREDITS] ?: DAILY_AI_CREDITS
            if (lastReset < today) {
                credits = DAILY_AI_CREDITS
                prefs[Keys.HD_CREDITS] = DAILY_HD_CREDITS
                prefs[Keys.LAST_CREDIT_RESET] = today
                prefs[Keys.REWARDED_ADS_TODAY] = 0
            }
            if (credits >= amount) {
                prefs[Keys.AI_CREDITS] = credits - amount
                success = true
            }
        }
        return success
    }

    /** Refund AI credits after a failed charged operation (not subject to ad caps). */
    suspend fun refundAiCredits(amount: Int = 1) {
        if (amount <= 0) return
        context.pixiJoyPrefsDataStore.edit { prefs ->
            if (prefs[Keys.IS_PRO] == true) return@edit
            val credits = prefs[Keys.AI_CREDITS] ?: DAILY_AI_CREDITS
            prefs[Keys.AI_CREDITS] = credits + amount
        }
    }

    /** Grants +1 AI credit from rewarded ad. Returns false if daily ad cap reached. */
    suspend fun grantRewardedAiCredit(): Boolean {
        var granted = false
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val isPro = prefs[Keys.IS_PRO] ?: false
            if (isPro) {
                granted = true
                return@edit
            }
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
            if (lastReset < today) {
                prefs[Keys.HD_CREDITS] = DAILY_HD_CREDITS
                prefs[Keys.AI_CREDITS] = DAILY_AI_CREDITS
                prefs[Keys.LAST_CREDIT_RESET] = today
                prefs[Keys.REWARDED_ADS_TODAY] = 0
            }
            val adsToday = prefs[Keys.REWARDED_ADS_TODAY] ?: 0
            if (adsToday >= MAX_REWARDED_ADS_PER_DAY) return@edit
            val credits = prefs[Keys.AI_CREDITS] ?: DAILY_AI_CREDITS
            prefs[Keys.AI_CREDITS] = credits + 1
            prefs[Keys.REWARDED_ADS_TODAY] = adsToday + 1
            granted = true
        }
        return granted
    }

    /** Grants +1 HD export credit from rewarded ad. */
    suspend fun grantRewardedHdCredit(): Boolean {
        var granted = false
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val isPro = prefs[Keys.IS_PRO] ?: false
            if (isPro) {
                granted = true
                return@edit
            }
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
            if (lastReset < today) {
                prefs[Keys.HD_CREDITS] = DAILY_HD_CREDITS
                prefs[Keys.AI_CREDITS] = DAILY_AI_CREDITS
                prefs[Keys.LAST_CREDIT_RESET] = today
                prefs[Keys.REWARDED_ADS_TODAY] = 0
            }
            val adsToday = prefs[Keys.REWARDED_ADS_TODAY] ?: 0
            if (adsToday >= MAX_REWARDED_ADS_PER_DAY) return@edit
            val credits = prefs[Keys.HD_CREDITS] ?: DAILY_HD_CREDITS
            prefs[Keys.HD_CREDITS] = credits + 1
            prefs[Keys.REWARDED_ADS_TODAY] = adsToday + 1
            granted = true
        }
        return granted
    }

    suspend fun canWatchRewardedAd(): Boolean {
        var canWatch = false
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val isPro = prefs[Keys.IS_PRO] ?: false
            if (isPro) {
                canWatch = false
                return@edit
            }
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val lastReset = prefs[Keys.LAST_CREDIT_RESET] ?: 0L
            val adsToday = if (lastReset < today) 0 else prefs[Keys.REWARDED_ADS_TODAY] ?: 0
            canWatch = adsToday < MAX_REWARDED_ADS_PER_DAY
        }
        return canWatch
    }

    /** Marks that the user has at least one creation — switches Home from C00 to Flow 02. */
    suspend fun markUserCreations() {
        context.pixiJoyPrefsDataStore.edit { it[Keys.HAS_USER_CREATIONS] = true }
    }

    suspend fun setKidsSpace(enabled: Boolean) {
        context.pixiJoyPrefsDataStore.edit { it[Keys.KIDS_SPACE] = enabled }
    }

    companion object {
        const val DAILY_HD_CREDITS = 3
        const val DAILY_AI_CREDITS = 5
        const val MAX_REWARDED_ADS_PER_DAY = 5
    }
}
