package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.media3.common.Effect
import androidx.media3.common.GlTextureInfo
import androidx.media3.common.util.GlRect
import androidx.media3.common.util.Size
import androidx.media3.effect.ByteBufferGlEffect
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.pack.PackModelContract
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal enum class DepthOffer { Start, Queue, Skip }

/**
 * Decides which frames get a depth sample. One inference is in flight.
 * A newer frame is kept and run next instead of dropped. A frame in the
 * same [bucketUs] as the published map, or as the queued frame, is skipped.
 */
internal data class DepthGate(
    val busy: Boolean = false,
    val inFlightUs: Long? = null,
    val pendingUs: Long? = null,
    val publishedUs: Long? = null,
    val bucketUs: Long = DepthFrames.BUCKET_US
) {
    fun offer(timeUs: Long): Pair<DepthGate, DepthOffer> {
        if (sameBucket(publishedUs, timeUs) ||
            sameBucket(pendingUs, timeUs) ||
            sameBucket(inFlightUs, timeUs)
        ) {
            return this to DepthOffer.Skip
        }
        if (busy) return copy(pendingUs = timeUs) to DepthOffer.Queue
        return copy(busy = true, inFlightUs = timeUs) to DepthOffer.Start
    }

    /** Next sample time after the in-flight frame, or null when the queue is idle. */
    fun finished(published: Boolean, timeUs: Long): Pair<DepthGate, Long?> {
        val publishedUs = if (published) timeUs else this.publishedUs
        val next = pendingUs
        if (next == null || sameBucket(publishedUs, next)) {
            return copy(
                busy = false,
                inFlightUs = null,
                pendingUs = null,
                publishedUs = publishedUs
            ) to null
        }
        return copy(
            busy = true,
            inFlightUs = next,
            pendingUs = null,
            publishedUs = publishedUs
        ) to next
    }

    fun withPublished(timeUs: Long): DepthGate = copy(publishedUs = timeUs)

    private fun sameBucket(anchorUs: Long?, timeUs: Long): Boolean =
        anchorUs != null && timeUs / bucketUs == anchorUs / bucketUs
}

/**
 * Latest relative-depth map for soft Z. Empty until a bound depth engine
 * publishes one. A miss keeps the flat Z-order insert.
 *
 * Inference is slower than the frame rate. The newest waiting frame is run
 * when the current one finishes, and the map stays usable forward for
 * [HOLD_US] so the plate does not pop back before the next sample.
 */
internal object DepthFrames {
    const val BUCKET_US = 200_000L
    const val SLACK_US = 500_000L
    const val HOLD_US = 4_000_000L

    private val latest = AtomicReference<Pair<Long, FloatArray>?>(null)
    private val generation = AtomicInteger(0)
    private val lock = Any()
    private var gate = DepthGate()
    private var pendingFrame: Bitmap? = null
    private val exec = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "pixijoy-depth").apply { isDaemon = true }
    }

    fun publish(timeUs: Long, depth: FloatArray) {
        latest.set(timeUs to depth)
        generation.incrementAndGet()
        synchronized(lock) { gate = gate.withPublished(timeUs) }
    }

    /** Bumps on every [publish] so a cached plate can redraw once the map exists. */
    fun generation(): Int = generation.get()

    /**
     * Map for [timeUs], or null. A sample covers [SLACK_US] before it and
     * [HOLD_US] after it. Outside that window the plate stays on manual Z.
     */
    fun at(timeUs: Long): FloatArray? {
        val snap = latest.get() ?: return null
        val delta = timeUs - snap.first
        if (delta >= 0) {
            if (delta > HOLD_US) return null
        } else if (-delta > SLACK_US) {
            return null
        }
        return snap.second
    }

    /**
     * Takes ownership of [frame] and recycles it. Skips a frame in the
     * published bucket. While a sample is running, keeps only the newest
     * other frame and runs that next.
     */
    fun request(timeUs: Long, frame: Bitmap) {
        val start = synchronized(lock) {
            val (next, offer) = gate.offer(timeUs)
            gate = next
            when (offer) {
                DepthOffer.Skip -> {
                    recycle(frame)
                    false
                }
                DepthOffer.Queue -> {
                    recycle(pendingFrame)
                    pendingFrame = frame
                    false
                }
                DepthOffer.Start -> true
            }
        }
        if (start) exec.execute { drain(timeUs, frame) }
    }

    private fun drain(timeUs: Long, frame: Bitmap) {
        var sampleUs = timeUs
        var sample = frame
        while (true) {
            var published = false
            try {
                val depth = PackEngineHooks.tryDepth(sample)
                val edge = PackModelContract.DEPTH_EDGE
                if (depth != null && depth.size == edge * edge) {
                    publish(sampleUs, depth)
                    published = true
                }
            } catch (_: Exception) {
                published = false
            } finally {
                recycle(sample)
            }
            val next = synchronized(lock) {
                val (nextGate, nextUs) = gate.finished(published, sampleUs)
                gate = nextGate
                if (nextUs == null) {
                    recycle(pendingFrame)
                    pendingFrame = null
                    null
                } else {
                    val queued = pendingFrame
                    pendingFrame = null
                    if (queued == null) {
                        gate = gate.copy(busy = false, inFlightUs = null)
                        null
                    } else {
                        nextUs to queued
                    }
                }
            } ?: return
            sampleUs = next.first
            sample = next.second
        }
    }

    fun resetForTests() {
        synchronized(lock) {
            recycle(pendingFrame)
            pendingFrame = null
            gate = DepthGate()
        }
        latest.set(null)
        generation.set(0)
    }

    private fun recycle(frame: Bitmap?) {
        if (frame != null && !frame.isRecycled) frame.recycle()
    }
}

/** Higher [zOrder] is nearer. 0 = near, 1 = far, matching the depth contract. */
internal fun cgPlaneDepth(zOrder: Int): Float {
    val z = zOrder.coerceIn(-8, 8)
    return (8 - z) / 16f
}

/** Keep the plate where the scene is as far as the plate, or farther. */
internal fun softZKeepsPlate(sceneDepth: Float, planeDepth: Float): Boolean =
    sceneDepth >= planeDepth

/**
 * Clears alpha where the scene is nearer than [planeDepth]. RGB stays.
 * [depth] is row-major with the top row first. A short map leaves pixels unchanged.
 */
internal fun occludePlateByDepth(
    pixels: IntArray,
    width: Int,
    height: Int,
    depth: FloatArray,
    depthW: Int,
    depthH: Int,
    planeDepth: Float
): IntArray {
    if (width <= 0 || height <= 0 || depthW <= 0 || depthH <= 0) return pixels
    if (depth.size < depthW * depthH || pixels.size < width * height) return pixels
    for (y in 0 until height) {
        val dy = (y.toLong() * depthH / height).toInt().coerceIn(0, depthH - 1)
        val row = dy * depthW
        for (x in 0 until width) {
            val i = y * width + x
            val color = pixels[i]
            if ((color ushr 24) == 0) continue
            val dx = (x.toLong() * depthW / width).toInt().coerceIn(0, depthW - 1)
            if (!softZKeepsPlate(depth[row + dx], planeDepth)) {
                pixels[i] = color and 0x00ffffff
            }
        }
    }
    return pixels
}

/**
 * Samples scene depth on a background thread. The video frame is passed through.
 * Insert only for SDR: [ByteBufferGlEffect] rejects HDR, and Keep HDR stays on
 * manual Z-order.
 */
internal fun depthSampleEffect(): Effect = ByteBufferGlEffect(DepthFrameProcessor())

private class DepthFrameProcessor : ByteBufferGlEffect.Processor<Unit> {
    private var frameW = 1
    private var frameH = 1

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        frameW = inputWidth.coerceAtLeast(1)
        frameH = inputHeight.coerceAtLeast(1)
        val edge = PackModelContract.DEPTH_EDGE
        return Size(edge, edge)
    }

    override fun getScaledRegion(presentationTimeUs: Long): GlRect = GlRect(frameW, frameH)

    override fun processImage(
        image: ByteBufferGlEffect.Image,
        presentationTimeUs: Long
    ): ListenableFuture<Unit> {
        val frame = runCatching { uprightBitmap(image) }.getOrNull()
        if (frame != null) DepthFrames.request(presentationTimeUs, frame)
        return Futures.immediateFuture(Unit)
    }

    /**
     * OpenGL rows start at the bottom. The depth model and the plate punch
     * both expect the top row first. Recycle the intermediate copy.
     */
    private fun uprightBitmap(image: ByteBufferGlEffect.Image): Bitmap {
        val gl = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val buffer = image.pixelBuffer
        buffer.rewind()
        gl.copyPixelsFromBuffer(buffer)
        val upright = Bitmap.createBitmap(
            gl,
            0,
            0,
            gl.width,
            gl.height,
            Matrix().apply { setScale(1f, -1f) },
            false
        )
        if (upright !== gl) gl.recycle()
        return upright
    }

    override fun finishProcessingAndBlend(
        outputFrame: GlTextureInfo,
        presentationTimeUs: Long,
        result: Unit
    ) = Unit

    override fun release() = Unit
}
