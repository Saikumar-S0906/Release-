package com.pixijoy.ai

import android.app.Application
import com.pixijoy.ai.ads.AdMobManager
import com.pixijoy.ai.notifications.NotificationHelper
import com.pixijoy.ai.qa.QaReportManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PixiJoyApp : Application() {

    @Inject lateinit var adMobManager: AdMobManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var qaReportManager: QaReportManager

    override fun onCreate() {
        super.onCreate()
        qaReportManager.install()
    }
}
