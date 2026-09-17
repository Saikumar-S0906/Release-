package com.pixijoy.photo

import kotlin.math.roundToInt

data class Stamp(val x: Float, val y: Float, val r: Float)

enum class ExportFormat(val mime: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp")
}

sealed interface PhotoOp {
    data class Crop(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) : PhotoOp

    data class Rotate(val degrees: Int) : PhotoOp
    data object FlipH : PhotoOp
    data class Grade(val exposure: Float, val contrast: Float) : PhotoOp
    data class Hsl(val hue: Float, val saturation: Float) : PhotoOp
    data object Restore : PhotoOp
    data class Expand(val fraction: Float) : PhotoOp
    data object Cutout : PhotoOp
    data class Heal(val stamps: List<Stamp>) : PhotoOp
    data class Erase(val stamps: List<Stamp>) : PhotoOp
    data class TextLayer(
        val text: String,
        val x: Float,
        val y: Float,
        val size: Float,
        val color: Int
    ) : PhotoOp
    data class Sticker(
        val glyph: String,
        val x: Float,
        val y: Float,
        val scale: Float
    ) : PhotoOp
    data class Collage(val columns: Int) : PhotoOp
}

data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

fun PhotoOp.Crop.toPixels(width: Int, height: Int): PixelRect {
    val l = (left * width).roundToInt().coerceIn(0, (width - 1).coerceAtLeast(0))
    val t = (top * height).roundToInt().coerceIn(0, (height - 1).coerceAtLeast(0))
    val r = (right * width).roundToInt().coerceIn(l + 1, width.coerceAtLeast(l + 1))
    val b = (bottom * height).roundToInt().coerceIn(t + 1, height.coerceAtLeast(t + 1))
    return PixelRect(l, t, r, b)
}

fun List<Stamp>.pack(): String =
    joinToString(";") { "${it.x},${it.y},${it.r}" }

fun String.unpackStamps(): List<Stamp> {
    if (isBlank()) return emptyList()
    return split(';').mapNotNull { token ->
        val p = token.split(',')
        if (p.size != 3) return@mapNotNull null
        runCatching { Stamp(p[0].toFloat(), p[1].toFloat(), p[2].toFloat()) }.getOrNull()
    }
}

fun List<PhotoOp>.encodeGraph(): String =
    joinToString("\n") { op ->
        when (op) {
            is PhotoOp.Crop -> "C,${op.left},${op.top},${op.right},${op.bottom}"
            is PhotoOp.Rotate -> "R,${op.degrees}"
            is PhotoOp.FlipH -> "F"
            is PhotoOp.Grade -> "G,${op.exposure},${op.contrast}"
            is PhotoOp.Hsl -> "Y,${op.hue},${op.saturation}"
            is PhotoOp.Restore -> "S"
            is PhotoOp.Expand -> "E,${op.fraction}"
            is PhotoOp.Cutout -> "K"
            is PhotoOp.Heal -> "B,${op.stamps.pack()}"
            is PhotoOp.Erase -> "Z,${op.stamps.pack()}"
            is PhotoOp.TextLayer ->
                "T,${op.x},${op.y},${op.size},${op.color},${op.text.encodePayload()}"
            is PhotoOp.Sticker -> "P,${op.glyph.encodePayload()},${op.x},${op.y},${op.scale}"
            is PhotoOp.Collage -> "L,${op.columns}"
        }
    }

fun String.decodeGraph(): List<PhotoOp> {
    if (isBlank()) return emptyList()
    return lineSequence().mapNotNull { line ->
        val parts = line.trim().split(',')
        try {
            when (parts.firstOrNull()) {
                "C" -> if (parts.size == 5) {
                    PhotoOp.Crop(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat(), parts[4].toFloat())
                } else null
                "R" -> if (parts.size == 2) PhotoOp.Rotate(parts[1].toInt()) else null
                "F" -> PhotoOp.FlipH
                "G" -> if (parts.size == 3) PhotoOp.Grade(parts[1].toFloat(), parts[2].toFloat()) else null
                "Y" -> if (parts.size == 3) PhotoOp.Hsl(parts[1].toFloat(), parts[2].toFloat()) else null
                "S" -> PhotoOp.Restore
                "E" -> if (parts.size == 2) PhotoOp.Expand(parts[1].toFloat()) else null
                "K" -> PhotoOp.Cutout
                "B" -> PhotoOp.Heal(line.substringAfter(',').unpackStamps())
                "Z" -> PhotoOp.Erase(line.substringAfter(',').unpackStamps())
                "T" -> if (parts.size >= 6) {
                    PhotoOp.TextLayer(
                        text = parts.drop(5).joinToString(",").decodePayload(),
                        x = parts[1].toFloat(),
                        y = parts[2].toFloat(),
                        size = parts[3].toFloat(),
                        color = parts[4].toInt()
                    )
                } else null
                "P" -> if (parts.size == 5) {
                    PhotoOp.Sticker(
                        glyph = parts[1].decodePayload(),
                        x = parts[2].toFloat(),
                        y = parts[3].toFloat(),
                        scale = parts[4].toFloat()
                    )
                } else null
                "L" -> if (parts.size == 2) PhotoOp.Collage(parts[1].toInt().coerceIn(1, 4)) else null
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }.toList()
}

internal fun String.encodePayload(): String = buildString {
    for (ch in this@encodePayload) {
        when (ch) {
            '%' -> append("%25")
            ',' -> append("%2C")
            '\n' -> append("%0A")
            else -> append(ch)
        }
    }
}

internal fun String.decodePayload(): String = buildString {
    var i = 0
    val src = this@decodePayload
    while (i < src.length) {
        if (src[i] == '%' && i + 2 < src.length) {
            when (src.substring(i, i + 3)) {
                "%25" -> { append('%'); i += 3 }
                "%2C" -> { append(','); i += 3 }
                "%0A" -> { append('\n'); i += 3 }
                else -> { append(src[i]); i++ }
            }
        } else {
            append(src[i])
            i++
        }
    }
}
