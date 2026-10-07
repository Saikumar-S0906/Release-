package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.media3.effect.MatrixTransformation
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.pack.PackId
import java.io.File
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * AI stabilize: NeuFlow v2 (Flow pack, on device) measures per-frame camera shake, then the
 * smoothed camera path is subtracted. Only the zoom needed to hide the *measured* correction
 * is applied, instead of the classical constant crop-zoom ([ClipFrame.toEffects]).
 *
 * Falls back to classical (returns null) when the Flow engine is not bound or the bake fails.
 * Baked synchronously like [TrackPathBake] and cached per file + window + strength.
 */
object FlowStabilize {
    /** Sample rate for flow pairs; corrections are interpolated between samples. */
    private const val SAMPLE_FPS = 8
    private const val MAX_SAMPLES = 160
    private const val MAX_ZOOM = 1.2f

    data class Path(
        /** Clip-relative sample times (µs). */
        val timesUs: LongArray,
        /** Correction in NDC (x right, y up) per sample. */
        val dx: FloatArray,
        val dy: FloatArray,
        val zoom: Float
    ) {
        val isIdentity: Boolean get() = timesUs.size < 2

        fun correctionAt(tUs: Long): Pair<Float, Float> {
            if (timesUs.isEmpty()) return 0f to 0f
            if (tUs <= timesUs.first()) return dx.first() to dy.first()
            if (tUs >= timesUs.last()) return dx.last() to dy.last()
            var i = 1
            while (i < timesUs.size && timesUs[i] < tUs) i++
            val t0 = timesUs[i - 1]
            val f = (tUs - t0).toFloat() / (timesUs[i] - t0).coerceAtLeast(1L)
            return (dx[i - 1] + (dx[i] - dx[i - 1]) * f) to (dy[i - 1] + (dy[i] - dy[i - 1]) * f)
        }

        fun toMatrixTransformation(): MatrixTransformation = MatrixTransformation { tUs ->
            val (cx, cy) = correctionAt(tUs)
            Matrix().apply {
                postTranslate(cx, cy)
                postScale(zoom, zoom)
            }
        }
    }

    private data class Key(val path: String, val modified: Long, val inMs: Long, val outMs: Long, val strength: Int)

    private val lock = Any()
    private val cache = LruCache<Key, Path>(8)

    fun available(): Boolean = PackEngineHooks.hasEngine(PackId.Flow)

    /** Neural stabilize effect for [clip], or null → caller keeps the classical crop-zoom. */
    fun effect(path: String, clip: TimelineClip): MatrixTransformation? {
        val strength = clip.frame.normalized().stabilize
        if (strength < 0.01f || !available()) return null
        val file = File(path)
        if (!file.isFile) return null
        val inMs = clip.trackBakeIn().coerceAtLeast(0L)
        val outMs = clip.trackBakeOut().coerceAtLeast(inMs + 1L)
        val key = Key(file.absolutePath, file.lastModified(), inMs, outMs, (strength * 100).toInt())
        synchronized(lock) { cache.get(key) }?.let { return if (it.isIdentity) null else it.toMatrixTransformation() }
        val baked = runCatching { bake(file.absolutePath, inMs, outMs, strength) }.getOrNull() ?: return null
        synchronized(lock) { cache.put(key, baked) }
        return if (baked.isIdentity) null else baked.toMatrixTransformation()
    }

    fun clearCache() = synchronized(lock) { cache.evictAll() }

    private fun bake(path: String, inMs: Long, outMs: Long, strength: Float): Path? {
        val spanMs = outMs - inMs
        val count = (spanMs * SAMPLE_FPS / 1000L).toInt().coerceIn(2, MAX_SAMPLES)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val times = LongArray(count) { TrackPathBake.sourceSampleMs(it, count, inMs, outMs) }
            val rawX = FloatArray(count)
            val rawY = FloatArray(count)
            var prev: Bitmap? = frame(retriever, times[0]) ?: return null
            for (i in 1 until count) {
                val next = frame(retriever, times[i]) ?: break
                val flow = PackEngineHooks.tryFlow(prev!!, next)
                prev.recycle()
                prev = next
                val (mx, my) = if (flow != null) globalMotion(flow) else 0f to 0f
                // Cumulative camera path in NDC (256-grid px → NDC: ×2/256; image y down → NDC y up).
                rawX[i] = rawX[i - 1] + mx * 2f / EDGE
                rawY[i] = rawY[i - 1] - my * 2f / EDGE
            }
            prev?.recycle()
            val radius = (2f + strength * 10f)
            val smoothX = gaussian(rawX, radius)
            val smoothY = gaussian(rawY, radius)
            // Content moved by raw; shift back toward the smooth path.
            val dx = FloatArray(count) { (smoothX[it] - rawX[it]) * strength.coerceAtLeast(0.5f) }
            val dy = FloatArray(count) { (smoothY[it] - rawY[it]) * strength.coerceAtLeast(0.5f) }
            val maxShift = max(dx.maxOf { abs(it) }, dy.maxOf { abs(it) })
            val zoom = (1f + maxShift).coerceIn(1f, MAX_ZOOM)
            val limit = zoom - 1f
            for (i in 0 until count) {
                dx[i] = dx[i].coerceIn(-limit, limit)
                dy[i] = dy[i].coerceIn(-limit, limit)
            }
            return Path(LongArray(count) { (times[it] - inMs) * 1000L }, dx, dy, zoom)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private const val EDGE = 256

    private fun frame(r: MediaMetadataRetriever, ms: Long): Bitmap? {
        val src = r.getScaledFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST, EDGE, EDGE) ?: return null
        if (src.width == EDGE && src.height == EDGE) return src
        return Bitmap.createScaledBitmap(src, EDGE, EDGE, true).also { if (it !== src) src.recycle() }
    }

    /** Robust global translation of an interleaved (dx, dy) flow: per-axis median, in 256-grid px. */
    internal fun globalMotion(flow: FloatArray): Pair<Float, Float> {
        val n = flow.size / 2
        if (n == 0) return 0f to 0f
        val step = max(1, n / 4096)
        val xs = FloatArray((n + step - 1) / step)
        val ys = FloatArray(xs.size)
        var k = 0
        var i = 0
        while (i < n && k < xs.size) {
            xs[k] = flow[2 * i]
            ys[k] = flow[2 * i + 1]
            k++
            i += step
        }
        xs.sort()
        ys.sort()
        return xs[k / 2] to ys[k / 2]
    }

    internal fun gaussian(v: FloatArray, sigma: Float): FloatArray {
        val r = (sigma * 2).toInt().coerceAtLeast(1)
        val w = FloatArray(2 * r + 1) { exp(-((it - r) * (it - r)) / (2f * sigma * sigma)) }
        return FloatArray(v.size) { i ->
            var s = 0f
            var ws = 0f
            for (j in -r..r) {
                val idx = (i + j).coerceIn(0, v.size - 1)
                s += v[idx] * w[j + r]
                ws += w[j + r]
            }
            s / ws
        }
    }
}
