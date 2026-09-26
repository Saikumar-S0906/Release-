package com.pixijoy.ai.ui.session

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pixijoy.ai.export.ExportNotifications

/**
 * Ensures the export notification channel exists and, on API 33+, asks for
 * [Manifest.permission.POST_NOTIFICATIONS] only when the user starts an export.
 * Export still proceeds if the user denies the permission.
 */
@Composable
fun rememberExportNotificationGate(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { PendingExport() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pending.runAndClear()
    }
    return remember(context, launcher) {
        { start: () -> Unit ->
            ExportNotifications.ensureChannel(context)
            if (Build.VERSION.SDK_INT < 33) {
                start()
                return@remember
            }
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                start()
            } else {
                pending.action = start
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private class PendingExport {
    var action: (() -> Unit)? = null
    fun runAndClear() {
        val next = action
        action = null
        next?.invoke()
    }
}
