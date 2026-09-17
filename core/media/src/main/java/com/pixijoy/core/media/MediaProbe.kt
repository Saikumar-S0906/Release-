package com.pixijoy.core.media

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.File

data class MediaInfo(
    val mime: String,
    val width: Int,
    val height: Int,
    val durationMs: Long? = null,
    val fps: Float? = null
)

object MediaProbe {
    fun probe(file: File): MediaInfo {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            return MediaInfo(
                mime = bounds.outMimeType ?: "image/jpeg",
                width = bounds.outWidth,
                height = bounds.outHeight
            )
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: if (w > 0 && h > 0) "video/mp4" else "audio/mp4"
            val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull()
            MediaInfo(mime = mime, width = w, height = h, durationMs = duration, fps = fps)
        } finally {
            retriever.release()
        }
    }
}
