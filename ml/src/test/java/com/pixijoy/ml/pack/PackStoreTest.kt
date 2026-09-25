package com.pixijoy.ml.pack

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackStoreTest {
    @Test
    fun install_recordsChecksumAndReady() {
        val root = File.createTempFile("packs", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            val store = PackStore(root)
            val spec = PackCatalog.spec(PackId.Inpaint)
            val src = File(root, "src.bin")
            src.writeBytes(ByteArray(128) { it.toByte() })
            val state = store.installFrom(spec, src)
            assertTrue(state is PackInstallState.Ready)
            assertTrue(store.isReady(spec))
            val sha = (state as PackInstallState.Ready).sha256
            assertEquals(64, sha.length)
            assertTrue(PackChecksum.matches(store.modelFile(spec), sha))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun tinyFile_isRejected() {
        val root = File.createTempFile("packs", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            val store = PackStore(root)
            val spec = PackCatalog.spec(PackId.Denoise)
            val src = File(root, "tiny.bin")
            src.writeBytes(ByteArray(8))
            val state = store.installFrom(spec, src)
            assertTrue(state is PackInstallState.Failed)
            assertFalse(store.isReady(spec))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun expectedHashMismatch_fails() {
        val root = File.createTempFile("packs", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            val store = PackStore(root)
            val spec = PackCatalog.spec(PackId.SuperRes).copy(expectedSha256 = "00".repeat(32))
            val src = File(root, "src.bin")
            src.writeBytes(ByteArray(128) { 1 })
            val state = store.installFrom(spec, src)
            assertTrue(state is PackInstallState.Failed)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun catalog_coversAllPacks() {
        assertEquals(5, PackCatalog.all().size)
        assertEquals("ml_inpaint", PackCatalog.spec(PackId.Inpaint).playModule)
        assertEquals("ml_track", PackCatalog.spec(PackId.Track).playModule)
        val sr = PackCatalog.spec(PackId.SuperRes)
        assertTrue(sr.downloadUrl!!.startsWith("https://"))
        assertEquals(64, sr.downloadSha256!!.length)
        val denoise = PackCatalog.spec(PackId.Denoise)
        assertTrue(denoise.downloadUrl!!.startsWith("https://"))
        assertEquals(64, denoise.downloadSha256!!.length)
        assertEquals(null, PackCatalog.spec(PackId.Inpaint).downloadUrl)
        assertFalse(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.FrameInterp)))
        val sideloadHint = PackCatalog.installUnavailableMessage(
            PackCatalog.spec(PackId.Inpaint),
            "Play is unavailable."
        )
        assertTrue(sideloadHint.contains("inpaint.tflite"))
        assertTrue(sideloadHint.contains("not hosted for download"))
    }
}
