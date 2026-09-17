package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.pixijoy.core.common.AppResult
import java.io.File
import kotlin.math.max

internal class KenBurnsEncoder {
    fun encode(
        source: Bitmap,
        output: File,
        durationMs: Long,
        talking: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> {
        val width = 1280
        val height = 720
        val fps = 24
        val frameCount = ((durationMs.coerceAtLeast(1000) / 1000f) * fps).toInt().coerceAtLeast(24)
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(mime)
        val frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frame)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val cover = coverScale(source.width, source.height, width, height)
        return try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var track = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var frameIndex = 0
            while (!outputEos) {
                if (!inputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        if (frameIndex >= frameCount) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                frameIndex * 1_000_000L / fps,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEos = true
                        } else {
                            val t = frameIndex / (frameCount - 1).toFloat().coerceAtLeast(1f)
                            drawFrame(canvas, source, cover, width, height, t, talking, paint)
                            val image = codec.getInputImage(inIndex)
                            val pts = frameIndex * 1_000_000L / fps
                            if (image != null) {
                                fillYuv(image, frame)
                                codec.queueInputBuffer(inIndex, 0, image.width * image.height * 3 / 2, pts, 0)
                            } else {
                                val buffer = codec.getInputBuffer(inIndex)
                                    ?: return AppResult.Err("Encoder input missing.")
                                val nv12 = bitmapToNv12(frame)
                                buffer.clear()
                                buffer.put(nv12)
                                codec.queueInputBuffer(inIndex, 0, nv12.size, pts, 0)
                            }
                            onProgress(frameIndex / frameCount.toFloat())
                            frameIndex++
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outIndex >= 0 -> {
                        val encoded = codec.getOutputBuffer(outIndex)
                        if (encoded != null &&
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
            muxer.release()
            AppResult.Ok(Unit)
        } catch (t: Throwable) {
            AppResult.Err("Could not encode motion.", t)
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            if (!frame.isRecycled) frame.recycle()
        }
    }

    private fun coverScale(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Float =
        max(dstW / srcW.toFloat(), dstH / srcH.toFloat())

    private fun drawFrame(
        canvas: Canvas,
        source: Bitmap,
        cover: Float,
        width: Int,
        height: Int,
        t: Float,
        talking: Boolean,
        paint: Paint
    ) {
        canvas.drawColor(Color.BLACK)
        val pan = MovieMotion.pan(t, talking, width.toFloat())
        val scale = cover * MovieMotion.scale(t, talking)
        val matrix = Matrix().apply {
            postTranslate(-source.width / 2f, -source.height / 2f)
            postScale(scale, scale)
            postTranslate(width / 2f + pan, height / 2f)
        }
        canvas.drawBitmap(source, matrix, paint)
    }
}

private fun fillYuv(image: Image, bitmap: Bitmap) {
    val width = image.width
    val height = image.height
    val argb = IntArray(width * height)
    bitmap.getPixels(argb, 0, width, 0, 0, width, height)
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val yBuf = yPlane.buffer
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer
    val yStride = yPlane.rowStride
    val uStride = uPlane.rowStride
    val vStride = vPlane.rowStride
    val uPixel = uPlane.pixelStride
    val vPixel = vPlane.pixelStride
    for (row in 0 until height) {
        for (col in 0 until width) {
            val c = argb[row * width + col]
            val r = (c shr 16) and 0xff
            val g = (c shr 8) and 0xff
            val b = c and 0xff
            val y = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
            yBuf.put(row * yStride + col, y.toByte())
            if (row % 2 == 0 && col % 2 == 0) {
                val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val uvRow = row / 2
                val uvCol = col / 2
                uBuf.put(uvRow * uStride + uvCol * uPixel, u.toByte())
                vBuf.put(uvRow * vStride + uvCol * vPixel, v.toByte())
            }
        }
    }
}

private fun bitmapToNv12(bitmap: Bitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val argb = IntArray(width * height)
    bitmap.getPixels(argb, 0, width, 0, 0, width, height)
    val ySize = width * height
    val out = ByteArray(ySize * 3 / 2)
    var uv = ySize
    var i = 0
    for (row in 0 until height) {
        for (col in 0 until width) {
            val c = argb[i]
            val r = (c shr 16) and 0xff
            val g = (c shr 8) and 0xff
            val b = c and 0xff
            val y = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
            out[i] = y.toByte()
            if (row % 2 == 0 && col % 2 == 0) {
                val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                out[uv++] = u.toByte()
                out[uv++] = v.toByte()
            }
            i++
        }
    }
    return out
}
