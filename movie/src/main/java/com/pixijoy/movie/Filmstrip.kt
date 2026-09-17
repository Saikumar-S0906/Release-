package com.pixijoy.movie

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever

data class FilmstripFrame(val bitmap: Bitmap, val timeMs: Long)

object Filmstrip {
    fun thumbs(path: String, count: Int): List<Bitmap> = frames(path, count).map { it.bitmap }

    fun frames(
        path: String,
        count: Int,
        startMs: Long = 0,
        endMs: Long = Long.MAX_VALUE
    ): List<FilmstripFrame> {
        val n = count.coerceIn(4, 16)
        val file = java.io.File(path)
        if (!file.exists()) return emptyList()
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return emptyList()
            val from = startMs.coerceIn(0, duration)
            val to = endMs.coerceIn((from + 1).coerceAtMost(duration), duration)
            val span = (to - from).coerceAtLeast(1)
            val step = (span / n).coerceAtLeast(1)
            (0 until n).mapNotNull { i ->
                val timeMs = (from + i * step).coerceAtMost(to)
                retriever.getFrameAtTime(
                    timeMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )?.let { frame ->
                    val w = 120
                    val h = (frame.height * w / frame.width.toFloat()).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(frame, w, h, true)
                    if (scaled !== frame) frame.recycle()
                    FilmstripFrame(scaled, timeMs)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            retriever.release()
        }
    }
}
