package com.pixijoy.photo

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.roundToInt

/**
 * Parsed 3D LUT (.cube). Size is typically 17/33/65.
 * Graph ops store a project-local file key; this type is the in-memory table.
 */
data class CubeLut(
    val title: String?,
    val size: Int,
    /** Row-major RGB triples, length = size^3 * 3 */
    val table: FloatArray
) {
    init {
        require(size >= 2) { "LUT size must be >= 2" }
        require(table.size == size * size * size * 3) {
            "Expected ${size * size * size * 3} floats, got ${table.size}"
        }
    }
}

sealed class LutParseResult {
    data class Ok(val lut: CubeLut) : LutParseResult()
    data class Err(val message: String) : LutParseResult()
}

object CubeLutParser {
    private val DOMAIN_MIN = Regex("""DOMAIN_MIN\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)""", RegexOption.IGNORE_CASE)
    private val DOMAIN_MAX = Regex("""DOMAIN_MAX\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)""", RegexOption.IGNORE_CASE)

    fun parseFile(file: java.io.File, maxSize: Int = 65): LutParseResult {
        if (!file.isFile) return LutParseResult.Err("LUT file missing")
        return file.inputStream().use { input ->
            when (file.extension.lowercase()) {
                "cube" -> parseCube(input, maxSize)
                "3dl" -> parse3dl(input, maxSize)
                else -> LutParseResult.Err("Unsupported LUT type .${file.extension}")
            }
        }
    }

    fun parseCube(input: InputStream, maxSize: Int = 65): LutParseResult {
        return try {
            parseCubeInternal(BufferedReader(InputStreamReader(input, Charsets.UTF_8)), maxSize)
        } catch (t: Throwable) {
            LutParseResult.Err(t.message ?: "LUT parse failed")
        }
    }

    fun parseCubeText(text: String, maxSize: Int = 65): LutParseResult =
        parseCubeInternal(BufferedReader(text.reader()), maxSize)

    /**
     * Minimal .3dl: optional `Mesh N N` / `LUT_3D_SIZE N`, then N³ RGB triples
     * (0–1 or 0–4095).
     */
    fun parse3dl(input: InputStream, maxSize: Int = 65): LutParseResult {
        return try {
            parse3dlInternal(BufferedReader(InputStreamReader(input, Charsets.UTF_8)), maxSize)
        } catch (t: Throwable) {
            LutParseResult.Err(t.message ?: "3DL parse failed")
        }
    }

    fun parse3dlText(text: String, maxSize: Int = 65): LutParseResult =
        parse3dlInternal(BufferedReader(text.reader()), maxSize)

    private fun parse3dlInternal(reader: BufferedReader, maxSize: Int): LutParseResult {
        var size: Int? = null
        val values = ArrayList<Float>()
        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val upper = line.uppercase()
                when {
                    upper.startsWith("3DMESH") || upper == "Mesh".uppercase() -> Unit
                    upper.startsWith("MESH") -> {
                        val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                        val n = parts.getOrNull(1)?.toIntOrNull()
                            ?: parts.getOrNull(2)?.toIntOrNull()
                        if (n != null) {
                            val cells = n + 1 // Mesh 16 16 often means 17 points
                            if (cells in 2..maxSize) size = cells
                        }
                    }
                    upper.startsWith("LUT_3D_SIZE") -> {
                        val n = line.substringAfter("LUT_3D_SIZE", missingDelimiterValue = "")
                            .trim().toIntOrNull()
                            ?: return LutParseResult.Err("Invalid LUT_3D_SIZE in 3dl")
                        if (n !in 2..maxSize) return LutParseResult.Err("3dl size out of range")
                        size = n
                    }
                    else -> {
                        val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                        if (parts.size == 1 && size == null) {
                            val n = parts[0].toIntOrNull()
                            if (n != null && n in 2..maxSize) {
                                size = n
                                continue
                            }
                        }
                        if (parts.size < 3) continue
                        val r = parts[0].toFloatOrNull() ?: continue
                        val g = parts[1].toFloatOrNull() ?: continue
                        val b = parts[2].toFloatOrNull() ?: continue
                        values.add(r)
                        values.add(g)
                        values.add(b)
                    }
                }
            }
        }
        if (values.isEmpty()) return LutParseResult.Err("Empty 3dl")
        val n = size ?: cubeRootSize(values.size / 3)
            ?: return LutParseResult.Err("Could not determine 3dl size")
        if (n !in 2..maxSize) return LutParseResult.Err("3dl size $n out of range")
        val expected = n * n * n * 3
        if (values.size < expected) {
            return LutParseResult.Err("Expected $expected floats, got ${values.size}")
        }
        val scale = if (values.take(expected).maxOrNull() ?: 0f > 1.5f) 1f / 4095f else 1f
        val table = FloatArray(expected) { i -> (values[i] * scale).coerceIn(0f, 1f) }
        return LutParseResult.Ok(CubeLut(title = null, size = n, table = table))
    }

    private fun cubeRootSize(count: Int): Int? {
        var n = 2
        while (n * n * n < count) n++
        return n.takeIf { it * it * it == count }
    }

    private fun parseCubeInternal(reader: BufferedReader, maxSize: Int): LutParseResult {
        var title: String? = null
        var size: Int? = null
        val values = ArrayList<Float>(17 * 17 * 17 * 3)
        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                when {
                    line.startsWith("TITLE", ignoreCase = true) -> {
                        title = line.substringAfter("TITLE", missingDelimiterValue = "")
                            .trim()
                            .trim('"')
                            .ifBlank { null }
                    }
                    line.startsWith("LUT_3D_SIZE", ignoreCase = true) -> {
                        val n = line.substringAfter("LUT_3D_SIZE", missingDelimiterValue = "")
                            .trim()
                            .toIntOrNull()
                            ?: return LutParseResult.Err("Invalid LUT_3D_SIZE")
                        if (n < 2 || n > maxSize) {
                            return LutParseResult.Err("LUT size $n out of range 2..$maxSize")
                        }
                        size = n
                    }
                    DOMAIN_MIN.containsMatchIn(line) || DOMAIN_MAX.containsMatchIn(line) -> Unit
                    else -> {
                        val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                        if (parts.size < 3) {
                            return LutParseResult.Err("Malformed LUT row: $line")
                        }
                        val r = parts[0].toFloatOrNull()
                        val g = parts[1].toFloatOrNull()
                        val b = parts[2].toFloatOrNull()
                        if (r == null || g == null || b == null) {
                            return LutParseResult.Err("Non-numeric LUT row: $line")
                        }
                        values.add(r.coerceIn(0f, 1f))
                        values.add(g.coerceIn(0f, 1f))
                        values.add(b.coerceIn(0f, 1f))
                    }
                }
            }
        }
        val n = size ?: return LutParseResult.Err("Missing LUT_3D_SIZE")
        val expected = n * n * n * 3
        if (values.size != expected) {
            return LutParseResult.Err("Expected $expected floats, got ${values.size}")
        }
        return LutParseResult.Ok(CubeLut(title, n, values.toFloatArray()))
    }

    /** Nearest-neighbor sample — preview helper until tetrahedral land. */
    fun sampleNearest(lut: CubeLut, r: Float, g: Float, b: Float): FloatArray {
        val n = lut.size
        val max = n - 1
        val ir = (r.coerceIn(0f, 1f) * max).roundToInt()
        val ig = (g.coerceIn(0f, 1f) * max).roundToInt()
        val ib = (b.coerceIn(0f, 1f) * max).roundToInt()
        val idx = ((ir * n + ig) * n + ib) * 3
        return floatArrayOf(lut.table[idx], lut.table[idx + 1], lut.table[idx + 2])
    }
}
