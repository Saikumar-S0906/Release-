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
 * Samples the clip's source window ([inMs]..[outMs]), not output duration, so
 * trim and speed keep follow on the frames that actually play.
 * Does not invent a path when [TrackFacade] is unavailable — anchors fall back
 * to the seed point so classical follow stays honest.
 */
object TrackPathBake {
    private const val SAMPLES = 10
    private const val THUMB = 320

    /**
     * Source windows shorter than this are not a Track-ML sample.
     * [MovieSpeed.expandRamp] must not pass those slices — it keeps the parent
     * window (often ~167 ms steps on a one-second clip).
     */
    const val MIN_ML_SOURCE_MS = 200L

    fun acceptsMlSourceSpan(sourceSpanMs: Long): Boolean = sourceSpanMs >= MIN_ML_SOURCE_MS

    /**
     * Follow curve for [clip], baked on the parent source window and sliced to
     * this clip when a speed ramp expanded it.
     */
    fun followMotion(path: String, clip: TimelineClip): ClipMotion {
        val baked = motion(path, clip.track, clip.trackBakeIn(), clip.trackBakeOut())
        if (baked.isIdentity) return baked
        val (t0, t1) = clip.trackBakeWindow()
        return baked.withWindow(t0, t1)
    }

    fun followAnchors(path: String, clip: TimelineClip): TrackAnchorPath =
        anchors(path, clip.track, clip.trackBakeIn(), clip.trackBakeOut())

    private data class Key(
        val path: String,
        val modifiedMs: Long,
        val sizeBytes: Long,
        val trackEncode: String,
        val inMs: Long,
        val outMs: Long
    )

    private data class BakeResult(
        val motion: ClipMotion,
        val anchors: TrackAnchorPath
    )

    private val lock = Any()
    private val cache = LruCache<Key, BakeResult>(12)

    fun motion(path: String, track: ClipTrack, inMs: Long, outMs: Long): ClipMotion =
        bakeCached(path, track, inMs, outMs).motion

    fun anchors(path: String, track: ClipTrack, inMs: Long, outMs: Long): TrackAnchorPath =
        bakeCached(path, track, inMs, outMs).anchors

    fun clearCache() {
        synchronized(lock) { cache.evictAll() }
    }

    fun panX(nx: Float): Float = ((0.5f - nx) * 0.75f).coerceIn(-0.35f, 0.35f)

    fun panY(ny: Float): Float = ((0.5f - ny) * 0.75f).coerceIn(-0.35f, 0.35f)

    /**
     * Source media time (ms) for bake sample [index] of [count] across the
     * clip's [inMs]..[outMs] window. Exposed for units; not a public API contract.
     */
    fun sourceSampleMs(index: Int, count: Int, inMs: Long, outMs: Long): Long {
        val start = inMs.coerceAtLeast(0L)
        val end = outMs.coerceAtLeast(start + 1L)
        if (count <= 1) return start
        val i = index.coerceIn(0, count - 1)
        return start + ((end - start) * i / (count - 1))
    }

    private fun followZoom(radius: Float): Float =
        (1.08f + (0.12f - radius).coerceIn(0f, 0.1f)).coerceIn(1.05f, 1.25f)

    private fun bakeCached(path: String, track: ClipTrack, inMs: Long, outMs: Long): BakeResult {
        val seed = TrackAnchorPath.seed(track.x, track.y)
        if (!track.isActive || track.resolvedEngine() != TrackEngine.Ml) {
            return BakeResult(ClipMotion.None, seed)
        }
        if (!TrackFacade.mlAvailable()) {
            return BakeResult(ClipMotion.None, seed)
        }
        val start = inMs.coerceAtLeast(0L)
        val end = outMs.coerceAtLeast(start + 1L)
        val sourceSpan = (end - start).coerceAtLeast(1L)
        val file = File(path)
        if (!file.isFile || !acceptsMlSourceSpan(sourceSpan)) {
            return BakeResult(ClipMotion.None, seed)
        }
        val key = Key(
            path = file.absolutePath,
            modifiedMs = file.lastModified(),
            sizeBytes = file.length(),
            trackEncode = track.encode(),
            inMs = start,
            outMs = end
        )
        synchronized(lock) {
            cache.get(key)?.let { return it }
        }
        val baked = bake(file.absolutePath, track.normalized(), start, end, seed)
        synchronized(lock) {
            cache.put(key, baked)
        }
        return baked
    }

    private fun bake(
        path: String,
        track: ClipTrack,
        inMs: Long,
        outMs: Long,
        seed: TrackAnchorPath
    ): BakeResult {
        val frames = sampleFrames(path, SAMPLES, inMs, outMs)
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

    private fun sampleFrames(path: String, count: Int, inMs: Long, outMs: Long): List<Bitmap>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val out = ArrayList<Bitmap>(count)
            for (i in 0 until count) {
                val tMs = sourceSampleMs(i, count, inMs, outMs)
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
