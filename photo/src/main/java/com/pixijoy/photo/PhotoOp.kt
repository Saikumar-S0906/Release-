package com.pixijoy.photo

import kotlin.math.roundToInt

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

fun List<PhotoOp>.encodeGraph(): String =
    joinToString("\n") { op ->
        when (op) {
            is PhotoOp.Crop -> "C,${op.left},${op.top},${op.right},${op.bottom}"
            is PhotoOp.Rotate -> "R,${op.degrees}"
            is PhotoOp.FlipH -> "F"
            is PhotoOp.Grade -> "G,${op.exposure},${op.contrast}"
        }
    }

fun String.decodeGraph(): List<PhotoOp> {
    if (isBlank()) return emptyList()
    return lineSequence().mapNotNull { line ->
        val parts = line.trim().split(',')
        try {
            when (parts.firstOrNull()) {
                "C" -> if (parts.size == 5) {
                    PhotoOp.Crop(
                        parts[1].toFloat(),
                        parts[2].toFloat(),
                        parts[3].toFloat(),
                        parts[4].toFloat()
                    )
                } else null
                "R" -> if (parts.size == 2) PhotoOp.Rotate(parts[1].toInt()) else null
                "F" -> PhotoOp.FlipH
                "G" -> if (parts.size == 3) {
                    PhotoOp.Grade(parts[1].toFloat(), parts[2].toFloat())
                } else null
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }.toList()
}
