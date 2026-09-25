package com.pixijoy.ml.pack

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
        assertEquals("superres.tflite", sr.fileName)
        assertEquals("ml_superres", sr.playModule)
        assertEquals(PackCatalog.SUPERRES_DOWNLOAD_URL, sr.downloadUrl)
        assertEquals(PackCatalog.SUPERRES_DOWNLOAD_SHA256, sr.downloadSha256)
        assertNull(sr.expectedSha256)
        val denoise = PackCatalog.spec(PackId.Denoise)
        assertEquals("denoise.tflite", denoise.fileName)
        assertEquals("ml_denoise", denoise.playModule)
        assertEquals(PackCatalog.DENOISE_DOWNLOAD_URL, denoise.downloadUrl)
        assertEquals(PackCatalog.DENOISE_DOWNLOAD_SHA256, denoise.downloadSha256)
        assertNull(denoise.expectedSha256)
        assertEquals(null, PackCatalog.spec(PackId.Inpaint).downloadUrl)
        assertNull(PackCatalog.spec(PackId.Inpaint).downloadSha256)
        assertNull(PackCatalog.spec(PackId.Inpaint).expectedSha256)
        assertFalse(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.FrameInterp)))
        assertFalse(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.Track)))
        val sideloadHint = PackCatalog.installUnavailableMessage(
            PackCatalog.spec(PackId.Inpaint),
            "Play is unavailable."
        )
        assertTrue(sideloadHint.contains("inpaint.tflite"))
        assertTrue(sideloadHint.contains("not hosted for download"))
    }

    @Test
    fun catalog_httpsPinsMatchArchitectArtifacts() {
        assertEquals(
            "https://github.com/captain-pool/GSOC/releases/download/2.0.0/compressed_esrgan.tflite",
            PackCatalog.SUPERRES_DOWNLOAD_URL
        )
        assertEquals(
            "27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f",
            PackCatalog.SUPERRES_DOWNLOAD_SHA256
        )
        assertEquals(
            "https://raw.githubusercontent.com/raspberrypi/AI_denoise/master/networks/unet_rgb_fast.tflite",
            PackCatalog.DENOISE_DOWNLOAD_URL
        )
        assertEquals(
            "53f0112f248b18ae518af64942ea4741f5b22ccdc4af5aea0ff452875f17fb84",
            PackCatalog.DENOISE_DOWNLOAD_SHA256
        )
        assertEquals(
            setOf(PackId.SuperRes, PackId.Denoise),
            PackId.entries.filter { PackCatalog.hasHttpsFallback(PackCatalog.spec(it)) }.toSet()
        )
    }

    @Test
    fun frameInterpAndTrack_stayUnpinned() {
        listOf(PackId.FrameInterp, PackId.Track).forEach { id ->
            val spec = PackCatalog.spec(id)
            assertNull(spec.downloadUrl)
            assertNull(spec.downloadSha256)
            assertNull(spec.expectedSha256)
            assertFalse(PackCatalog.hasHttpsFallback(spec))
        }
        assertEquals("interp.tflite", PackCatalog.spec(PackId.FrameInterp).fileName)
        assertEquals("ml_interp", PackCatalog.spec(PackId.FrameInterp).playModule)
        assertEquals("track.tflite", PackCatalog.spec(PackId.Track).fileName)
        assertEquals("ml_track", PackCatalog.spec(PackId.Track).playModule)
    }

    @Test
    fun architectPin_rejectsStandInBytes() {
        val root = File.createTempFile("packs", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            val store = PackStore(root)
            val src = File(root, "stand-in.bin")
            src.writeBytes(ByteArray(128) { 7 })
            val sha = PackChecksum.sha256(src)
            assertNotEquals(PackCatalog.SUPERRES_DOWNLOAD_SHA256, sha)
            assertNotEquals(PackCatalog.DENOISE_DOWNLOAD_SHA256, sha)
            listOf(
                PackId.SuperRes to PackCatalog.SUPERRES_DOWNLOAD_SHA256,
                PackId.Denoise to PackCatalog.DENOISE_DOWNLOAD_SHA256
            ).forEach { (id, pin) ->
                val spec = PackCatalog.spec(id).copy(expectedSha256 = pin)
                val state = store.installFrom(spec, src)
                assertTrue(state is PackInstallState.Failed)
                assertFalse(store.isReady(spec))
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
