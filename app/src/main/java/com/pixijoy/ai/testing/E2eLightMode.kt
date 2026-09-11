package com.pixijoy.ai.testing

import android.content.Context
import com.pixijoy.ai.BuildConfig

/**
 * Light mode for Maestro / device farm runs: skip AdMob + Play Billing init so cold start
 * does not ANR under automation load. Enable with:
 * `adb shell setprop debug.pixijoy.e2e 1`
 */
object E2eLightMode {
    fun enabled(context: Context? = null): Boolean {
        if (!BuildConfig.DEBUG) return false
        if (systemProperty("debug.pixijoy.e2e") == "1") return true
        if (System.getProperty("pixijoy.e2e") == "1") return true
        // Maestro driver package present on device
        if (context != null) {
            val pm = context.packageManager
            val maestroPkgs = listOf(
                "dev.mobile.maestro",
                "dev.mobile.maestro.debug",
                "com.mobile.dev.maestro"
            )
            if (maestroPkgs.any { pkg ->
                    runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
                }
            ) {
                return true
            }
        }
        return isRunningUnderInstrumentation()
    }

    private fun systemProperty(key: String): String =
        runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java, String::class.java)
            get.invoke(null, key, "") as String
        }.getOrDefault("")

    private fun isRunningUnderInstrumentation(): Boolean =
        try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (_: Throwable) {
            false
        }
}
