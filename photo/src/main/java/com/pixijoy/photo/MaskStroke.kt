package com.pixijoy.photo

import kotlin.math.hypot
import kotlin.math.roundToInt

enum class MaskFill { None, Subject, Background }

data class MaskStroke(
    val x: Float,
    val y: Float,
    val r: Float,
    val reveal: Boolean
)

fun List<MaskStroke>.pack(): String =
    joinToString(";") { "${it.x},${it.y},${it.r},${if (it.reveal) 1 else 0}" }

fun String.unpackMask(): List<MaskStroke> {
    if (isBlank()) return emptyList()
    return split(';').mapNotNull { token ->
        val p = token.split(',')
        if (p.size != 4) return@mapNotNull null
        runCatching {
            MaskStroke(p[0].toFloat(), p[1].toFloat(), p[2].toFloat(), p[3] == "1")
        }.getOrNull()
    }
}

fun invertMask(src: ByteArray): ByteArray =
    ByteArray(src.size) { i -> (255 - (src[i].toInt() and 0xff)).toByte() }

fun maskBytesFromArgb(px: IntArray, invert: Boolean = false): ByteArray =
    ByteArray(px.size) { i ->
        val a = (px[i] ushr 24) and 0xff
        (if (invert) 255 - a else a).toByte()
    }

fun rasterizeMask(
    width: Int,
    height: Int,
    strokes: List<MaskStroke>,
    fill: ByteArray? = null
): ByteArray {
    val n = width * height
    val mask = if (fill != null && fill.size == n) fill.copyOf()
    else ByteArray(n) { 255.toByte() }
    if (strokes.isEmpty()) return mask
    val minSide = minOf(width, height)
    for (stroke in strokes) {
        val cx = (stroke.x * width).roundToInt()
        val cy = (stroke.y * height).roundToInt()
        val radius = (stroke.r * minSide).roundToInt().coerceAtLeast(2)
        val value = if (stroke.reveal) 255 else 0
        for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(height - 1)) {
            for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(width - 1)) {
                val d = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
                if (d > radius) continue
                val t = (1f - d / radius).coerceIn(0f, 1f)
                val i = y * width + x
                val cur = mask[i].toInt() and 0xff
                mask[i] = (cur + ((value - cur) * t).roundToInt()).coerceIn(0, 255).toByte()
            }
        }
    }
    return mask
}
