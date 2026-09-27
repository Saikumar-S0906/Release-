package com.pixijoy.movie

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bakes F1 track follow into a [ClipMotion] pan/zoom curve when the Track pack
 * engine is bound. Does not invent a path when [TrackFacade] is unavailable.
 */
object TrackPathBake {
    private const val SAMPLES = 10
    private const val THUMB = 320

    private data class Key(
        val path: String,
        val modifiedMs: Long,
        val sizeBytes: Long,
        val trackEncode: String,
        val durationMs: Long
    )

    private val lock = Any()
    private val cache = LruCache<Key, ClipMotion>(12)

    fun motion(path: String, track: ClipTrack, durationMs: Long): ClipMotion {
        if (!track.isActive || track.resolvedEngine() != TrackEngine.Ml) {
            return ClipMotion.None
        }
        if (!TrackFacade.mlAvailable()) return ClipMotion.None
        val file = File(path)
        if (!file.isFile || durationMs < 200L) return ClipMotion.None
        val key = Key(
            path = file.absolutePath,
            modifiedMs = file.lastModified(),
            sizeBytes = file.length(),
            trackEncode = track.encode(),
            durationMs = durationMs
        )
        synchronized(lock) {
            cache.get(key)?.let { return it }
        }
        val baked = bake(file.absolutePath, track.normalized(), durationMs)
        synchronized(lock) {
            cache.put(key, baked)
        }
        return baked
    }

    fun clearCache() {
        synchronized(lock) { cache.evictAll() }
    }

    fun panX(nx: Float): Float = ((0.5f - nx) * 0.75f).coerceIn(-0.35f, 0.35f)

    fun panY(ny: Float): Float = ((0.5f - ny) * 0.75f).coerceIn(-0.35f, 0.35f)

    private fun followZoom(radius: Float): Float =
        (1.08f + (0.12f - radius).coerceIn(0f, 0.1f)).coerceIn(1.05f, 1.25f)

    private fun bake(path: String, track: ClipTrack, durationMs: Long): ClipMotion {
        val frames = sampleFrames(path, SAMPLES, durationMs) ?: return ClipMotion.None
        try {
            if (frames.isEmpty()) return ClipMotion.None
            var x = track.x
            var y = track.y
            val keys = ArrayList<TransformKeyframe>(frames.size)
            keys += TransformKeyframe(
                0f,
                scale = followZoom(track.radius),
                tx = panX(x),
                ty = panY(y)
            )
            for (i in frames.indices) {
                val next = TrackFacade.follow(frames[i], x, y) ?: continue
                x = next.first
                y = next.second
                val t = if (frames.size <= 1) 1f else i / (frames.size - 1).toFloat()
                if (i == 0) {
                    keys[0] = TransformKeyframe(
                        0f,
                        scale = followZoom(track.radius),
                        tx = panX(x),
                        ty = panY(y)
                    )
                } else {
                    keys += TransformKeyframe(
                        t = t,
                        scale = followZoom(track.radius),
                        tx = panX(x),
                        ty = panY(y)
                    )
                }
            }
            return if (keys.size < 2) ClipMotion.None else ClipMotion(keys)
        } finally {
            frames.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun sampleFrames(path: String, count: Int, durationMs: Long): List<Bitmap>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val out = ArrayList<Bitmap>(count)
            for (i in 0 until count) {
                val tMs = if (count <= 1) 0L else (durationMs * i / (count - 1)).coerceAtLeast(0L)
                val raw = retriever.getFrameAtTime(
                    tMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: continue
                val scaled = scaleDown(raw, THUMB)
                if (scaled !== raw && !raw.isRecycled) raw.recycle()
                out += scaled
            }
            if (out.isEmpty()) null else out
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleDown(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = max(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val scale = maxEdge / longEdge.toFloat()
        val w = (src.width * scale).roundToInt().coerceAtLeast(1)
        val h = (src.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }
}
