package com.pixijoy.ai.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.pixijoy.ai.PixiJoyApp

/**
 * Uses the real [PixiJoyApp] so Compose UI tests that launch [com.pixijoy.ai.MainActivity]
 * get a full Hilt graph. HiltTestApplication caused startup ANRs under instrumentation.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, PixiJoyApp::class.java.name, context)
    }
}
