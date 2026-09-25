package com.pixijoy.ai

import android.app.Application
import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat
import com.pixijoy.ai.export.ExportNotifications
import com.pixijoy.ml.PackLiteRt
import com.pixijoy.ml.pack.PackRuntime

class PixiJoyApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        runCatching { SplitCompat.install(this) }
    }

    override fun onCreate() {
        super.onCreate()
        PackRuntime.bind(this)
        PackLiteRt.start(this)
        ExportNotifications.ensureChannel(this)
    }
}
