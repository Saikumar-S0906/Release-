package com.pixijoy.ai

import android.app.Application
import com.pixijoy.ai.export.MovieExportWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PixiJoyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MovieExportWorker.ensureChannel(this)
    }
}
