package com.pixijoy.movie

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File

data class FilmstripFrame(val bitmap: Bitmap, val timeMs: Long)

object Filmstrip {
    private const val THUMB_WIDTH = 160
    private const val THUMB_HEIGHT = 90
    private const val CACHE_KB = 12 * 1024
    private val cacheLock = Any()

    private data class CacheKey(
        val path: String,
        val modifiedMs: Long,
        val sizeBytes: Long,
        val count: Int,
        val startMs: Long,
        val endMs: Long
    )

    private val cache = object : LruCache<CacheKey, List<FilmstripFrame>>(CACHE_KB) {
        override fun sizeOf(key: CacheKey, value: List<FilmstripFrame>): Int =
            value.sumOf { frame -> frame.bitmap.allocationByteCount / 1024 }.coerceAtLeast(1)

        override fun entryRemoved(
            evicted: Boolean,
            key: CacheKey,
            oldValue: List<FilmstripFrame>,
            newValue: List<FilmstripFrame>?
        ) {
            if (oldValue !== newValue) oldValue.recycleFrames()
        }
    }

    /**
     * Returned bitmaps are owned by the caller and must be recycled. Cached masters are copied so
     * eviction cannot recycle a bitmap that Compose is still displaying.
     */
    fun thumbs(path: String, count: Int): List<Bitmap> = frames(path, count).map { it.bitmap }

    fun frames(
        path: String,
        count: Int,
        startMs: Long = 0,
        endMs: Long = Long.MAX_VALUE
    ): List<FilmstripFrame> {
        val n = count.coerceIn(1, 16)
        val file = File(path)
        if (!file.exists()) return emptyList()
        val key = CacheKey(
            path = file.absolutePath,
            modifiedMs = file.lastModified(),
            sizeBytes = file.length(),
            count = n,
            startMs = startMs.coerceAtLeast(0),
            endMs = endMs
        )
        synchronized(cacheLock) {
            cache.get(key)?.let { return it.copyFrames() }
        }

        val decoded = decodeFrames(file.absolutePath, n, startMs, endMs)
        if (decoded.isEmpty()) return emptyList()
        synchronized(cacheLock) {
            cache.get(key)?.let { existing ->
                decoded.recycleFrames()
                return existing.copyFrames()
            }
            val ownedCopies = decoded.copyFrames()
            cache.put(key, decoded)
            return ownedCopies
        }
    }

    fun clearCache() {
        synchronized(cacheLock) {
            cache.evictAll()
        }
    }

    private fun decodeFrames(
        path: String,
        count: Int,
        startMs: Long,
        endMs: Long
    ): List<FilmstripFrame> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return emptyList()
            val from = startMs.coerceIn(0, duration)
            val to = endMs.coerceIn((from + 1).coerceAtMost(duration), duration)
            val span = (to - from).coerceAtLeast(1)
            (0 until count).mapNotNull { i ->
                val timeMs = (from + ((i + 0.5) * span / count).toLong()).coerceAtMost(to)
                retriever.getScaledFrameAtTime(
                    timeMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    THUMB_WIDTH,
                    THUMB_HEIGHT
                )?.let { frame ->
                    FilmstripFrame(frame, timeMs)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            retriever.release()
        }
    }

    private fun List<FilmstripFrame>.copyFrames(): List<FilmstripFrame> =
        mapNotNull { frame ->
            if (frame.bitmap.isRecycled) {
                null
            } else {
                frame.bitmap.copy(Bitmap.Config.ARGB_8888, false)?.let { copy ->
                    FilmstripFrame(copy, frame.timeMs)
                }
            }
        }

    private fun List<FilmstripFrame>.recycleFrames() {
        forEach { frame ->
            if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
        }
    }
}
