package com.pixijoy.photo

enum class BlendMode { Normal, Multiply, Screen, Overlay }

data class LayeredOp(
    val id: String,
    val name: String,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val blend: BlendMode = BlendMode.Normal,
    val op: PhotoOp,
    val mask: List<MaskStroke> = emptyList()
)

fun PhotoOp.layerName(): String = when (this) {
    is PhotoOp.Crop -> "Crop"
    is PhotoOp.Rotate -> "Rotate"
    is PhotoOp.FlipH -> "Flip"
    is PhotoOp.Grade -> "Grade"
    is PhotoOp.Hsl -> "HSL"
    is PhotoOp.Restore -> "Restore"
    is PhotoOp.Expand -> "Expand"
    is PhotoOp.Cutout -> "Cutout"
    is PhotoOp.Heal -> "Heal"
    is PhotoOp.Erase -> "Erase"
    is PhotoOp.TextLayer -> "Type"
    is PhotoOp.Sticker -> "Sticker"
    is PhotoOp.Collage -> "Collage"
    is PhotoOp.Curves -> "Curves"
    is PhotoOp.Blur -> "Blur"
    is PhotoOp.Sharpen -> "Sharpen"
    is PhotoOp.Vignette -> "Vignette"
    is PhotoOp.Subject -> "Subject"
}

fun PhotoOp.isGeometry(): Boolean =
    this is PhotoOp.Crop || this is PhotoOp.Rotate || this is PhotoOp.FlipH ||
        this is PhotoOp.Expand || this is PhotoOp.Collage

fun List<PhotoOp>.toLayers(): List<LayeredOp> =
    mapIndexed { i, op ->
        LayeredOp(id = "L$i-${op.layerName()}", name = op.layerName(), op = op)
    }

fun List<LayeredOp>.toOps(): List<PhotoOp> = filter { it.visible }.map { it.op }

fun List<LayeredOp>.encodeLayers(): String =
    joinToString("\n") { layer ->
        buildString {
            append("LY,${layer.id},${layer.name.encodePayload()},${if (layer.visible) 1 else 0},${layer.opacity},${layer.blend.name}")
            append('\n')
            append(listOf(layer.op).encodeGraph())
            if (layer.mask.isNotEmpty()) {
                append('\n')
                append("MK,${layer.mask.pack()}")
            }
        }
    }

fun String.decodeLayers(): List<LayeredOp> {
    if (isBlank()) return emptyList()
    if (!contains("LY,")) return decodeGraph().toLayers()
    val lines = lines()
    val out = ArrayList<LayeredOp>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.startsWith("LY,")) {
            val p = line.split(',')
            val opLine = lines.getOrNull(i + 1).orEmpty()
            val op = opLine.decodeGraph().firstOrNull()
            var mask = emptyList<MaskStroke>()
            var consumed = 2
            val maybeMask = lines.getOrNull(i + 2).orEmpty()
            if (maybeMask.startsWith("MK,")) {
                mask = maybeMask.substringAfter(',').unpackMask()
                consumed = 3
            }
            if (op != null && p.size >= 6) {
                out += LayeredOp(
                    id = p[1],
                    name = p[2].decodePayload(),
                    visible = p[3] == "1",
                    opacity = p[4].toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f,
                    blend = runCatching { BlendMode.valueOf(p[5]) }.getOrDefault(BlendMode.Normal),
                    op = op,
                    mask = mask
                )
            }
            i += consumed
        } else {
            i++
        }
    }
    return out
}
