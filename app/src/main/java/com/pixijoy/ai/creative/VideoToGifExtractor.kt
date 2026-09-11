package com.pixijoy.ai.creative

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoToGifExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun extractFrames(
        uri: Uri,
        maxFrames: Int = 12,
        maxDurationMs: Long = 4_000L
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtMost(maxDurationMs)
                ?: maxDurationMs
            val frameCount = maxFrames.coerceAtLeast(2)
            val step = durationMs / (frameCount - 1).coerceAtLeast(1)
            (0 until frameCount).mapNotNull { index ->
                val timeUs = (index * step) * 1_000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: return@mapNotNull null
                toSoftware(frame)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            retriever.release()
        }
    }

    private fun toSoftware(frame: Bitmap): Bitmap {
        if (frame.config != Bitmap.Config.HARDWARE) return frame
        val copy = frame.copy(Bitmap.Config.ARGB_8888, false)
        if (copy != null && copy !== frame && !frame.isRecycled) {
            runCatching { frame.recycle() }
        }
        return copy ?: frame
    }
}
