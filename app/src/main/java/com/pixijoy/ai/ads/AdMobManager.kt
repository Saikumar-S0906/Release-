package com.pixijoy.ai.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.pixijoy.ai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _rewardedAdReady = MutableStateFlow(false)
    val rewardedAdReady: StateFlow<Boolean> = _rewardedAdReady.asStateFlow()

    private val _interstitialReady = MutableStateFlow(false)
    val interstitialReady: StateFlow<Boolean> = _interstitialReady.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    @Volatile private var initialized = false

    /**
     * Safe to call from main thread — heavy MobileAds work is deferred off the UI thread
     * to avoid startup ANRs ("Process failed to complete startup").
     */
    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch {
            runCatching {
                MobileAds.initialize(context) {}
                loadRewardedAd()
                loadInterstitialAd()
            }
        }
    }

    fun loadRewardedAd() {
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _rewardedAdReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _rewardedAdReady.value = false
                }
            }
        )
    }

    fun loadInterstitialAd() {
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _interstitialReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _interstitialReady.value = false
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFailed()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            onFailed()
            loadRewardedAd()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedAdReady.value = false
                loadRewardedAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _rewardedAdReady.value = false
                loadRewardedAd()
                onFailed()
            }
        }
        ad.show(activity) { onRewarded() }
    }

    /** Shows interstitial for free users before high-value actions (export). Proceeds even if ad unavailable. */
    fun showInterstitialThen(
        activity: Activity,
        isPro: Boolean,
        onComplete: () -> Unit
    ) {
        if (isPro) {
            onComplete()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitialAd()
            onComplete()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _interstitialReady.value = false
                loadInterstitialAd()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                _interstitialReady.value = false
                loadInterstitialAd()
                onComplete()
            }
        }
        ad.show(activity)
    }
}
