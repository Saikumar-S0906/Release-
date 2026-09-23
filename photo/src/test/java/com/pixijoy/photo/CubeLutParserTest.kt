package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CubeLutParserTest {
    @Test
    fun parseIdentity2_ok() {
        val text = """
            TITLE "Identity"
            LUT_3D_SIZE 2
            0 0 0
            1 0 0
            0 1 0
            1 1 0
            0 0 1
            1 0 1
            0 1 1
            1 1 1
        """.trimIndent()
        val result = CubeLutParser.parseCubeText(text)
        assertTrue(result is LutParseResult.Ok)
        val lut = (result as LutParseResult.Ok).lut
        assertEquals(2, lut.size)
        assertEquals("Identity", lut.title)
        val white = CubeLutParser.sampleNearest(lut, 1f, 1f, 1f)
        assertEquals(1f, white[0], 0.001f)
        assertEquals(1f, white[1], 0.001f)
        assertEquals(1f, white[2], 0.001f)
    }

    @Test
    fun malformed_missingSize() {
        val result = CubeLutParser.parseCubeText("0 0 0\n1 1 1\n")
        assertTrue(result is LutParseResult.Err)
    }

    @Test
    fun oversized_rejected() {
        val text = """
            LUT_3D_SIZE 99
            0 0 0
        """.trimIndent()
        val result = CubeLutParser.parseCubeText(text, maxSize = 65)
        assertTrue(result is LutParseResult.Err)
    }

    @Test
    fun parse3dl_size2() {
        val text = """
            LUT_3D_SIZE 2
            0 0 0
            4095 0 0
            0 4095 0
            4095 4095 0
            0 0 4095
            4095 0 4095
            0 4095 4095
            4095 4095 4095
        """.trimIndent()
        val result = CubeLutParser.parse3dlText(text)
        assertTrue(result is LutParseResult.Ok)
        val lut = (result as LutParseResult.Ok).lut
        assertEquals(2, lut.size)
        val white = CubeLutParser.sampleNearest(lut, 1f, 1f, 1f)
        assertEquals(1f, white[0], 0.01f)
    }

    @Test
    fun importedKey_roundTripGraph() {
        val key = ImportedLuts.keyFor("Warm_Look.CUBE")
        assertTrue(ImportedLuts.isImported(key))
        assertEquals("Warm_Look.CUBE", ImportedLuts.fileName(key))
        val encoded = listOf(PhotoOp.Lut(key)).encodeGraph()
        val decoded = encoded.decodeGraph().filterIsInstance<PhotoOp.Lut>().single()
        assertEquals(key, decoded.assetKey)
        assertFalse(decoded.isIdentity())
    }

    @Test
    fun parseFile_cube() {
        val tmp = File.createTempFile("pixi", ".cube")
        tmp.writeText(
            """
            LUT_3D_SIZE 2
            0 0 0
            1 0 0
            0 1 0
            1 1 0
            0 0 1
            1 0 1
            0 1 1
            1 1 1
            """.trimIndent()
        )
        try {
            assertTrue(CubeLutParser.parseFile(tmp) is LutParseResult.Ok)
        } finally {
            tmp.delete()
        }
    }
}
