package com.pixijoy.ai.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.pixijoy.ai.domain.model.SocialFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapchatExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageExporter: ImageExporter,
    private val socialShareHelper: SocialShareHelper
) {
    /** Snapchat prefers 9:16 vertical content with transparent PNG for stickers. */
    suspend fun prepareSnapShare(bitmap: Bitmap, transparent: Boolean = false): Uri? =
        withContext(Dispatchers.IO) {
            val cropped = cropToSnap(bitmap)
            imageExporter.createShareUri(cropped, addWatermark = false)
        }

    suspend fun createSnapIntent(bitmap: Bitmap): Intent? {
        val uri = prepareSnapShare(bitmap) ?: return null
        return socialShareHelper.createSnapchatShareIntent(uri)
    }

    private fun cropToSnap(bitmap: Bitmap): Bitmap {
        val targetW = 1080
        val targetH = 1920
        val scale = maxOf(targetW.toFloat() / bitmap.width, targetH.toFloat() / bitmap.height)
        val w = (bitmap.width * scale).toInt()
        val h = (bitmap.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val x = ((w - targetW) / 2).coerceAtLeast(0)
        val y = ((h - targetH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(scaled, x, y, targetW.coerceAtMost(scaled.width), targetH.coerceAtMost(scaled.height))
    }
}
