package com.pixijoy.movie

import android.graphics.Bitmap
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Latest relative-depth map for soft Z. Empty until a bound depth engine
 * publishes one. A miss keeps the flat Z-order insert.
 */
internal object DepthFrames {
    const val BUCKET_US = 200_000L
    const val SLACK_US = 500_000L

    private val latest = AtomicReference<Pair<Long, FloatArray>?>(null)
    private val busy = AtomicBoolean(false)
    private val exec = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "pixijoy-depth").apply { isDaemon = true }
    }

    fun publish(timeUs: Long, depth: FloatArray) {
        latest.set(timeUs to depth)
    }

    /** Map close enough to [timeUs] to occlude this frame, or null. */
    fun at(timeUs: Long): FloatArray? {
        val snap = latest.get() ?: return null
        val delta = if (timeUs >= snap.first) timeUs - snap.first else snap.first - timeUs
        if (delta > SLACK_US) return null
        return snap.second
    }

    /**
     * Takes ownership of [frame] and recycles it. Skips inference when this
     * time bucket is already published or another frame is in flight.
     */
    fun request(timeUs: Long, frame: Bitmap) {
        val snap = latest.get()
        val sameBucket = snap != null && timeUs / BUCKET_US == snap.first / BUCKET_US
        if (sameBucket || !busy.compareAndSet(false, true)) {
            if (!frame.isRecycled) frame.recycle()
            return
        }
        exec.execute {
            try {
                val depth = PackEngineHooks.tryDepth(frame)
                val edge = PackModelContract.DEPTH_EDGE
                if (depth != null && depth.size == edge * edge) publish(timeUs, depth)
            } finally {
                if (!frame.isRecycled) frame.recycle()
                busy.set(false)
            }
        }
    }

    fun resetForTests() {
        latest.set(null)
        busy.set(false)
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
        val frame = runCatching { image.copyToBitmap() }.getOrNull()
        if (frame != null) DepthFrames.request(presentationTimeUs, frame)
        return Futures.immediateFuture(Unit)
    }

    override fun finishProcessingAndBlend(
        outputFrame: GlTextureInfo,
        presentationTimeUs: Long,
        result: Unit
    ) = Unit

    override fun release() = Unit
}
