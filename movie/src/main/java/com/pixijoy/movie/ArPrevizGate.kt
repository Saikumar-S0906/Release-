package com.pixijoy.movie

import android.content.Context
import android.content.Intent

/**
 * F4 ARCore previz gate. Lives in `:movie` without linking ARCore into the base APK.
 * Device check + launch use reflection against the on-demand `:feature_ar` split.
 */
object ArPrevizGate {
    fun isSupported(context: Context): Boolean =
        runCatching {
            val apk = Class.forName("com.google.ar.core.ArCoreApk")
            val getInstance = apk.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            val check = apk.getMethod("checkAvailability", Context::class.java)
            val availability = check.invoke(instance, context.applicationContext)
            val name = availability.javaClass.getMethod("name").invoke(availability) as String
            name == "SUPPORTED_INSTALLED" || name == "SUPPORTED_APK_TOO_OLD" || name == "SUPPORTED_NOT_INSTALLED"
        }.getOrDefault(false)

    fun launchPreviz(context: Context, titleText: String = "PixiJoy"): Boolean =
        runCatching {
            val clazz = Class.forName("com.pixijoy.feature.ar.ArPrevizActivity")
            val intent = Intent(context, clazz).putExtra("title_text", titleText)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
}
