package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import com.pixijoy.core.common.AppResult
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * E4 export path: sample source frames, insert FILM mid-frames, encode a stretched
 * silent video. Callers remux slowed audio and clear timeline slow-mo so Media3 does
 * not also apply [SpeedChangeEffect] / frame-hold.
 *
 * Inference budget is capped so a long clip cannot melt the device; more source time
 * means coarser key spacing, not unbounded FILM calls.
 */
internal object NeuralSlowMoMux {
    /** Soft cap on FILM mid-frame inferences per clip. */
    const val INFER_BUDGET = 32

    fun interpSteps(rate: Float): Int {
        val r = MovieSpeed.normalizeSlowMo(rate)
        if (r >= 0.99f) return 0
        return ((1f / r).roundToInt() - 1).coerceIn(1, 7)
    }

    fun keyFrameCount(sourceMs: Long, rate: Float, budget: Int = INFER_BUDGET): Int {
        val steps = interpSteps(rate)
        if (steps <= 0) return 0
        val maxPairs = (budget / steps).coerceAtLeast(2)
        val byDuration = ((sourceMs.coerceAtLeast(1) / 1000f) * 12f).roundToInt()
        return byDuration.coerceIn(3, maxPairs + 1)
    }

    /**
     * Builds a silent AVC file whose timeline duration matches slow-mo output.
     * Returns null when the pack declines or sampling fails — caller keeps heuristic.
     */
    fun synthesize(
        path: String,
        startMs: Long,
        endMs: Long,
        rate: Float,
        output: File,
        height: Int = 720,
        onProgress: (Float) -> Unit = {}
    ): AppResult<Unit> {
        if (!FrameInterp.mlAvailable()) {
            return AppResult.Err("Neural frame interp is not bound.")
        }
        val steps = interpSteps(rate)
        if (steps <= 0) return AppResult.Err("Rate does not need interpolation.")
        val span = (endMs - startMs).coerceAtLeast(1)
        val keys = keyFrameCount(span, rate)
        if (keys < 2) return AppResult.Err("Clip too short for neural slow-mo.")

        val sampled = sampleFrames(path, startMs, endMs, keys) ?: return AppResult.Err(
            "Could not sample frames for neural slow-mo."
        )
        if (sampled.size < 2) {
            sampled.forEach { if (!it.isRecycled) it.recycle() }
            return AppResult.Err("Need at least two source frames.")
        }

        val expanded = ArrayList<Bitmap>(sampled.size + (sampled.size - 1) * steps)
        try {
            val pairs = sampled.size - 1
            for (i in 0 until pairs) {
                expanded += sampled[i]
                onProgress((i.toFloat() / pairs) * 0.7f)
                val mids = FrameInterp.interpolatedFrames(sampled[i], sampled[i + 1], steps)
                if (mids == null || mids.size != steps) {
                    expanded.filter { it !in sampled }.forEach { if (!it.isRecycled) it.recycle() }
                    return AppResult.Err("Neural mid-frames unavailable.")
                }
                expanded.addAll(mids)
            }
            expanded += sampled.last()
            onProgress(0.72f)

            val outMs = MovieSpeed.outputDurationMs(span, rate)
            val fps = ((expanded.size * 1000.0) / outMs.coerceAtLeast(1)).toFloat()
                .coerceIn(8f, 30f)
            return encodeFrames(expanded, output, fps, height) { p ->
                onProgress(0.72f + p * 0.28f)
            }
        } finally {
            val keep = expanded.toHashSet()
            sampled.forEach { if (it !in keep && !it.isRecycled) it.recycle() }
            // Encoder recycles or owns nothing — recycle all expanded after encode returns.
            // encodeFrames must not recycle input; we recycle here after success/fail.
            expanded.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun sampleFrames(
        path: String,
        startMs: Long,
        endMs: Long,
        count: Int
    ): List<Bitmap>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return null
            val from = startMs.coerceIn(0, duration)
            val to = endMs.coerceIn((from + 1).coerceAtMost(duration), duration)
            val span = (to - from).coerceAtLeast(1)
            val n = count.coerceAtLeast(2)
            val out = ArrayList<Bitmap>(n)
            for (i in 0 until n) {
                val t = if (n == 1) from else from + (span * i / (n - 1).toLong())
                val frame = retriever.getFrameAtTime(
                    t * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                ) ?: continue
                out += frame
            }
            if (out.size < 2) {
                out.forEach { if (!it.isRecycled) it.recycle() }
                null
            } else {
                out
            }
        } catch (_: Throwable) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun encodeFrames(
        frames: List<Bitmap>,
        output: File,
        fps: Float,
        height: Int,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> {
        if (frames.isEmpty()) return AppResult.Err("No frames to encode.")
        val outHeight = MovieExportSize.normalize(height)
        val width = MovieExportSize.width(outHeight)
        val frameRate = fps.roundToInt().coerceIn(8, 30)
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, outHeight).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )
            setInteger(MediaFormat.KEY_BIT_RATE, MovieExportSize.bitrate(outHeight))
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        var codec: MediaCodec? = null
        val canvasFrame = Bitmap.createBitmap(width, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasFrame)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        var muxer: MediaMuxer? = null
        return try {
            codec = MediaCodec.createEncoderByType(mime)
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (_: Throwable) {
                format.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            codec.start()
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var track = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var frameIndex = 0
            var idleLoops = 0
            while (!outputEos) {
                if (!inputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        if (frameIndex >= frames.size) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                frameIndex * 1_000_000L / frameRate,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEos = true
                        } else {
                            drawCover(canvas, frames[frameIndex], width, outHeight, paint)
                            val pts = frameIndex * 1_000_000L / frameRate
                            val image = codec.getInputImage(inIndex)
                            if (image != null) {
                                fillYuv(image, canvasFrame)
                                val size = image.planes.sumOf { it.buffer.remaining() }
                                    .coerceAtLeast(width * outHeight * 3 / 2)
                                codec.queueInputBuffer(inIndex, 0, size, pts, 0)
                            } else {
                                val buffer = codec.getInputBuffer(inIndex)
                                    ?: return AppResult.Err("Encoder input missing.")
                                val nv12 = bitmapToNv12(canvasFrame)
                                buffer.clear()
                                buffer.put(nv12)
                                codec.queueInputBuffer(inIndex, 0, nv12.size, pts, 0)
                            }
                            onProgress((frameIndex + 1) / frames.size.toFloat())
                            frameIndex++
                        }
                        idleLoops = 0
                    }
                }
                when (val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        idleLoops++
                        if (idleLoops > 200) {
                            return AppResult.Err("Could not encode neural slow-mo.")
                        }
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                        idleLoops = 0
                    }
                    else -> if (outIndex >= 0) {
                        idleLoops = 0
                        val encoded = codec.getOutputBuffer(outIndex)
                        if (
                            encoded != null &&
                            bufferInfo.size > 0 &&
                            muxerStarted &&
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            encoded.position(bufferInfo.offset)
                            encoded.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(track, encoded, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEos = true
                        }
                    }
                }
            }
            if (muxerStarted) muxer.stop()
            AppResult.Ok(Unit)
        } catch (t: Throwable) {
            AppResult.Err("Could not encode neural slow-mo.", t)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { muxer?.release() }
            if (!canvasFrame.isRecycled) canvasFrame.recycle()
        }
    }

    private fun drawCover(
        canvas: Canvas,
        source: Bitmap,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        canvas.drawColor(Color.BLACK)
        val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
        val matrix = Matrix().apply {
            postTranslate(-source.width / 2f, -source.height / 2f)
            postScale(scale, scale)
            postTranslate(width / 2f, height / 2f)
        }
        canvas.drawBitmap(source, matrix, paint)
    }
}
