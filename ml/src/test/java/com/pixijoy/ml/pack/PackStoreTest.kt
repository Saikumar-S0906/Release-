package com.pixijoy.ml.pack

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(6, PackCatalog.all().size)
        val depth = PackCatalog.spec(PackId.Depth)
        assertEquals("depth.tflite", depth.fileName)
        assertNull(depth.expectedSha256)
        assertNull(depth.assetPath)
        assertEquals(PackCatalog.DEPTH_DOWNLOAD_URL, depth.downloadUrl)
        assertEquals(PackCatalog.DEPTH_DOWNLOAD_SHA256, depth.downloadSha256)
        assertEquals("ml_inpaint", PackCatalog.spec(PackId.Inpaint).playModule)
        assertEquals("ml_track", PackCatalog.spec(PackId.Track).playModule)
        val sr = PackCatalog.spec(PackId.SuperRes)
        assertEquals("superres.tflite", sr.fileName)
        assertEquals("ml_superres", sr.playModule)
        assertNull(sr.downloadUrl)
        assertNull(sr.downloadSha256)
        assertEquals(PackCatalog.SUPERRES_ASSET_SHA256, sr.expectedSha256)
        assertEquals(PackCatalog.SUPERRES_ASSET_PATH, sr.assetPath)
        assertTrue(PackCatalog.isBundled(PackId.SuperRes))
        val denoise = PackCatalog.spec(PackId.Denoise)
        assertEquals("denoise.tflite", denoise.fileName)
        assertEquals("ml_denoise", denoise.playModule)
        assertNull(denoise.downloadUrl)
        assertNull(denoise.downloadSha256)
        assertEquals(PackCatalog.DENOISE_ASSET_SHA256, denoise.expectedSha256)
        assertEquals(PackCatalog.DENOISE_ASSET_PATH, denoise.assetPath)
        assertTrue(PackCatalog.isBundled(PackId.Denoise))
        assertEquals(
            PackCatalog.INPAINT_DOWNLOAD_URL,
            PackCatalog.spec(PackId.Inpaint).downloadUrl
        )
        assertEquals(
            PackCatalog.INPAINT_DOWNLOAD_SHA256,
            PackCatalog.spec(PackId.Inpaint).downloadSha256
        )
        assertNull(PackCatalog.spec(PackId.Inpaint).expectedSha256)
        assertNull(PackCatalog.spec(PackId.Inpaint).assetPath)
        assertTrue(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.FrameInterp)))
        assertEquals(
            PackCatalog.FRAME_INTERP_DOWNLOAD_URL,
            PackCatalog.spec(PackId.FrameInterp).downloadUrl
        )
        assertEquals(
            PackCatalog.FRAME_INTERP_DOWNLOAD_SHA256,
            PackCatalog.spec(PackId.FrameInterp).downloadSha256
        )
        assertFalse(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.Track)))
        assertTrue(PackCatalog.isBundled(PackId.Track))
        assertEquals(
            PackCatalog.TRACK_ASSET_PATH,
            PackCatalog.spec(PackId.Track).assetPath
        )
        assertEquals(
            PackCatalog.TRACK_ASSET_SHA256,
            PackCatalog.spec(PackId.Track).expectedSha256
        )
        assertTrue(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.Inpaint)))
        assertTrue(PackCatalog.hasHttpsFallback(PackCatalog.spec(PackId.Depth)))
        val hostedHint = PackCatalog.installUnavailableMessage(
            PackCatalog.spec(PackId.Inpaint),
            "Play is unavailable."
        )
        assertEquals("Play is unavailable.", hostedHint)
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
            "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v1/interp.tflite",
            PackCatalog.FRAME_INTERP_DOWNLOAD_URL
        )
        assertEquals(
            "3a201916658277ed6e7626d93a6ee117a47b04bc1986579600a90feabfe05eb4",
            PackCatalog.FRAME_INTERP_DOWNLOAD_SHA256
        )
        assertEquals(
            "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v1/inpaint.tflite",
            PackCatalog.INPAINT_DOWNLOAD_URL
        )
        assertEquals(
            "6ca50eaad897f046245d956c1e4c77d12b885e61fa93a953c48967ecc1afb73c",
            PackCatalog.INPAINT_DOWNLOAD_SHA256
        )
        assertEquals(
            "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v1/depth.tflite",
            PackCatalog.DEPTH_DOWNLOAD_URL
        )
        assertEquals(
            "a6b296193d7497ded445a1c257a08d4a9d49faad2a8a728767e9d1402287db1f",
            PackCatalog.DEPTH_DOWNLOAD_SHA256
        )
        assertEquals(
            "e2eb42c38ac53b9f708811f2887b53f4eb9774d9cb1bfff058a197d9fe26301f",
            PackCatalog.TRACK_ASSET_SHA256
        )
        assertEquals(
            setOf(PackId.FrameInterp, PackId.Inpaint, PackId.Depth),
            PackId.entries.filter { PackCatalog.hasHttpsFallback(PackCatalog.spec(it)) }.toSet()
        )
        assertEquals(
            setOf(PackId.SuperRes, PackId.Denoise, PackId.Track),
            PackId.entries.filter { PackCatalog.isBundled(it) }.toSet()
        )
    }

    @Test
    fun track_isBundledInApkAssets() {
        val track = PackCatalog.spec(PackId.Track)
        assertNull(track.downloadUrl)
        assertNull(track.downloadSha256)
        assertEquals(PackCatalog.TRACK_ASSET_SHA256, track.expectedSha256)
        assertEquals(PackCatalog.TRACK_ASSET_PATH, track.assetPath)
        assertTrue(PackCatalog.isBundled(PackId.Track))
        assertFalse(PackCatalog.hasHttpsFallback(track))
        assertEquals("track.tflite", track.fileName)
        assertEquals("ml_track", track.playModule)
        val interp = PackCatalog.spec(PackId.FrameInterp)
        assertEquals("interp.tflite", interp.fileName)
        assertEquals("ml_interp", interp.playModule)
        assertTrue(PackCatalog.hasHttpsFallback(interp))
    }

    @Test
    fun superResAndDenoise_areBundledInApkAssets() {
        for (id in listOf(PackId.SuperRes, PackId.Denoise)) {
            val spec = PackCatalog.spec(id)
            assertTrue(PackCatalog.isBundled(id))
            assertFalse(PackCatalog.hasHttpsFallback(spec))
            assertNull(spec.downloadUrl)
            assertNotNull(spec.assetPath)
            assertNotNull(spec.expectedSha256)
        }
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
