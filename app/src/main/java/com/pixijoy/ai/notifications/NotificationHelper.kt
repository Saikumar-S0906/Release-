package com.pixijoy.ai.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixijoy.ai.MainActivity
import com.pixijoy.ai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                context.getString(R.string.notification_channel_general),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHALLENGES,
                context.getString(R.string.notification_channel_challenges),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERATION,
                context.getString(R.string.notification_channel_generation),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }

    private fun boardScreenIntent(
        screenId: String,
        requestCode: Int
    ): PendingIntent {
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_BOARD_SCREEN, screenId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showDailyChallengeNotification() {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_CHALLENGES)
            .setSmallIcon(R.drawable.ic_stat_pixijoy)
            .setContentTitle(context.getString(R.string.notification_daily_challenge_title))
            .setContentText(context.getString(R.string.notification_daily_challenge_body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(boardScreenIntent("b2_discover", DAILY_CHALLENGE_ID))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(DAILY_CHALLENGE_ID, notification)
    }

    fun showGenerateProgress(progressPercent: Int, title: String? = null) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val pct = progressPercent.coerceIn(0, 100)
        val screenId = if (pct >= 100) "b19_success" else "b19_generating"
        val notification = NotificationCompat.Builder(context, CHANNEL_GENERATION)
            .setSmallIcon(R.drawable.ic_stat_pixijoy)
            .setContentTitle(title ?: context.getString(R.string.notification_generate_title))
            .setContentText(context.getString(R.string.notification_generate_body, pct))
            .setProgress(100, pct, pct <= 0)
            .setOngoing(pct < 100)
            .setOnlyAlertOnce(true)
            .setContentIntent(boardScreenIntent(screenId, GENERATE_PROGRESS_ID))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        NotificationManagerCompat.from(context).notify(GENERATE_PROGRESS_ID, notification)
    }

    fun cancelGenerateProgress() {
        NotificationManagerCompat.from(context).cancel(GENERATE_PROGRESS_ID)
    }

    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    companion object {
        const val CHANNEL_GENERAL = "pixijoy_general"
        const val CHANNEL_CHALLENGES = "pixijoy_challenges"
        const val CHANNEL_GENERATION = "pixijoy_generation"
        private const val DAILY_CHALLENGE_ID = 1001
        private const val GENERATE_PROGRESS_ID = 1002
    }
}
