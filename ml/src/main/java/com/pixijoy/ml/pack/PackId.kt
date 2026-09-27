package com.pixijoy.ml.pack

/**
 * On-demand ML packs. Small TFLite packs may ship in `:ml` assets and seed into
 * the pack store on first use. Large packs (FILM, oversized inpaint) use HTTPS
 * Download or sideload — never invent weights.
 * [PackId.Track], [PackId.SuperRes], and [PackId.Denoise] ship in APK assets.
 */
enum class PackId {
    Inpaint,
    SuperRes,
    Denoise,
    FrameInterp,
    Track
}

data class PackSpec(
    val id: PackId,
    val version: String,
    val fileName: String,
    val playModule: String,
    /** When null, sideload records the SHA-256 of the copied file. */
    val expectedSha256: String? = null,
    /** HTTPS fallback when Play Feature Delivery is missing (emulator / sideload APK). */
    val downloadUrl: String? = null,
    /** Required when [downloadUrl] is set. Sideload does not use this pin. */
    val downloadSha256: String? = null,
    /**
     * Asset path under the `:ml` module (e.g. `track.tflite`). When set, the pack
     * is copied from APK assets into the on-device store — no Download CTA.
     */
    val assetPath: String? = null
)

object PackCatalog {
    const val VERSION = "1"

    /**
     * Distilled ESRGAN (Apache-2.0), ~33 KB. Tensor contract is 4× RGB super-res.
     * Shipped in APK assets (`ml/src/main/assets/superres.tflite`).
     */
    const val SUPERRES_ASSET_PATH = "superres.tflite"
    const val SUPERRES_ASSET_SHA256 =
        "27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f"
    /** Provenance URL (not used for Download once bundled). */
    const val SUPERRES_DOWNLOAD_URL =
        "https://github.com/captain-pool/GSOC/releases/download/2.0.0/compressed_esrgan.tflite"
    const val SUPERRES_DOWNLOAD_SHA256 = SUPERRES_ASSET_SHA256

    /**
     * Raspberry Pi UNet RGB denoise (BSD-3-Clause), ~700 KB, 256×256 NHWC.
     * Shipped in APK assets (`ml/src/main/assets/denoise.tflite`).
     * https://github.com/raspberrypi/AI_denoise
     */
    const val DENOISE_ASSET_PATH = "denoise.tflite"
    const val DENOISE_ASSET_SHA256 =
        "53f0112f248b18ae518af64942ea4741f5b22ccdc4af5aea0ff452875f17fb84"
    const val DENOISE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/raspberrypi/AI_denoise/master/networks/unet_rgb_fast.tflite"
    const val DENOISE_DOWNLOAD_SHA256 = DENOISE_ASSET_SHA256

    /**
     * FILM style frame-interp TFLite (Apache-2.0 lineage via Google FILM), ~67 MB.
     * Live Download is the public [Release-](https://github.com/Saikumar-S0906/Release-)
     * repo asset (SHA-pinned). Provenance URL retained for attribution only.
     */
    const val FRAME_INTERP_DOWNLOAD_URL =
        "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v1/interp.tflite"
    const val FRAME_INTERP_DOWNLOAD_SHA256 =
        "3a201916658277ed6e7626d93a6ee117a47b04bc1986579600a90feabfe05eb4"
    const val FRAME_INTERP_PROVENANCE_URL =
        "https://raw.githubusercontent.com/egemenertugrul/frame-interpolation-tflite-unity/main/FILM_TFLite_UnityProject/Assets/StreamingAssets/model_512_quant_float16.tflite"
    /** Public ML artifact host for FILM, inpaint (when large), and future packs. */
    const val ML_RELEASE_REPO = "https://github.com/Saikumar-S0906/Release-"
    const val ML_RELEASE_TAG = "ml-packs-v1"

    /**
     * PixiJoy F1 point-track TFLite (Apache-2.0), synthetic-trained Siamese,
     * FLOAT32 template 64×64 + search 128×128 → normalized xy. Shipped in APK
     * assets (`ml/src/main/assets/track.tflite`), not downloaded.
     */
    const val TRACK_ASSET_PATH = "track.tflite"
    const val TRACK_ASSET_SHA256 =
        "e2eb42c38ac53b9f708811f2887b53f4eb9774d9cb1bfff058a197d9fe26301f"

    fun spec(id: PackId): PackSpec = when (id) {
        PackId.Inpaint -> PackSpec(id, VERSION, "inpaint.tflite", "ml_inpaint")
        PackId.SuperRes -> PackSpec(
            id,
            VERSION,
            "superres.tflite",
            "ml_superres",
            expectedSha256 = SUPERRES_ASSET_SHA256,
            assetPath = SUPERRES_ASSET_PATH
        )
        PackId.Denoise -> PackSpec(
            id,
            VERSION,
            "denoise.tflite",
            "ml_denoise",
            expectedSha256 = DENOISE_ASSET_SHA256,
            assetPath = DENOISE_ASSET_PATH
        )
        PackId.FrameInterp -> PackSpec(
            id,
            VERSION,
            "interp.tflite",
            "ml_interp",
            downloadUrl = FRAME_INTERP_DOWNLOAD_URL,
            downloadSha256 = FRAME_INTERP_DOWNLOAD_SHA256
        )
        PackId.Track -> PackSpec(
            id,
            VERSION,
            "track.tflite",
            "ml_track",
            expectedSha256 = TRACK_ASSET_SHA256,
            assetPath = TRACK_ASSET_PATH
        )
    }

    fun isBundled(id: PackId): Boolean = !spec(id).assetPath.isNullOrBlank()

    fun hasHttpsFallback(spec: PackSpec): Boolean =
        !spec.downloadUrl.isNullOrBlank() && !spec.downloadSha256.isNullOrBlank()

    fun installUnavailableMessage(spec: PackSpec, playError: String?): String {
        if (!spec.assetPath.isNullOrBlank()) {
            return "This pack ships in the app. Restart Studio if it is not ready."
        }
        val sideload = "Copy ${spec.fileName} into the incoming folder, or use Install from file."
        return if (!hasHttpsFallback(spec)) {
            buildString {
                append("This pack is not hosted for download in this build. ")
                append(sideload)
                if (!playError.isNullOrBlank()) {
                    append(" (")
                    append(playError)
                    append(")")
                }
            }
        } else {
            playError ?: "Pack download failed. $sideload"
        }
    }

    fun all(): List<PackSpec> = PackId.entries.map(::spec)
}
