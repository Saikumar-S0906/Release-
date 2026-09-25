package com.pixijoy.ml.pack

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackChecksumTest {
    @Test
    fun sha256_isStableHex() {
        val empty = PackChecksum.sha256(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            empty
        )
        val abc = PackChecksum.sha256("abc".toByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            abc
        )
    }

    @Test
    fun tfliteMagic_readsFlatBufferId() {
        val tmp = File.createTempFile("pack", ".tflite")
        try {
            tmp.writeBytes(byteArrayOf(0, 0, 0, 0, 'T'.code.toByte(), 'F'.code.toByte(), 'L'.code.toByte(), '3'.code.toByte()))
            assertTrue(PackChecksum.looksLikeTflite(tmp))
            tmp.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
            assertFalse(PackChecksum.looksLikeTflite(tmp))
        } finally {
            tmp.delete()
        }
    }
}
