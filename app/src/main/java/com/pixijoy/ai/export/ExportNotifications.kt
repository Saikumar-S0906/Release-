package com.pixijoy.ai.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object ExportNotifications {
    const val CHANNEL_ID = "pixijoy_export"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Export", NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun notification(context: Context, progress: Int) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("PixiJoy")
        .setContentText(if (progress in 1..99) "Exporting $progress%" else "Exporting…")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
        .build()
}
