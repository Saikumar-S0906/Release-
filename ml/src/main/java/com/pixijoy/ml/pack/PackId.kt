package com.pixijoy.ml.pack

/**
 * On-demand ML packs. Small TFLite packs may ship in `:ml` assets and seed into
 * the pack store on first use. Large packs (FILM, oversized inpaint, depth)
 * use HTTPS Download or sideload — never invent weights, and never put a graph
 * over the asset budget into the base APK.
 * [PackId.Track], [PackId.SuperRes], and [PackId.Denoise] ship in APK assets.
 * [PackId.Inpaint] and [PackId.Depth] download from Release- (SHA-pinned). They
 * are not APK assets. Install from file still accepts a local copy.
 */
enum class PackId {
    Inpaint,
    SuperRes,
    Denoise,
    FrameInterp,
    Track,
    Depth,
    /** Hair-level subject matte (MODNet). MediaPipe selfie stays the live-preview tier. */
    Matting,
    /** Dense optical flow (NeuFlow v2) for stabilize / motion trails. Classical DIS/crop stays fallback. */
    Flow
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
    /** Public ML artifact host. Asset URLs use /releases/download/, not /releases/tag/. */
    const val ML_RELEASE_REPO = "https://github.com/Saikumar-S0906/Release-"
    const val ML_RELEASE_TAG = "ml-packs-v1"

    /**
     * v2 packs (Model Lab, docs/MODEL_LAB_REPORT.md): pretrained, int8-weight, FLOAT32 I/O, same
     * tensor contracts as v1. Spec version "2" so devices holding a v1 file re-download.
     */
    const val ML_V2_TAG = "ml-packs-v2"
    const val V2 = "2"

    /**
     * MI-GAN 256 Places2 (MIT), ~7.1 MB, FLOAT32 image 256×256×3 + mask 256×256×1 (1 = hole) → RGB.
     * The graph blanks the hole itself, matching [com.pixijoy.ml.PackLiteRt] feeding original pixels.
     * Release- `ml-packs-v2` asset. Telea stays the fallback.
     */
    const val INPAINT_DOWNLOAD_URL =
        "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v2/inpaint.tflite"
    const val INPAINT_DOWNLOAD_SHA256 =
        "3d9d6b30a1fe5bed0eb231c9877d5668380490229e8695a142df4872f65e1809"

    /**
     * Depth Anything V2 Small (Apache-2.0), ~26 MB, FLOAT32 RGB 256 → depth 256 (0 near, 1 far).
     * Release- `ml-packs-v2` asset. Manual Z stays the fallback.
     */
    const val DEPTH_DOWNLOAD_URL =
        "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v2/depth.tflite"
    const val DEPTH_DOWNLOAD_SHA256 =
        "251c44443c96b66790cc0f54aaf1e746de9d36af4b2e0ed01e504ee0d82e87ad"

    /**
     * MODNet photographic portrait matting (Apache-2.0), ~6.8 MB, FLOAT32 RGB 256 → alpha 256.
     * Release- `ml-packs-v2` asset. MediaPipe selfie segmenter stays the preview / fallback.
     */
    const val MATTING_DOWNLOAD_URL =
        "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v2/matting.tflite"
    const val MATTING_DOWNLOAD_SHA256 =
        "9902196ea431f4e7611d370561030e969fee45e309915dfa890b3a887ad60ec4"

    /**
     * PixiJoy F1 point-track TFLite (Apache-2.0), synthetic-trained Siamese,
     * FLOAT32 template 64×64 + search 128×128 → normalized xy. Shipped in APK
     * assets (`ml/src/main/assets/track.tflite`), not downloaded.
     */
    const val TRACK_ASSET_PATH = "track.tflite"
    const val TRACK_ASSET_SHA256 =
        "e2eb42c38ac53b9f708811f2887b53f4eb9774d9cb1bfff058a197d9fe26301f"

    /**
     * NeuFlow v2 "things" (Apache-2.0), ~36 MB FLOAT32, two RGB 256×256 frames → flow 256×256×2
     * as (dx, dy) / 256. Sintel EPE 0.76 px on the 256 grid (OpenCV DIS 1.64).
     */
    const val FLOW_DOWNLOAD_URL =
        "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v2/flow.tflite"
    const val FLOW_DOWNLOAD_SHA256 =
        "7fdffa3e5077201f351ca86975ef119ead2d7c1a39755114e8fbc9ead501a57a"

    /**
     * Flagship HQ tier (`ml-packs-v2-hq`, chosen by [PackTier]): same contracts at 512² tensors.
     * Depth Anything V2 Small @518, MI-GAN 512 Places2, MODNet @512. Version "2hq" keeps a separate
     * store folder, so a phone never mixes tiers.
     */
    const val ML_V2_HQ_TAG = "ml-packs-v2-hq"
    const val V2_HQ = "2hq"
    private const val HQ_BASE = "https://github.com/Saikumar-S0906/Release-/releases/download/ml-packs-v2-hq/"
    const val DEPTH_HQ_SHA256 = "6addaa7826620c017a589ead0aff3bbdaea3a0a7aa8400d8619dfeb86f094960"
    const val INPAINT_HQ_SHA256 = "5769125dc24b60a7160f6854fb2adfea4780b66052fa1fa71e523ec238fb1c50"
    const val MATTING_HQ_SHA256 = "e715a2c951445b705d1dc071742a906720cf2fb55be5819479cf68b0edbf0a2d"

    fun hqSpec(id: PackId): PackSpec? = when (id) {
        PackId.Depth -> PackSpec(id, V2_HQ, "depth_hq.tflite", "ml_depth",
            downloadUrl = HQ_BASE + "depth_hq.tflite", downloadSha256 = DEPTH_HQ_SHA256)
        PackId.Inpaint -> PackSpec(id, V2_HQ, "inpaint_hq.tflite", "ml_inpaint",
            downloadUrl = HQ_BASE + "inpaint_hq.tflite", downloadSha256 = INPAINT_HQ_SHA256)
        PackId.Matting -> PackSpec(id, V2_HQ, "matting_hq.tflite", "ml_matting",
            downloadUrl = HQ_BASE + "matting_hq.tflite", downloadSha256 = MATTING_HQ_SHA256)
        else -> null
    }

    fun spec(id: PackId): PackSpec = if (PackTier.hq) hqSpec(id) ?: standardSpec(id) else standardSpec(id)

    fun standardSpec(id: PackId): PackSpec = when (id) {
        PackId.Inpaint -> PackSpec(
            id,
            V2,
            "inpaint.tflite",
            "ml_inpaint",
            downloadUrl = INPAINT_DOWNLOAD_URL,
            downloadSha256 = INPAINT_DOWNLOAD_SHA256
        )
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
        PackId.Depth -> PackSpec(
            id,
            V2,
            "depth.tflite",
            "ml_depth",
            downloadUrl = DEPTH_DOWNLOAD_URL,
            downloadSha256 = DEPTH_DOWNLOAD_SHA256
        )
        PackId.Matting -> PackSpec(
            id,
            V2,
            "matting.tflite",
            "ml_matting",
            downloadUrl = MATTING_DOWNLOAD_URL,
            downloadSha256 = MATTING_DOWNLOAD_SHA256
        )
        PackId.Flow -> PackSpec(
            id,
            V2,
            "flow.tflite",
            "ml_flow",
            downloadUrl = FLOW_DOWNLOAD_URL,
            downloadSha256 = FLOW_DOWNLOAD_SHA256
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
