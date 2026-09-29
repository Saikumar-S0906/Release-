package com.pixijoy.movie

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GltfPropTest {
    @Test
    fun parseGlb_triangle_isUnitBox() {
        val parsed = GltfProp.parse(triangleGlb(rotation = null)) as GltfParseResult.Ok
        assertEquals(1, parsed.mesh.triangleCount)
        assertTrue(span(parsed.mesh, 2) < 0.001f)
        assertTrue(span(parsed.mesh, 0) > 1.9f)
        assertTrue(maxAbs(parsed.mesh) <= 1.001f)
    }

    @Test
    fun parseGltf_embeddedBuffer() {
        val bin = triangleBin()
        val b64 = Base64.getEncoder().encodeToString(bin)
        val json = meshJson(
            buffer = """{"uri":"data:application/octet-stream;base64,$b64","byteLength":42}""",
            rotation = null,
            indexed = true
        )
        val parsed = GltfProp.parse(json.encodeToByteArray()) as GltfParseResult.Ok
        assertEquals(1, parsed.mesh.triangleCount)
        assertTrue(maxAbs(parsed.mesh) <= 1.001f)
    }

    @Test
    fun parseGlb_nodeRotation_movesMeshOffXyPlane() {
        val flat = GltfProp.parse(triangleGlb(rotation = null)) as GltfParseResult.Ok
        val turned = GltfProp.parse(
            triangleGlb(rotation = "[0.70710678,0,0,0.70710678]")
        ) as GltfParseResult.Ok
        assertTrue(span(flat.mesh, 2) < 0.001f)
        assertTrue(span(turned.mesh, 1) < 0.05f)
        assertTrue(span(turned.mesh, 2) > 1.5f)
    }

    @Test
    fun parse_rejectsExternalBuffer_andDraco() {
        val external = meshJson(
            buffer = """{"uri":"mesh.bin","byteLength":42}""",
            rotation = null,
            indexed = true
        )
        val externalErr = GltfProp.parse(external.encodeToByteArray()) as GltfParseResult.Err
        assertTrue(externalErr.message.contains("external"))

        val draco = """
            {"asset":{"version":"2.0"},"extensionsRequired":["KHR_draco_mesh_compression"]}
        """.trimIndent()
        val dracoErr = GltfProp.parse(draco.encodeToByteArray()) as GltfParseResult.Err
        assertTrue(dracoErr.message.contains("Compressed"))
    }

    @Test
    fun parse_nonIndexedTriangle() {
        val bin = triangleBin().copyOf(36)
        val b64 = Base64.getEncoder().encodeToString(bin)
        val json = """
            {"asset":{"version":"2.0"},
             "meshes":[{"primitives":[{"attributes":{"POSITION":0},"mode":4}]}],
             "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}],
             "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":36}],
             "buffers":[{"uri":"data:application/octet-stream;base64,$b64","byteLength":36}]}
        """.trimIndent()
        val parsed = GltfProp.parse(json.encodeToByteArray()) as GltfParseResult.Ok
        assertEquals(1, parsed.mesh.triangleCount)
    }

    @Test
    fun thin_keepsCap_andSmallMeshes() {
        val small = intArrayOf(0, 1, 2)
        assertEquals(3, thinPropTriangles(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f), small, 8).size)
        val positions = FloatArray(12)
        val indices = IntArray(30) { it % 4 }
        val thinned = thinPropTriangles(positions, indices, 2)
        assertEquals(6, thinned.size)
    }

    @Test
    fun safeNames_andPlateMath() {
        assertTrue(GltfProp.isSafePropFileName("chair.glb"))
        assertTrue(GltfProp.isSafePropFileName("foo..gltf"))
        assertFalse(GltfProp.isSafePropFileName("../a.glb"))
        assertFalse(GltfProp.isSafePropFileName("chair.obj"))
        val near = projectPropVertex(1f, 0f, 0f, 0f, 0f, 100f, 100f, 10f)
        val far = projectPropVertex(1f, 0f, 0f, 0f, 0f, 100f, 100f, 20f)
        assertTrue(abs(far.x - 100f) > abs(near.x - 100f))
        val (dx, dy) = cgPlateOffset(0.5f, -0.25f, 200, 100)
        assertEquals(45f, dx, 0.01f)
        assertEquals(11.25f, dy, 0.01f)
    }

    private fun triangleGlb(rotation: String?): ByteArray {
        val bin = triangleBin()
        val json = meshJson("""{"byteLength":42}""", rotation, indexed = true)
        return glb(json, bin)
    }

    private fun meshJson(buffer: String, rotation: String?, indexed: Boolean): String {
        val node = if (rotation == null) {
            ""
        } else {
            """"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0,"rotation":$rotation}],"""
        }
        val indicesAttr = if (indexed) ""","indices":1""" else ""
        val indexAccessor = if (indexed) {
            """,{"bufferView":1,"componentType":5123,"count":3,"type":"SCALAR"}"""
        } else {
            ""
        }
        val indexView = if (indexed) {
            """,{"buffer":0,"byteOffset":36,"byteLength":6}"""
        } else {
            ""
        }
        return """
            {"asset":{"version":"2.0"},$node
             "meshes":[{"primitives":[{"attributes":{"POSITION":0}$indicesAttr,"mode":4}]}],
             "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}$indexAccessor],
             "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":36}$indexView],
             "buffers":[$buffer]}
        """.trimIndent()
    }

    private fun triangleBin(): ByteArray {
        val bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        bb.putFloat(0f)
        bb.putFloat(0f)
        bb.putFloat(0f)
        bb.putFloat(1f)
        bb.putFloat(0f)
        bb.putFloat(0f)
        bb.putFloat(0f)
        bb.putFloat(1f)
        bb.putFloat(0f)
        bb.putShort(0)
        bb.putShort(1)
        bb.putShort(2)
        return bb.array().copyOf(42)
    }

    private fun glb(json: String, bin: ByteArray): ByteArray {
        val jsonBytes = pad4(json.encodeToByteArray(), 0x20)
        val binBytes = pad4(bin, 0)
        val total = 12 + 8 + jsonBytes.size + 8 + binBytes.size
        val out = ByteArray(total)
        putU32(out, 0, 0x46546C67)
        putU32(out, 4, 2)
        putU32(out, 8, total)
        putU32(out, 12, jsonBytes.size)
        putU32(out, 16, 0x4E4F534A)
        jsonBytes.copyInto(out, 20)
        val binAt = 20 + jsonBytes.size
        putU32(out, binAt, binBytes.size)
        putU32(out, binAt + 4, 0x004E4942)
        binBytes.copyInto(out, binAt + 8)
        return out
    }

    private fun pad4(bytes: ByteArray, pad: Byte): ByteArray {
        val rem = bytes.size % 4
        if (rem == 0) return bytes
        return bytes + ByteArray(4 - rem) { pad }
    }

    private fun putU32(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xff).toByte()
        out[offset + 1] = ((value shr 8) and 0xff).toByte()
        out[offset + 2] = ((value shr 16) and 0xff).toByte()
        out[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun span(mesh: GltfMesh, axis: Int): Float {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        var i = axis
        while (i < mesh.positions.size) {
            val v = mesh.positions[i]
            if (v < min) min = v
            if (v > max) max = v
            i += 3
        }
        return max - min
    }

    private fun maxAbs(mesh: GltfMesh): Float {
        var max = 0f
        for (v in mesh.positions) {
            val a = abs(v)
            if (a > max) max = a
        }
        return max
    }
}
