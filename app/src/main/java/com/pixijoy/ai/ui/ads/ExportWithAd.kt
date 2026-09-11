package com.pixijoy.ai.ui.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pixijoy.ai.ads.AdMobManager
import dagger.hilt.android.EntryPointAccessors

@Composable
fun rememberAdMobManager(): AdMobManager {
    val context = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(
            context,
            com.pixijoy.ai.di.AdMobEntryPoint::class.java
        ).adMobManager()
    }
}

/** Wraps export/action for free users with an interstitial ad when available. */
@Composable
fun rememberExportWithAd(
    isPro: Boolean,
    onExport: () -> Unit
): () -> Unit {
    val adMobManager = rememberAdMobManager()
    val context = LocalContext.current
    val activity = context as? Activity
    return {
        if (activity != null) {
            adMobManager.showInterstitialThen(activity, isPro, onExport)
        } else {
            onExport()
        }
    }
}
