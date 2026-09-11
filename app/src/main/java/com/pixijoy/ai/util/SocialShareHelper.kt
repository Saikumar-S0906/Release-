package com.pixijoy.ai.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SocialPlatform(val label: String, val packageName: String?) {
    WHATSAPP("WhatsApp", "com.whatsapp"),
    INSTAGRAM("Instagram", "com.instagram.android"),
    SNAPCHAT("Snapchat", "com.snapchat.android"),
    TELEGRAM("Telegram", "org.telegram.messenger"),
    TWITTER("X (Twitter)", "com.twitter.android"),
    GENERIC("More Apps", null)
}

@Singleton
class SocialShareHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createShareIntent(uri: Uri, platform: SocialPlatform, mimeType: String = "image/png"): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            platform.packageName?.let { setPackage(it) }
        }
        return intent
    }

    fun createSnapchatShareIntent(uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(SocialPlatform.SNAPCHAT.packageName)
            putExtra("snapchat", true)
        }
        val snapPackage = SocialPlatform.SNAPCHAT.packageName
        return if (snapPackage != null && isAppInstalled(snapPackage)) {
            intent
        } else {
            createShareIntent(uri, SocialPlatform.GENERIC)
        }
    }

    fun createMultiShareIntent(uris: List<Uri>, platform: SocialPlatform): Intent {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            platform.packageName?.let { setPackage(it) }
        }
        return intent
    }

    fun isAppInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    fun availablePlatforms(): List<SocialPlatform> = SocialPlatform.entries.filter {
        it.packageName == null || isAppInstalled(it.packageName)
    }
}
