package com.pixijoy.ai

import android.app.Application
import com.pixijoy.ai.export.ExportNotifications

class PixiJoyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ExportNotifications.ensureChannel(this)
    }
}
