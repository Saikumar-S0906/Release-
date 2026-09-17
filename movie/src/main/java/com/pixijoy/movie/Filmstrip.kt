package com.pixijoy.movie

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
object Filmstrip {
    fun thumbs(path: String, count: Int): List<Bitmap> {
        val n = count.coerceIn(4, 16)
        val file = java.io.File(path)
        if (!file.exists()) return emptyList()
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return emptyList()
            val step = (duration / n).coerceAtLeast(1)
            (0 until n).mapNotNull { i ->
                retriever.getFrameAtTime(
                    i * step * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )?.let { frame ->
                    val w = 120
                    val h = (frame.height * w / frame.width.toFloat()).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(frame, w, h, true)
                    if (scaled !== frame) frame.recycle()
                    scaled
                }
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            retriever.release()
        }
    }
}
