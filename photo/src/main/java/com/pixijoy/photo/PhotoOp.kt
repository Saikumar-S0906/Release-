package com.pixijoy.photo

import kotlin.math.roundToInt

data class Stamp(
    val x: Float,
    val y: Float,
    val r: Float,
    val sx: Float? = null,
    val sy: Float? = null
) {
    val hasSource: Boolean get() = sx != null && sy != null
}

enum class ExportFormat(val mime: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    HEIC("image/heic", "heic"),
    AVIF("image/avif", "avif")
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
    data class SelectiveHsl(
        val range: SelectiveHslRange,
        val hue: Float,
        val sat: Float,
        val light: Float
    ) : PhotoOp
    data object Restore : PhotoOp
    data class Expand(val fraction: Float) : PhotoOp
    data object Cutout : PhotoOp
    data class Heal(val stamps: List<Stamp>) : PhotoOp
    /**
     * [neural] requests the sideloaded inpaint engine. False is nearby-pixel
     * Telea and is never a neural result. Legacy graphs omit the flag.
     */
    data class Fill(val stamps: List<Stamp>, val neural: Boolean = false) : PhotoOp
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
    data class Lut(val assetKey: String) : PhotoOp
    data class Curves(val y: FloatArray) : PhotoOp {
        override fun equals(other: Any?): Boolean =
            other is Curves && y.contentEquals(other.y)
        override fun hashCode(): Int = y.contentHashCode()
    }
    data class Blur(val radius: Float) : PhotoOp
    data class Sharpen(val amount: Float) : PhotoOp
    data class Vignette(val amount: Float) : PhotoOp
    data object Subject : PhotoOp
    /** C4: replace sky or flat background with [color] (ARGB). Graph `H`. */
    data class SkyReplace(val target: SkyReplaceTarget, val color: Int) : PhotoOp
    /** C5: keystone perspective. Graph `D`. */
    data class Perspective(val horizontal: Float, val vertical: Float) : PhotoOp
    /** C5: local liquify push. Graph `M`. */
    data class Liquify(
        val x: Float,
        val y: Float,
        val radius: Float,
        val amount: Float
    ) : PhotoOp
    /** C8 lightweight portrait soften. Graph `O`. Model-heavy → R5. */
    data class Soften(val amount: Float) : PhotoOp
    /** E2 on-demand super-resolution. Graph `SR`. No-op until the pack engine is bound. */
    data class SuperRes(val scale: Int) : PhotoOp
    /** E3 on-demand denoise. Graph `N`. No-op until the pack engine is bound. */
    data class Denoise(val amount: Float) : PhotoOp
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
    joinToString(";") { s ->
        if (s.hasSource) "${s.x},${s.y},${s.r},${s.sx},${s.sy}" else "${s.x},${s.y},${s.r}"
    }

/** `n` marks a neural-inpaint request. Stamp tokens stay numeric, so this cannot collide. */
private fun encodeFill(op: PhotoOp.Fill): String {
    val packed = op.stamps.pack()
    return when {
        op.neural && packed.isEmpty() -> "I,n"
        op.neural -> "I,$packed;n"
        else -> "I,$packed"
    }
}

private fun decodeFill(body: String): PhotoOp.Fill {
    val neural = body.split(';').any { it == "n" }
    return PhotoOp.Fill(body.unpackStamps(), neural = neural)
}

fun String.unpackStamps(): List<Stamp> {
    if (isBlank()) return emptyList()
    return split(';').mapNotNull { token ->
        val p = token.split(',')
        runCatching {
            when (p.size) {
                3 -> Stamp(p[0].toFloat(), p[1].toFloat(), p[2].toFloat())
                5 -> Stamp(
                    x = p[0].toFloat(),
                    y = p[1].toFloat(),
                    r = p[2].toFloat(),
                    sx = p[3].toFloat(),
                    sy = p[4].toFloat()
                )
                else -> null
            }
        }.getOrNull()
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
            is PhotoOp.SelectiveHsl -> "A,${op.range.name},${op.hue},${op.sat},${op.light}"
            is PhotoOp.Restore -> "S"
            is PhotoOp.Expand -> "E,${op.fraction}"
            is PhotoOp.Cutout -> "K"
            is PhotoOp.Heal -> "B,${op.stamps.pack()}"
            is PhotoOp.Fill -> encodeFill(op)
            is PhotoOp.Erase -> "Z,${op.stamps.pack()}"
            is PhotoOp.TextLayer ->
                "T,${op.x},${op.y},${op.size},${op.color},${op.text.encodePayload()}"
            is PhotoOp.Sticker -> "P,${op.glyph.encodePayload()},${op.x},${op.y},${op.scale}"
            is PhotoOp.Collage -> "L,${op.columns}"
            is PhotoOp.Lut -> "U,${op.assetKey}"
            is PhotoOp.Curves -> "Q,${op.y.joinToString(",")}"
            is PhotoOp.Blur -> "W,${op.radius}"
            is PhotoOp.Sharpen -> "X,${op.amount}"
            is PhotoOp.Vignette -> "V,${op.amount}"
            is PhotoOp.Subject -> "J"
            is PhotoOp.SkyReplace -> "H,${op.target.name},${op.color}"
            is PhotoOp.Perspective -> "D,${op.horizontal},${op.vertical}"
            is PhotoOp.Liquify -> "M,${op.x},${op.y},${op.radius},${op.amount}"
            is PhotoOp.Soften -> "O,${op.amount}"
            is PhotoOp.SuperRes -> "SR,${if (op.scale >= 4) 4 else 2}"
            is PhotoOp.Denoise -> "N,${op.amount}"
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
                "A" -> if (parts.size == 5) {
                    val range = runCatching { SelectiveHslRange.valueOf(parts[1]) }.getOrNull()
                    if (range != null) {
                        PhotoOp.SelectiveHsl(
                            range = range,
                            hue = parts[2].toFloat(),
                            sat = parts[3].toFloat(),
                            light = parts[4].toFloat()
                        )
                    } else null
                } else null
                "S" -> PhotoOp.Restore
                "E" -> if (parts.size == 2) PhotoOp.Expand(parts[1].toFloat()) else null
                "K" -> PhotoOp.Cutout
                "B" -> PhotoOp.Heal(line.substringAfter(',').unpackStamps())
                "I" -> decodeFill(line.substringAfter(','))
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
                "U" -> if (parts.size >= 2) PhotoOp.Lut(parts[1].ifBlank { BundledLooks.ORIGINAL }) else null
                "Q" -> if (parts.size == 6) {
                    PhotoOp.Curves(floatArrayOf(
                        parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat(),
                        parts[4].toFloat(), parts[5].toFloat()
                    ))
                } else null
                "W" -> if (parts.size == 2) PhotoOp.Blur(parts[1].toFloat().coerceIn(0f, 16f)) else null
                "X" -> if (parts.size == 2) PhotoOp.Sharpen(parts[1].toFloat()) else null
                "V" -> if (parts.size == 2) PhotoOp.Vignette(parts[1].toFloat()) else null
                "J" -> PhotoOp.Subject
                "H" -> if (parts.size == 3) {
                    val target = runCatching { SkyReplaceTarget.valueOf(parts[1]) }.getOrNull()
                    val color = parts[2].toIntOrNull()
                    if (target != null && color != null) PhotoOp.SkyReplace(target, color) else null
                } else null
                "D" -> if (parts.size == 3) {
                    PhotoOp.Perspective(parts[1].toFloat(), parts[2].toFloat())
                } else null
                "M" -> if (parts.size == 5) {
                    PhotoOp.Liquify(
                        x = parts[1].toFloat(),
                        y = parts[2].toFloat(),
                        radius = parts[3].toFloat(),
                        amount = parts[4].toFloat()
                    )
                } else null
                "O" -> if (parts.size == 2) PhotoOp.Soften(parts[1].toFloat()) else null
                "SR" -> if (parts.size == 2) {
                    PhotoOp.SuperRes(parts[1].toInt().let { if (it >= 4) 4 else 2 })
                } else null
                "N" -> if (parts.size == 2) PhotoOp.Denoise(parts[1].toFloat()) else null
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
