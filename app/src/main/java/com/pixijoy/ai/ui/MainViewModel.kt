package com.pixijoy.ai.ui

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixijoy.ai.BuildConfig
import com.pixijoy.ai.R
import com.pixijoy.ai.ads.AdMobManager
import com.pixijoy.ai.billing.BillingRepository
import com.pixijoy.ai.core.network.NetworkMonitor
import com.pixijoy.ai.creative.DailyChallengeProvider
import com.pixijoy.ai.data.AuthRepository
import com.pixijoy.ai.data.ConsentRepository
import com.pixijoy.ai.data.UserPreferencesRepository
import com.pixijoy.ai.data.UserStats
import com.pixijoy.ai.domain.model.DailyChallenge
import com.pixijoy.ai.testing.E2eLightMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val onboardingComplete: Boolean = false,
    val stats: UserStats = UserStats(),
    val dailyChallenge: DailyChallenge? = null,
    val isOnline: Boolean = true,
    val isSlowNetwork: Boolean = false,
    val privacyAccepted: Boolean = false,
    val aiCloudConsent: Boolean = false,
    val showAiConsentDialog: Boolean = false,
    val rewardedAdReady: Boolean = false,
    val adMessageRes: Int? = null,
    /** False until first DataStore emission — avoid NavHost starting on privacy by default. */
    val prefsReady: Boolean = false,
    /** Flow 1 entry when Onboarding route is shown. */
    val onboardingEntryScreenId: String = "b1_splash",
    val needsWhatsNew: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val billingRepository: BillingRepository,
    private val consentRepository: ConsentRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val adMobManager: AdMobManager,
    private val imageSession: com.pixijoy.ai.data.ImageSessionRepository,
    dailyChallengeProvider: DailyChallengeProvider
) : ViewModel() {
    private val challenge = dailyChallengeProvider.todayChallenge()
    private val adMessage = MutableStateFlow<Int?>(null)

    init {
        // Defer ads/billing/auth so first frame + preference gate can complete without ANR.
        // Maestro sets debug.pixijoy.e2e=1 → skip heavy SDKs entirely (no ANR under automation).
        viewModelScope.launch(Dispatchers.Default) {
            delay(if (BuildConfig.DEBUG) 1_200 else 300)
            if (E2eLightMode.enabled(appContext)) return@launch
            runCatching { adMobManager.initialize() }
            runCatching { authRepository.ensureGuestSession() }
            runCatching { billingRepository.startConnection() }
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        preferencesRepository.userStats,
        consentRepository.privacyAccepted,
        consentRepository.aiCloudConsent,
        consentRepository.aiConsentPrompted,
        networkMonitor.status
    ) { stats, privacyAccepted, aiConsent, aiPrompted, networkStatus ->
        val needsWhatsNew = stats.onboardingComplete &&
            stats.lastSeenVersionCode > 0 &&
            BuildConfig.VERSION_CODE > stats.lastSeenVersionCode
        // Incomplete first-run must always start at splash → welcome (Flow 1).
        // Do not jump to "returning" just because splash marked prior launch mid-flow.
        val onboardingEntry = when {
            needsWhatsNew -> "b1_whats_new"
            else -> "b1_splash"
        }
        MainUiState(
            onboardingComplete = stats.onboardingComplete,
            stats = stats,
            dailyChallenge = challenge,
            isOnline = networkStatus.isOnline,
            isSlowNetwork = networkStatus.isSlow,
            privacyAccepted = privacyAccepted,
            aiCloudConsent = aiConsent,
            // After privacy + onboarding, ask once before cloud Gemini is used.
            showAiConsentDialog = privacyAccepted && stats.onboardingComplete && !aiPrompted,
            prefsReady = true,
            onboardingEntryScreenId = onboardingEntry,
            needsWhatsNew = needsWhatsNew
        )
    }.combine(adMobManager.rewardedAdReady) { state, rewardedReady ->
        state.copy(rewardedAdReady = rewardedReady)
    }.combine(adMessage) { state, messageRes ->
        state.copy(adMessageRes = messageRes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.completeOnboarding()
            preferencesRepository.markVersionSeen(BuildConfig.VERSION_CODE)
        }
    }

    /** Await durable onboarding flag before leaving the Flow 1 gate (avoids Start Creating no-op race). */
    suspend fun completeOnboardingAwait() {
        preferencesRepository.completeOnboarding()
        preferencesRepository.markVersionSeen(BuildConfig.VERSION_CODE)
    }

    fun dismissWhatsNew() {
        viewModelScope.launch {
            preferencesRepository.markVersionSeen(BuildConfig.VERSION_CODE)
        }
    }

    fun markPriorLaunch() {
        viewModelScope.launch { preferencesRepository.markPriorLaunch() }
    }

    fun markUserCreations() {
        viewModelScope.launch { preferencesRepository.markUserCreations() }
    }

    fun setKidsSpace(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setKidsSpace(enabled) }
    }

    fun continueAsGuest(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.ensureGuestSession() }
            preferencesRepository.markPriorLaunch()
            onDone()
        }
    }

    fun acceptPrivacy() {
        viewModelScope.launch { consentRepository.acceptPrivacy() }
    }

    /**
     * Seeds gates for board screenshot / adb review capture, then returns when prefs are ready.
     * Always accepts privacy + AI consent prompt and completes onboarding so [BoardScreen] is reachable.
     */
    suspend fun prepareBoardReviewCapture() {
        consentRepository.acceptPrivacy()
        consentRepository.setAiCloudConsent(true)
        preferencesRepository.completeOnboarding()
        preferencesRepository.markVersionSeen(BuildConfig.VERSION_CODE)
    }

    fun acceptAiConsent() {
        viewModelScope.launch { consentRepository.setAiCloudConsent(true) }
    }

    fun declineAiConsent() {
        viewModelScope.launch { consentRepository.setAiCloudConsent(false) }
    }

    fun deleteUserData() {
        viewModelScope.launch {
            consentRepository.deleteAllData()
            preferencesRepository.setProStatus(false)
        }
    }

    fun clearAdMessage() {
        adMessage.value = null
    }

    fun markPendingRegenerate() {
        imageSession.pendingRegenerate = true
    }

    fun clearCreateSession() {
        imageSession.clear()
    }

    fun applyChallengeTemplate(top: String, bottom: String) {
        imageSession.setAiHints(top = top, bottom = bottom)
    }

    fun setTrendPrompt(prompt: String) {
        imageSession.pendingNlCommand = prompt
        imageSession.pendingTrendPrompt = prompt
    }

    fun setTemplateCategory(category: String) {
        imageSession.pendingTemplateCategory = category
    }

    fun watchRewardedAdForAiCredit(activity: Activity) {
        viewModelScope.launch {
            if (!preferencesRepository.canWatchRewardedAd()) {
                adMessage.value = R.string.ad_daily_limit
                return@launch
            }
            adMobManager.showRewardedAd(
                activity = activity,
                onRewarded = {
                    viewModelScope.launch {
                        val granted = preferencesRepository.grantRewardedAiCredit()
                        adMessage.value = if (granted) R.string.ad_reward_earned else R.string.ad_daily_limit
                    }
                },
                onFailed = { adMessage.value = R.string.ad_not_available }
            )
        }
    }

    fun watchRewardedAdForHdCredit(activity: Activity) {
        viewModelScope.launch {
            if (!preferencesRepository.canWatchRewardedAd()) {
                adMessage.value = R.string.ad_daily_limit
                return@launch
            }
            adMobManager.showRewardedAd(
                activity = activity,
                onRewarded = {
                    viewModelScope.launch {
                        val granted = preferencesRepository.grantRewardedHdCredit()
                        adMessage.value = if (granted) R.string.ad_hd_reward_earned else R.string.ad_daily_limit
                    }
                },
                onFailed = { adMessage.value = R.string.ad_not_available }
            )
        }
    }
}

