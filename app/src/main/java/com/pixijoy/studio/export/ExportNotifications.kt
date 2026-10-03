package com.pixijoy.studio.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixijoy.studio.R

object ExportNotifications {
    const val CHANNEL_ID = "pixijoy_export"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val name = context.getString(R.string.studio_export)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun notification(context: Context, progress: Int) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(
            if (progress in 1..99) {
                context.getString(R.string.studio_export_pct, progress)
            } else {
                context.getString(R.string.studio_exporting)
            }
        )
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
        .build()
}
