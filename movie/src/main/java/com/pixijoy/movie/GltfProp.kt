package com.pixijoy.movie

import java.io.File
import java.util.Base64
import kotlin.math.max

/**
 * Classical glTF 2.0 / GLB prop load.
 *
 * Reads uncompressed triangle meshes (embedded buffer or GLB BIN chunk).
 * Node translation, rotation, and scale are applied, then the mesh is fit
 * to a unit box so the existing place and scale controls match the cube plate.
 * Dense meshes are thinned for the CPU raster. This is not Filament and not neural.
 *
 * Project files live in `{project}/props/`.
 */
const val CG_PROP_DIR = "props"

const val GLTF_MAX_BYTES = 8 * 1024 * 1024
const val GLTF_MAX_SOURCE_TRIS = 80_000
const val GLTF_MAX_DRAW_TRIS = 1_600
private const val GLTF_MAX_VERTICES = 80_000

class GltfMesh(
    val positions: FloatArray,
    val indices: IntArray
) {
    val triangleCount: Int get() = indices.size / 3
}

sealed class GltfParseResult {
    class Ok(val mesh: GltfMesh) : GltfParseResult()
    class Err(val message: String) : GltfParseResult()
}

object GltfProp {
    fun parseFile(file: File): GltfParseResult {
        if (!file.isFile) return GltfParseResult.Err("Prop file is missing.")
        if (file.length() > GLTF_MAX_BYTES) return GltfParseResult.Err("Prop file is too large.")
        val bytes = try {
            file.readBytes()
        } catch (_: Exception) {
            return GltfParseResult.Err("Could not read prop.")
        }
        return parse(bytes)
    }

    fun parse(bytes: ByteArray): GltfParseResult {
        if (bytes.size > GLTF_MAX_BYTES) return GltfParseResult.Err("Prop file is too large.")
        if (bytes.size < 20) return GltfParseResult.Err("Not a glTF or GLB file.")
        return try {
            val magic = u32(bytes, 0)
            if (magic == 0x46546C67) parseGlb(bytes) else parseGltfJson(bytes, glbBin = null)
        } catch (ex: GltfReadException) {
            GltfParseResult.Err(ex.message ?: "glTF could not be read.")
        } catch (_: Exception) {
            GltfParseResult.Err("glTF could not be read.")
        }
    }

    fun isSafePropFileName(name: String): Boolean {
        if (name.length !in 1..120) return false
        if (name == "." || name == "..") return false
        if (name.contains('/') || name.contains('\\')) return false
        val lower = name.lowercase()
        if (!lower.endsWith(".gltf") && !lower.endsWith(".glb")) return false
        return name.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
    }
}

/** Keeps a parsed mesh for preview and export so both plates share one raster. */
object CgPropMeshes {
    private val lock = Any()
    private val cache = LinkedHashMap<String, GltfMesh>()

    fun get(projectDir: File?, propFile: String): GltfMesh? {
        if (projectDir == null || !GltfProp.isSafePropFileName(propFile)) return null
        val file = File(File(projectDir, CG_PROP_DIR), propFile)
        if (!file.isFile) return null
        val key = file.absolutePath + ":" + file.length() + ":" + file.lastModified()
        synchronized(lock) {
            cache[key]?.let { return it }
        }
        val mesh = (GltfProp.parseFile(file) as? GltfParseResult.Ok)?.mesh ?: return null
        synchronized(lock) {
            cache[key] = mesh
            while (cache.size > 6) {
                val eldest = cache.entries.firstOrNull()?.key ?: break
                cache.remove(eldest)
            }
        }
        return mesh
    }
}

internal fun thinPropTriangles(positions: FloatArray, indices: IntArray, maxTris: Int): IntArray {
    val triCount = indices.size / 3
    if (triCount <= maxTris) return indices
    val grid = 6
    val bins = Array(grid * grid * grid) { ArrayList<Int>() }
    val vertCount = positions.size / 3
    for (t in 0 until triCount) {
        val i0 = indices[t * 3]
        val i1 = indices[t * 3 + 1]
        val i2 = indices[t * 3 + 2]
        if (i0 !in 0 until vertCount || i1 !in 0 until vertCount || i2 !in 0 until vertCount) continue
        val cx = (positions[i0 * 3] + positions[i1 * 3] + positions[i2 * 3]) / 3f
        val cy = (positions[i0 * 3 + 1] + positions[i1 * 3 + 1] + positions[i2 * 3 + 1]) / 3f
        val cz = (positions[i0 * 3 + 2] + positions[i1 * 3 + 2] + positions[i2 * 3 + 2]) / 3f
        bins[propBin(cx, grid) + propBin(cy, grid) * grid + propBin(cz, grid) * grid * grid].add(t)
    }
    val out = ArrayList<Int>(maxTris * 3)
    var round = 0
    while (out.size / 3 < maxTris) {
        var added = false
        for (bin in bins) {
            if (round < bin.size && out.size / 3 < maxTris) {
                val t = bin[round]
                out.add(indices[t * 3])
                out.add(indices[t * 3 + 1])
                out.add(indices[t * 3 + 2])
                added = true
            }
        }
        if (!added) break
        round++
    }
    return out.toIntArray()
}

private fun propBin(v: Float, grid: Int): Int =
    ((v + 1f) * 0.5f * grid).toInt().coerceIn(0, grid - 1)

private class GltfReadException(message: String) : IllegalArgumentException(message)

private sealed class GltfJson {
    class Obj(val fields: Map<String, GltfJson>) : GltfJson()
    class Arr(val items: List<GltfJson>) : GltfJson()
    class Str(val text: String) : GltfJson()
    class Num(val value: Double) : GltfJson()
}

private fun GltfJson.asObj(): GltfJson.Obj? = this as? GltfJson.Obj
private fun GltfJson.asNum(): Double? = (this as? GltfJson.Num)?.value
private fun GltfJson.asStr(): String? = (this as? GltfJson.Str)?.text
private fun GltfJson.obj(key: String): GltfJson.Obj? =
    (this as? GltfJson.Obj)?.fields?.get(key) as? GltfJson.Obj
private fun GltfJson.arr(key: String): GltfJson.Arr? =
    (this as? GltfJson.Obj)?.fields?.get(key) as? GltfJson.Arr
private fun GltfJson.str(key: String): String? =
    ((this as? GltfJson.Obj)?.fields?.get(key) as? GltfJson.Str)?.text
private fun GltfJson.num(key: String): Double? =
    ((this as? GltfJson.Obj)?.fields?.get(key) as? GltfJson.Num)?.value

private class GltfJsonReader(private val s: String) {
    private var i = 0

    fun parseRoot(): GltfJson {
        skip()
        val value = readValue()
        skip()
        if (i < s.length) throw GltfReadException("glTF JSON could not be read.")
        return value
    }

    private fun skip() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun readValue(): GltfJson {
        skip()
        if (i >= s.length) throw GltfReadException("glTF JSON could not be read.")
        return when (val c = s[i]) {
            '{' -> readObj()
            '[' -> readArr()
            '"' -> GltfJson.Str(readString())
            't' -> {
                expect("true")
                GltfJson.Num(1.0)
            }
            'f' -> {
                expect("false")
                GltfJson.Num(0.0)
            }
            'n' -> {
                expect("null")
                GltfJson.Num(0.0)
            }
            else -> {
                if (c == '-' || c.isDigit()) readNum()
                else throw GltfReadException("glTF JSON could not be read.")
            }
        }
    }

    private fun expect(lit: String) {
        if (!s.startsWith(lit, i)) throw GltfReadException("glTF JSON could not be read.")
        i += lit.length
    }

    private fun readObj(): GltfJson.Obj {
        i++
        val map = LinkedHashMap<String, GltfJson>()
        skip()
        if (i < s.length && s[i] == '}') {
            i++
            return GltfJson.Obj(map)
        }
        while (i < s.length) {
            skip()
            if (i >= s.length || s[i] != '"') throw GltfReadException("glTF JSON could not be read.")
            val key = readString()
            skip()
            if (i >= s.length || s[i] != ':') throw GltfReadException("glTF JSON could not be read.")
            i++
            map[key] = readValue()
            skip()
            if (i >= s.length) throw GltfReadException("glTF JSON could not be read.")
            when (s[i]) {
                ',' -> i++
                '}' -> {
                    i++
                    return GltfJson.Obj(map)
                }
                else -> throw GltfReadException("glTF JSON could not be read.")
            }
        }
        throw GltfReadException("glTF JSON could not be read.")
    }

    private fun readArr(): GltfJson.Arr {
        i++
        val items = ArrayList<GltfJson>()
        skip()
        if (i < s.length && s[i] == ']') {
            i++
            return GltfJson.Arr(items)
        }
        while (i < s.length) {
            items.add(readValue())
            skip()
            if (i >= s.length) throw GltfReadException("glTF JSON could not be read.")
            when (s[i]) {
                ',' -> i++
                ']' -> {
                    i++
                    return GltfJson.Arr(items)
                }
                else -> throw GltfReadException("glTF JSON could not be read.")
            }
        }
        throw GltfReadException("glTF JSON could not be read.")
    }

    private fun readString(): String {
        if (i >= s.length || s[i] != '"') throw GltfReadException("glTF JSON could not be read.")
        i++
        val out = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            when (c) {
                '"' -> return out.toString()
                '\\' -> out.append(readEscape())
                else -> out.append(c)
            }
        }
        throw GltfReadException("glTF JSON could not be read.")
    }

    private fun readEscape(): Char {
        if (i >= s.length) throw GltfReadException("glTF JSON could not be read.")
        return when (val c = s[i++]) {
            '"', '\\', '/' -> c
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (i + 4 > s.length) throw GltfReadException("glTF JSON could not be read.")
                val hex = s.substring(i, i + 4)
                i += 4
                hex.toInt(16).toChar()
            }
            else -> throw GltfReadException("glTF JSON could not be read.")
        }
    }

    private fun readNum(): GltfJson.Num {
        val start = i
        if (s[i] == '-') i++
        if (i >= s.length || !s[i].isDigit()) throw GltfReadException("glTF JSON could not be read.")
        if (s[i] == '0') {
            i++
        } else {
            while (i < s.length && s[i].isDigit()) i++
        }
        if (i < s.length && s[i] == '.') {
            i++
            if (i >= s.length || !s[i].isDigit()) throw GltfReadException("glTF JSON could not be read.")
            while (i < s.length && s[i].isDigit()) i++
        }
        if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
            i++
            if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
            if (i >= s.length || !s[i].isDigit()) throw GltfReadException("glTF JSON could not be read.")
            while (i < s.length && s[i].isDigit()) i++
        }
        return GltfJson.Num(s.substring(start, i).toDouble())
    }
}

private class ViewDef(val buffer: Int, val offset: Int, val length: Int, val stride: Int)

private class AccDef(
    val view: Int,
    val offset: Int,
    val componentType: Int,
    val count: Int,
    val type: String,
    val sparse: Boolean
)

private class NodeDef(val mesh: Int?, val children: IntArray, val local: FloatArray)

private class RawMesh(val positions: FloatArray, val indices: IntArray)

private fun parseGlb(bytes: ByteArray): GltfParseResult {
    if (bytes.size < 20) throw GltfReadException("Not a glTF or GLB file.")
    val version = u32(bytes, 4)
    if (version != 2) throw GltfReadException("Unsupported glTF version.")
    var offset = 12
    var json: String? = null
    var bin: ByteArray? = null
    while (offset + 8 <= bytes.size) {
        val len = u32(bytes, offset)
        val type = u32(bytes, offset + 4)
        val start = offset + 8
        if (len < 0 || start > bytes.size || len > bytes.size - start) {
            throw GltfReadException("Truncated glTF chunk.")
        }
        val end = start + len
        if (type == 0x4E4F534A) {
            json = bytes.decodeToString(start, end).trim { it <= ' ' }
        } else if (type == 0x004E4942) {
            bin = bytes.copyOfRange(start, end)
        }
        offset = end
    }
    val text = json ?: throw GltfReadException("GLB is missing JSON.")
    return assemble(text, bin)
}

private fun parseGltfJson(bytes: ByteArray, glbBin: ByteArray?): GltfParseResult {
    var text = bytes.decodeToString()
    if (text.startsWith("\uFEFF")) text = text.substring(1)
    val trimmed = text.trim()
    if (!trimmed.startsWith("{")) throw GltfReadException("Not a glTF or GLB file.")
    return assemble(trimmed, glbBin)
}

private fun assemble(json: String, glbBin: ByteArray?): GltfParseResult {
    val root = GltfJsonReader(json).parseRoot().asObj()
        ?: throw GltfReadException("glTF JSON could not be read.")
    val version = root.obj("asset")?.str("version")
    if (version == null || !version.startsWith("2")) {
        throw GltfReadException("Unsupported glTF version.")
    }
    rejectCompressed(root)
    val bins = readBuffers(root, glbBin)
    val views = readViews(root)
    val accessors = readAccessors(root)
    val meshes = readAllMeshes(root, bins, views, accessors)
    if (meshes.isEmpty()) throw GltfReadException("The prop mesh is missing positions.")
    val merged = mergeInstances(root, meshes)
    val normalized = normalizePositions(merged.positions)
    val thinned = thinPropTriangles(normalized, merged.indices, GLTF_MAX_DRAW_TRIS)
    if (thinned.size < 3) throw GltfReadException("The prop has no triangles.")
    return GltfParseResult.Ok(GltfMesh(normalized, thinned))
}

private fun rejectCompressed(root: GltfJson.Obj) {
    val required = root.arr("extensionsRequired") ?: return
    for (item in required.items) {
        val name = item.asStr() ?: continue
        if (name == "KHR_draco_mesh_compression" ||
            name == "EXT_meshopt_compression" ||
            name == "KHR_mesh_quantization"
        ) {
            throw GltfReadException(
                "Compressed glTF is not supported. Export an uncompressed .glb."
            )
        }
    }
}

private fun readBuffers(root: GltfJson.Obj, glbBin: ByteArray?): List<ByteArray> {
    val arr = root.arr("buffers") ?: throw GltfReadException("glTF is missing buffers.")
    if (arr.items.isEmpty()) throw GltfReadException("glTF is missing buffers.")
    return arr.items.mapIndexed { index, item ->
        val obj = item.asObj() ?: throw GltfReadException("glTF is missing buffers.")
        val uri = obj.str("uri")
        when {
            uri == null && index == 0 && glbBin != null -> glbBin
            uri == null -> throw GltfReadException(
                "This glTF uses an external buffer. Export a .glb or embed the buffer."
            )
            uri.startsWith("data:", ignoreCase = true) -> decodeDataUri(uri)
            else -> throw GltfReadException(
                "This glTF uses an external buffer. Export a .glb or embed the buffer."
            )
        }
    }
}

private fun decodeDataUri(uri: String): ByteArray {
    val comma = uri.indexOf(',')
    if (comma < 0 || !uri.substring(0, comma).contains("base64", ignoreCase = true)) {
        throw GltfReadException("Embedded buffer is not base64.")
    }
    return try {
        Base64.getMimeDecoder().decode(uri.substring(comma + 1))
    } catch (_: IllegalArgumentException) {
        throw GltfReadException("Embedded buffer is not valid base64.")
    }
}

private fun readViews(root: GltfJson.Obj): List<ViewDef> {
    val arr = root.arr("bufferViews") ?: throw GltfReadException("glTF is missing buffer views.")
    return arr.items.map { item ->
        val obj = item.asObj() ?: throw GltfReadException("glTF is missing buffer views.")
        ViewDef(
            buffer = obj.num("buffer")?.toInt() ?: 0,
            offset = obj.num("byteOffset")?.toInt() ?: 0,
            length = obj.num("byteLength")?.toInt()
                ?: throw GltfReadException("glTF is missing buffer views."),
            stride = obj.num("byteStride")?.toInt() ?: 0
        )
    }
}

private fun readAccessors(root: GltfJson.Obj): List<AccDef> {
    val arr = root.arr("accessors") ?: throw GltfReadException("glTF is missing accessors.")
    return arr.items.map { item ->
        val obj = item.asObj() ?: throw GltfReadException("glTF is missing accessors.")
        AccDef(
            view = obj.num("bufferView")?.toInt()
                ?: throw GltfReadException("glTF accessor is missing a buffer view."),
            offset = obj.num("byteOffset")?.toInt() ?: 0,
            componentType = obj.num("componentType")?.toInt()
                ?: throw GltfReadException("glTF accessor is missing a component type."),
            count = obj.num("count")?.toInt()
                ?: throw GltfReadException("glTF accessor is missing a count."),
            type = obj.str("type") ?: throw GltfReadException("glTF accessor is missing a type."),
            sparse = obj.fields.containsKey("sparse")
        )
    }
}

private fun readAllMeshes(
    root: GltfJson.Obj,
    bins: List<ByteArray>,
    views: List<ViewDef>,
    accessors: List<AccDef>
): List<RawMesh> {
    val arr = root.arr("meshes") ?: throw GltfReadException("The prop mesh is missing positions.")
    return arr.items.map { item ->
        val mesh = item.asObj() ?: throw GltfReadException("The prop mesh is missing positions.")
        readMesh(mesh, bins, views, accessors)
    }
}

private fun readMesh(
    mesh: GltfJson.Obj,
    bins: List<ByteArray>,
    views: List<ViewDef>,
    accessors: List<AccDef>
): RawMesh {
    val prims = mesh.arr("primitives") ?: throw GltfReadException("The prop mesh is missing positions.")
    val positions = ArrayList<Float>()
    val indices = ArrayList<Int>()
    for (item in prims.items) {
        val prim = item.asObj() ?: continue
        if (prim.obj("extensions")?.fields?.containsKey("KHR_draco_mesh_compression") == true) {
            throw GltfReadException(
                "Compressed glTF is not supported. Export an uncompressed .glb."
            )
        }
        val mode = prim.num("mode")?.toInt() ?: 4
        if (mode != 4) throw GltfReadException("Only triangle glTF meshes are supported.")
        val posIndex = prim.obj("attributes")?.num("POSITION")?.toInt()
            ?: throw GltfReadException("The prop mesh is missing positions.")
        val pos = readVec3(bins, views, accessors, posIndex)
        val indexAcc = prim.num("indices")?.toInt()
        val local = if (indexAcc == null) {
            if (pos.size / 3 % 3 != 0) {
                throw GltfReadException("glTF indices are not a triangle list.")
            }
            IntArray(pos.size / 3) { it }
        } else {
            readIndices(bins, views, accessors, indexAcc)
        }
        if (local.size % 3 != 0) throw GltfReadException("glTF indices are not a triangle list.")
        val base = positions.size / 3
        for (v in pos) positions.add(v)
        for (idx in local) {
            if (idx < 0 || idx >= pos.size / 3) throw GltfReadException("Index out of range.")
            indices.add(base + idx)
        }
        if (indices.size / 3 > GLTF_MAX_SOURCE_TRIS || positions.size / 3 > GLTF_MAX_VERTICES) {
            throw GltfReadException("This prop is too dense for classical load.")
        }
    }
    if (indices.size < 3) throw GltfReadException("The prop has no triangles.")
    return RawMesh(positions.toFloatArray(), indices.toIntArray())
}

private fun readVec3(
    bins: List<ByteArray>,
    views: List<ViewDef>,
    accessors: List<AccDef>,
    index: Int
): FloatArray {
    val acc = accessors.getOrNull(index) ?: throw GltfReadException("The prop mesh is missing positions.")
    if (acc.sparse) throw GltfReadException("Sparse glTF accessors are not supported.")
    if (acc.componentType != 5126 || acc.type != "VEC3") {
        throw GltfReadException("Positions must be FLOAT VEC3.")
    }
    if (acc.count > GLTF_MAX_VERTICES) throw GltfReadException("This prop is too dense for classical load.")
    val view = views.getOrNull(acc.view) ?: throw GltfReadException("Truncated glTF buffer.")
    val bin = bins.getOrNull(view.buffer) ?: throw GltfReadException("Truncated glTF buffer.")
    val stride = if (view.stride > 0) view.stride else 12
    if (stride < 12) throw GltfReadException("Bad position stride.")
    val out = FloatArray(acc.count * 3)
    for (n in 0 until acc.count) {
        val p = view.offset + acc.offset + n * stride
        if (p < view.offset || p + 12 > view.offset + view.length || p + 12 > bin.size) {
            throw GltfReadException("Truncated glTF buffer.")
        }
        out[n * 3] = f32(bin, p)
        out[n * 3 + 1] = f32(bin, p + 4)
        out[n * 3 + 2] = f32(bin, p + 8)
    }
    return out
}

private fun readIndices(
    bins: List<ByteArray>,
    views: List<ViewDef>,
    accessors: List<AccDef>,
    index: Int
): IntArray {
    val acc = accessors.getOrNull(index) ?: throw GltfReadException("glTF indices are not a triangle list.")
    if (acc.sparse) throw GltfReadException("Sparse glTF accessors are not supported.")
    if (acc.type != "SCALAR") throw GltfReadException("glTF indices are not a triangle list.")
    val comp = when (acc.componentType) {
        5121 -> 1
        5123 -> 2
        5125 -> 4
        else -> throw GltfReadException("Unsupported index type.")
    }
    val view = views.getOrNull(acc.view) ?: throw GltfReadException("Truncated glTF buffer.")
    val bin = bins.getOrNull(view.buffer) ?: throw GltfReadException("Truncated glTF buffer.")
    val stride = if (view.stride > 0) view.stride else comp
    if (stride < comp) throw GltfReadException("Bad index stride.")
    val out = IntArray(acc.count)
    for (n in 0 until acc.count) {
        val p = view.offset + acc.offset + n * stride
        if (p < view.offset || p + comp > view.offset + view.length || p + comp > bin.size) {
            throw GltfReadException("Truncated glTF buffer.")
        }
        val value = when (comp) {
            1 -> bin[p].toInt() and 0xff
            2 -> (bin[p].toInt() and 0xff) or ((bin[p + 1].toInt() and 0xff) shl 8)
            else -> u32(bin, p)
        }
        if (value < 0) throw GltfReadException("Index out of range.")
        out[n] = value
    }
    return out
}

private fun mergeInstances(root: GltfJson.Obj, meshes: List<RawMesh>): RawMesh {
    val nodes = readNodes(root)
    val instances = sceneInstances(root, nodes, meshes.size)
    val positions = ArrayList<Float>()
    val indices = ArrayList<Int>()
    for (instance in instances) {
        val mesh = meshes.getOrNull(instance.mesh) ?: continue
        val base = positions.size / 3
        var i = 0
        while (i < mesh.positions.size) {
            val p = transformPoint(
                instance.world,
                mesh.positions[i],
                mesh.positions[i + 1],
                mesh.positions[i + 2]
            )
            positions.add(p[0])
            positions.add(p[1])
            positions.add(p[2])
            i += 3
        }
        for (idx in mesh.indices) indices.add(base + idx)
        if (indices.size / 3 > GLTF_MAX_SOURCE_TRIS) {
            throw GltfReadException("This prop is too dense for classical load.")
        }
    }
    if (indices.size < 3) throw GltfReadException("The prop has no triangles.")
    return RawMesh(positions.toFloatArray(), indices.toIntArray())
}

private class Instance(val mesh: Int, val world: FloatArray)

private fun readNodes(root: GltfJson.Obj): List<NodeDef> {
    val arr = root.arr("nodes") ?: return emptyList()
    return arr.items.map { item ->
        val obj = item.asObj() ?: throw GltfReadException("Bad node transform.")
        val children = obj.arr("children")?.items?.mapNotNull { it.asNum()?.toInt() }?.toIntArray()
            ?: IntArray(0)
        val matrix = obj.arr("matrix")
        val local = if (matrix != null) {
            if (matrix.items.size < 16) throw GltfReadException("Bad node matrix.")
            FloatArray(16) {
                matrix.items[it].asNum()?.toFloat() ?: throw GltfReadException("Bad node matrix.")
            }
        } else {
            trsMatrix(
                readVecN(obj.arr("translation"), 3),
                readVecN(obj.arr("rotation"), 4),
                readVecN(obj.arr("scale"), 3)
            )
        }
        NodeDef(mesh = obj.num("mesh")?.toInt(), children = children, local = local)
    }
}

private fun readVecN(arr: GltfJson.Arr?, n: Int): FloatArray? {
    if (arr == null) return null
    if (arr.items.size < n) throw GltfReadException("Bad node transform.")
    return FloatArray(n) {
        arr.items[it].asNum()?.toFloat() ?: throw GltfReadException("Bad node transform.")
    }
}

private fun sceneInstances(root: GltfJson.Obj, nodes: List<NodeDef>, meshCount: Int): List<Instance> {
    if (nodes.isEmpty() || meshCount == 0) {
        return listOf(Instance(0, identityMatrix()))
    }
    val parent = IntArray(nodes.size) { -1 }
    for (i in nodes.indices) {
        for (child in nodes[i].children) {
            if (child in nodes.indices) parent[child] = i
        }
    }
    val worldCache = arrayOfNulls<FloatArray>(nodes.size)
    fun world(index: Int, stack: MutableSet<Int>): FloatArray {
        worldCache[index]?.let { return it }
        if (!stack.add(index)) throw GltfReadException("glTF node graph has a cycle.")
        val local = nodes[index].local
        val p = parent[index]
        val result = if (p < 0) local else mulMatrix(world(p, stack), local)
        stack.remove(index)
        worldCache[index] = result
        return result
    }
    val sceneIndex = root.num("scene")?.toInt() ?: 0
    val scene = root.arr("scenes")?.items?.getOrNull(sceneIndex)?.asObj()
    val roots = scene?.arr("nodes")?.items?.mapNotNull { it.asNum()?.toInt() }
        ?: nodes.indices.toList()
    val ordered = ArrayList<Int>()
    val seen = HashSet<Int>()
    fun walk(index: Int) {
        if (index !in nodes.indices || !seen.add(index)) return
        ordered.add(index)
        for (child in nodes[index].children) walk(child)
    }
    for (rootIndex in roots) walk(rootIndex)
    val instances = ArrayList<Instance>()
    for (index in ordered) {
        val mesh = nodes[index].mesh ?: continue
        if (mesh !in 0 until meshCount) continue
        instances.add(Instance(mesh, world(index, HashSet())))
    }
    if (instances.isEmpty()) instances.add(Instance(0, identityMatrix()))
    return instances
}

private fun normalizePositions(positions: FloatArray): FloatArray {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var minZ = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    var maxZ = Float.NEGATIVE_INFINITY
    var i = 0
    while (i < positions.size) {
        val x = positions[i]
        val y = positions[i + 1]
        val z = positions[i + 2]
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
            throw GltfReadException("Prop positions are not finite.")
        }
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (z < minZ) minZ = z
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        if (z > maxZ) maxZ = z
        i += 3
    }
    val cx = (minX + maxX) * 0.5f
    val cy = (minY + maxY) * 0.5f
    val cz = (minZ + maxZ) * 0.5f
    val extent = max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(1e-6f)
    val scale = 2f / extent
    val out = FloatArray(positions.size)
    i = 0
    while (i < positions.size) {
        out[i] = (positions[i] - cx) * scale
        out[i + 1] = (positions[i + 1] - cy) * scale
        out[i + 2] = (positions[i + 2] - cz) * scale
        i += 3
    }
    return out
}

private fun identityMatrix(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)

private fun trsMatrix(t: FloatArray?, r: FloatArray?, s: FloatArray?): FloatArray {
    val sx = s?.getOrNull(0) ?: 1f
    val sy = s?.getOrNull(1) ?: 1f
    val sz = s?.getOrNull(2) ?: 1f
    val out = if (r != null && r.size >= 4) {
        quatMatrix(r[0], r[1], r[2], r[3])
    } else {
        identityMatrix()
    }
    out[0] *= sx
    out[1] *= sx
    out[2] *= sx
    out[4] *= sy
    out[5] *= sy
    out[6] *= sy
    out[8] *= sz
    out[9] *= sz
    out[10] *= sz
    if (t != null && t.size >= 3) {
        out[12] = t[0]
        out[13] = t[1]
        out[14] = t[2]
    }
    return out
}

/** Column-major rotation from a glTF quaternion (x, y, z, w). */
private fun quatMatrix(x: Float, y: Float, z: Float, w: Float): FloatArray {
    val xx = x * x
    val yy = y * y
    val zz = z * z
    val xy = x * y
    val xz = x * z
    val yz = y * z
    val wx = w * x
    val wy = w * y
    val wz = w * z
    return floatArrayOf(
        1f - 2f * (yy + zz), 2f * (xy + wz), 2f * (xz - wy), 0f,
        2f * (xy - wz), 1f - 2f * (xx + zz), 2f * (yz + wx), 0f,
        2f * (xz + wy), 2f * (yz - wx), 1f - 2f * (xx + yy), 0f,
        0f, 0f, 0f, 1f
    )
}

private fun mulMatrix(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(16)
    for (col in 0 until 4) {
        for (row in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += a[k * 4 + row] * b[col * 4 + k]
            }
            out[col * 4 + row] = sum
        }
    }
    return out
}

private fun transformPoint(m: FloatArray, x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    m[0] * x + m[4] * y + m[8] * z + m[12],
    m[1] * x + m[5] * y + m[9] * z + m[13],
    m[2] * x + m[6] * y + m[10] * z + m[14]
)

private fun u32(b: ByteArray, o: Int): Int {
    if (o < 0 || o + 4 > b.size) throw GltfReadException("Truncated glTF buffer.")
    return (b[o].toInt() and 0xff) or
        ((b[o + 1].toInt() and 0xff) shl 8) or
        ((b[o + 2].toInt() and 0xff) shl 16) or
        ((b[o + 3].toInt() and 0xff) shl 24)
}

private fun f32(b: ByteArray, o: Int): Float = java.lang.Float.intBitsToFloat(u32(b, o))
