package com.pixijoy.ai.analytics

import android.content.Context
import android.os.Bundle
import com.pixijoy.ai.data.ConsentRepository
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-compliant analytics facade.
 * Uses Firebase when google-services.json is present; falls back to debug logs otherwise.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentRepository: ConsentRepository
) {
    private val firebase: FirebaseAnalytics? by lazy {
        runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
    }

    private fun isEnabled(): Boolean = runBlocking {
        consentRepository.analyticsConsent.first()
    }

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        if (!isEnabled()) return
        val bundle = params.toBundle()
        firebase?.logEvent(name, bundle) ?: run {
            android.util.Log.d(TAG, "Analytics: $name $params")
        }
    }

    fun logScreen(screenName: String) {
        if (!isEnabled()) return
        firebase?.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName) }
        ) ?: logEvent("screen_view", mapOf("screen_name" to screenName))
    }

    fun logExport(format: String, isPro: Boolean) {
        logEvent("export", mapOf("format" to format, "is_pro" to isPro.toString()))
    }

    fun logAdImpression(type: String) {
        logEvent("ad_impression", mapOf("ad_type" to type))
    }

    private fun Map<String, String>.toBundle(): Bundle = Bundle().apply {
        forEach { (k, v) -> putString(k, v) }
    }

    companion object {
        private const val TAG = "PixiJoyAnalytics"
    }
}
