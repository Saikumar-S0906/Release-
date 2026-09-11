package com.pixijoy.ai.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.pixijoy.ai.R

fun Context.launchShareIntent(intent: Intent, chooserTitle: String? = null): Boolean {
    return runCatching {
        val launch = chooserTitle?.let { Intent.createChooser(intent, it) } ?: intent
        startActivity(launch)
    }.isSuccess
}

fun Context.launchShareIntentSafe(
    intent: Intent,
    chooserTitle: String? = null,
    onFailure: (() -> Unit)? = null
): Boolean {
    return try {
        val launch = chooserTitle?.let { Intent.createChooser(intent, it) } ?: intent
        startActivity(launch)
        true
    } catch (_: ActivityNotFoundException) {
        onFailure?.invoke()
        Toast.makeText(this, getString(R.string.share_app_not_found), Toast.LENGTH_SHORT).show()
        false
    } catch (_: Exception) {
        onFailure?.invoke()
        Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
        false
    }
}
