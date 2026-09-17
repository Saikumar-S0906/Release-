package com.pixijoy.movie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.pixijoy.core.common.AppResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max

internal class GifEncoder {
    fun encode(
        source: Bitmap,
        output: File,
        durationMs: Long,
        talking: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> {
        val width = 480
        val height = 270
        val fps = 10
        val frameCount = ((durationMs.coerceAtLeast(800) / 1000f) * fps).toInt().coerceIn(8, 40)
        val delayCs = (100 / fps).coerceAtLeast(6)
        val frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frame)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val cover = max(width / source.width.toFloat(), height / source.height.toFloat())
        return try {
            FileOutputStream(output).use { out ->
                writeHeader(out, width, height)
                for (i in 0 until frameCount) {
                    val t = i / (frameCount - 1).toFloat().coerceAtLeast(1f)
                    drawKenBurns(canvas, source, cover, width, height, t, talking, paint)
                    writeFrame(out, frame, width, height, delayCs)
                    onProgress(i / frameCount.toFloat())
                }
                out.write(0x3B)
            }
            AppResult.Ok(Unit)
        } catch (t: Throwable) {
            AppResult.Err("Could not encode GIF.", t)
        } finally {
            if (!frame.isRecycled) frame.recycle()
        }
    }
}

private fun drawKenBurns(
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
    val scale = cover * MovieMotion.scale(t, talking)
    val pan = MovieMotion.pan(t, talking, width.toFloat())
    val matrix = Matrix().apply {
        postTranslate(-source.width / 2f, -source.height / 2f)
        postScale(scale, scale)
        postTranslate(width / 2f + pan, height / 2f)
    }
    canvas.drawBitmap(source, matrix, paint)
}

private fun writeHeader(out: OutputStream, width: Int, height: Int) {
    out.write("GIF89a".toByteArray(Charsets.US_ASCII))
    writeShort(out, width)
    writeShort(out, height)
    out.write(0xF7) // global table, 256 colors
    out.write(0)
    out.write(0)
    for (i in 0 until 256) {
        val r = (i / 36) % 6 * 51
        val g = (i / 6) % 6 * 51
        val b = i % 6 * 51
        if (i < 216) {
            out.write(r)
            out.write(g)
            out.write(b)
        } else {
            out.write(0)
            out.write(0)
            out.write(0)
        }
    }
    out.write(0x21)
    out.write(0xFF)
    out.write(11)
    out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
    out.write(3)
    out.write(1)
    writeShort(out, 0)
    out.write(0)
}

private fun writeFrame(out: OutputStream, bitmap: Bitmap, width: Int, height: Int, delayCs: Int) {
    out.write(0x21)
    out.write(0xF9)
    out.write(4)
    out.write(0x04)
    writeShort(out, delayCs)
    out.write(0)
    out.write(0)
    out.write(0x2C)
    writeShort(out, 0)
    writeShort(out, 0)
    writeShort(out, width)
    writeShort(out, height)
    out.write(0)
    val indexed = ByteArray(width * height)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = ((c shr 16) and 0xff) / 51
        val g = ((c shr 8) and 0xff) / 51
        val b = (c and 0xff) / 51
        indexed[i] = (r * 36 + g * 6 + b).coerceIn(0, 215).toByte()
    }
    out.write(8)
    lzw(out, indexed)
}

private fun writeShort(out: OutputStream, value: Int) {
    out.write(value and 0xff)
    out.write((value shr 8) and 0xff)
}

private fun lzw(out: OutputStream, indexed: ByteArray) {
    val clear = 256
    val end = 257
    var codeSize = 9
    var nextCode = 258
    val table = HashMap<String, Int>(4096)
    val packet = ByteArray(255)
    var packetLen = 0
    var acc = 0
    var accBits = 0

    fun emit(code: Int) {
        acc = acc or (code shl accBits)
        accBits += codeSize
        while (accBits >= 8) {
            packet[packetLen++] = (acc and 0xff).toByte()
            acc = acc shr 8
            accBits -= 8
            if (packetLen == 255) {
                out.write(255)
                out.write(packet, 0, 255)
                packetLen = 0
            }
        }
    }

    fun flushBits() {
        if (accBits > 0) {
            packet[packetLen++] = (acc and 0xff).toByte()
            acc = 0
            accBits = 0
        }
        if (packetLen > 0) {
            out.write(packetLen)
            out.write(packet, 0, packetLen)
            packetLen = 0
        }
        out.write(0)
    }

    emit(clear)
    var w = indexed[0].toInt() and 0xff
    var i = 1
    while (i < indexed.size) {
        val k = indexed[i].toInt() and 0xff
        val key = "$w,$k"
        val existing = table[key]
        if (existing != null) {
            w = existing
        } else {
            emit(w)
            if (nextCode < 4096) {
                table[key] = nextCode++
                if (nextCode == (1 shl codeSize) && codeSize < 12) codeSize++
            } else {
                emit(clear)
                table.clear()
                codeSize = 9
                nextCode = 258
            }
            w = k
        }
        i++
    }
    emit(w)
    emit(end)
    flushBits()
}
