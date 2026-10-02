package com.pixijoy.movie

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bakes F1 track follow into a [ClipMotion] pan/zoom curve and a matching
 * [TrackAnchorPath] for CG / VFX attach when the Track pack engine is bound.
 * Does not invent a path when [TrackFacade] is unavailable — anchors fall back
 * to the seed point so classical follow stays honest.
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

    private data class BakeResult(
        val motion: ClipMotion,
        val anchors: TrackAnchorPath
    )

    private val lock = Any()
    private val cache = LruCache<Key, BakeResult>(12)

    fun motion(path: String, track: ClipTrack, durationMs: Long): ClipMotion =
        bakeCached(path, track, durationMs).motion

    fun anchors(path: String, track: ClipTrack, durationMs: Long): TrackAnchorPath =
        bakeCached(path, track, durationMs).anchors

    fun clearCache() {
        synchronized(lock) { cache.evictAll() }
    }

    fun panX(nx: Float): Float = ((0.5f - nx) * 0.75f).coerceIn(-0.35f, 0.35f)

    fun panY(ny: Float): Float = ((0.5f - ny) * 0.75f).coerceIn(-0.35f, 0.35f)

    private fun followZoom(radius: Float): Float =
        (1.08f + (0.12f - radius).coerceIn(0f, 0.1f)).coerceIn(1.05f, 1.25f)

    private fun bakeCached(path: String, track: ClipTrack, durationMs: Long): BakeResult {
        val seed = TrackAnchorPath.seed(track.x, track.y)
        if (!track.isActive || track.resolvedEngine() != TrackEngine.Ml) {
            return BakeResult(ClipMotion.None, seed)
        }
        if (!TrackFacade.mlAvailable()) {
            return BakeResult(ClipMotion.None, seed)
        }
        val file = File(path)
        if (!file.isFile || durationMs < 200L) {
            return BakeResult(ClipMotion.None, seed)
        }
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
        val baked = bake(file.absolutePath, track.normalized(), durationMs, seed)
        synchronized(lock) {
            cache.put(key, baked)
        }
        return baked
    }

    private fun bake(
        path: String,
        track: ClipTrack,
        durationMs: Long,
        seed: TrackAnchorPath
    ): BakeResult {
        val frames = sampleFrames(path, SAMPLES, durationMs)
            ?: return BakeResult(ClipMotion.None, seed)
        try {
            if (frames.isEmpty()) return BakeResult(ClipMotion.None, seed)
            var x = track.x
            var y = track.y
            val motionKeys = ArrayList<TransformKeyframe>(frames.size)
            val anchorKeys = ArrayList<TrackAnchorKeyframe>(frames.size)
            motionKeys += TransformKeyframe(
                0f,
                scale = followZoom(track.radius),
                tx = panX(x),
                ty = panY(y)
            )
            anchorKeys += TrackAnchorKeyframe(0f, x, y)
            for (i in frames.indices) {
                val next = TrackFacade.follow(frames[i], x, y) ?: continue
                x = next.first
                y = next.second
                val t = if (frames.size <= 1) 1f else i / (frames.size - 1).toFloat()
                if (i == 0) {
                    motionKeys[0] = TransformKeyframe(
                        0f,
                        scale = followZoom(track.radius),
                        tx = panX(x),
                        ty = panY(y)
                    )
                    anchorKeys[0] = TrackAnchorKeyframe(0f, x, y)
                } else {
                    motionKeys += TransformKeyframe(
                        t = t,
                        scale = followZoom(track.radius),
                        tx = panX(x),
                        ty = panY(y)
                    )
                    anchorKeys += TrackAnchorKeyframe(t, x, y)
                }
            }
            val motion = if (motionKeys.size < 2) ClipMotion.None else ClipMotion(motionKeys)
            val anchors = if (anchorKeys.isEmpty()) seed else TrackAnchorPath(anchorKeys)
            return BakeResult(motion, anchors)
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
