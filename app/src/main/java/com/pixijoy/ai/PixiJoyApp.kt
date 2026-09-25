package com.pixijoy.ai

import android.app.Application
import android.content.Context
import com.pixijoy.ai.export.ExportNotifications
import com.pixijoy.ml.PackSplits
import com.pixijoy.ml.pack.PackRuntime
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PixiJoyApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        PackSplits.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        PackRuntime.bind(this)
        ExportNotifications.ensureChannel(this)
    }
}
