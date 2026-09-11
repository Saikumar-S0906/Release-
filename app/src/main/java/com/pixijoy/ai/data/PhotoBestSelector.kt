package com.pixijoy.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.pixijoy.ai.ml.ImageContextAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoBestSelector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contextAnalyzer: ImageContextAnalyzer
) {
    suspend fun selectBestFromGallery(maxPhotos: Int = 12): Uri? = withContext(Dispatchers.IO) {
        val uris = queryRecentPhotos(maxPhotos)
        if (uris.isEmpty()) return@withContext null
        var bestUri = uris.first()
        var bestScore = -1f
        for (uri in uris) {
            val score = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream) ?: return@runCatching -1f
                    scorePhoto(bmp)
                } ?: -1f
            }.getOrDefault(-1f)
            if (score > bestScore) {
                bestScore = score
                bestUri = uri
            }
        }
        bestUri
    }

    suspend fun scorePhoto(bitmap: Bitmap): Float {
        val ctx = contextAnalyzer.analyze(bitmap)
        var score = 0f
        if (ctx.faceCount > 0) score += 3f
        if (ctx.isSelfie) score += 2f
        if (ctx.isBirthday) score += 2f
        if (ctx.isGroup) score += 1f
        score += ctx.labels.size * 0.2f
        val pixels = bitmap.width * bitmap.height
        score += when {
            pixels in 500_000..2_000_000 -> 2f
            pixels > 200_000 -> 1f
            else -> 0f
        }
        return score
    }

    private fun queryRecentPhotos(limit: Int): List<Uri> {
        return runCatching {
            val uris = mutableListOf<Uri>()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sort
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    uris.add(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()))
                    count++
                }
            }
            uris
        }.getOrDefault(emptyList())
    }
}
