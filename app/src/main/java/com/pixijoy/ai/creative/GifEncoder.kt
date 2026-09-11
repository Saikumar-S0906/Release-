package com.pixijoy.ai.creative

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GifEncoder @Inject constructor() {

    fun encode(frames: List<Bitmap>, frameDelayMs: Int = 500): ByteArray {
        require(frames.isNotEmpty()) { "At least one frame required" }
        val width = frames.first().width
        val height = frames.first().height
        val output = ByteArrayOutputStream()
        writeHeader(output)
        writeLogicalScreenDescriptor(output, width, height)
        writeNetscapeLoopExtension(output)
        frames.forEach { frame ->
            val scaled = if (frame.width != width || frame.height != height) {
                Bitmap.createScaledBitmap(frame, width, height, true)
            } else frame
            writeGraphicControlExtension(output, frameDelayMs)
            writeImageDescriptor(output, width, height)
            writeLzwImageData(output, scaled)
            if (scaled != frame) scaled.recycle()
        }
        output.write(0x3B)
        return output.toByteArray()
    }

    private fun writeHeader(out: OutputStream) {
        out.write("GIF89a".toByteArray())
    }

    private fun writeLogicalScreenDescriptor(out: OutputStream, width: Int, height: Int) {
        writeShort(out, width)
        writeShort(out, height)
        out.write(0xF0)
        out.write(0)
        out.write(0)
    }

    private fun writeNetscapeLoopExtension(out: OutputStream) {
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        out.write("NETSCAPE2.0".toByteArray())
        out.write(3)
        out.write(1)
        writeShort(out, 0)
        out.write(0)
    }

    private fun writeGraphicControlExtension(out: OutputStream, delayMs: Int) {
        out.write(0x21)
        out.write(0xF9)
        out.write(4)
        out.write(0)
        writeShort(out, delayMs / 10)
        out.write(0)
        out.write(0)
    }

    private fun writeImageDescriptor(out: OutputStream, width: Int, height: Int) {
        out.write(0x2C)
        writeShort(out, 0)
        writeShort(out, 0)
        writeShort(out, width)
        writeShort(out, height)
        out.write(0x00)
    }

    private fun writeLzwImageData(out: OutputStream, bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val colorTable = buildColorTable(pixels)
        out.write(0x07)
        colorTable.forEach { color ->
            out.write(color shr 16 and 0xFF)
            out.write(color shr 8 and 0xFF)
            out.write(color and 0xFF)
        }
        val indexed = pixels.map { pixel ->
            val r = pixel shr 16 and 0xFF
            val g = pixel shr 8 and 0xFF
            val b = pixel and 0xFF
            colorTable.indexOfFirst {
                val cr = it shr 16 and 0xFF
                val cg = it shr 8 and 0xFF
                val cb = it and 0xFF
                kotlin.math.abs(cr - r) + kotlin.math.abs(cg - g) + kotlin.math.abs(cb - b) < 48
            }.coerceAtLeast(0).toByte()
        }.toByteArray()
        out.write(0x08)
        var pos = 0
        while (pos < indexed.size) {
            val chunkSize = minOf(255, indexed.size - pos)
            out.write(chunkSize)
            out.write(indexed, pos, chunkSize)
            pos += chunkSize
        }
        out.write(0)
    }

    private fun buildColorTable(pixels: IntArray): List<Int> {
        val unique = linkedSetOf<Int>()
        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 32 * 32
            val g = (pixel shr 8 and 0xFF) / 32 * 32
            val b = (pixel and 0xFF) / 32 * 32
            unique.add((r shl 16) or (g shl 8) or b)
            if (unique.size >= 128) break
        }
        while (unique.size < 128) unique.add(0)
        return unique.take(128)
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write(value shr 8 and 0xFF)
    }
}
