package com.pixijoy.ml.pack

/**
 * On-demand ML packs. Models are never shipped in the base APK.
 * Play Feature Delivery module names are stable; sideload uses [PackSpec.fileName]
 * under the app incoming folder when Play is unavailable.
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
    val downloadSha256: String? = null
)

object PackCatalog {
    const val VERSION = "1"

    /**
     * Distilled ESRGAN (Apache-2.0), ~33 KB. Tensor contract is 4× RGB super-res.
     * Photos never upload; only this model file is fetched after the user taps Download.
     */
    const val SUPERRES_DOWNLOAD_URL =
        "https://github.com/captain-pool/GSOC/releases/download/2.0.0/compressed_esrgan.tflite"
    const val SUPERRES_DOWNLOAD_SHA256 =
        "27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f"

    fun spec(id: PackId): PackSpec = when (id) {
        PackId.Inpaint -> PackSpec(id, VERSION, "inpaint.tflite", "ml_inpaint")
        PackId.SuperRes -> PackSpec(
            id,
            VERSION,
            "superres.tflite",
            "ml_superres",
            downloadUrl = SUPERRES_DOWNLOAD_URL,
            downloadSha256 = SUPERRES_DOWNLOAD_SHA256
        )
        PackId.Denoise -> PackSpec(id, VERSION, "denoise.tflite", "ml_denoise")
        PackId.FrameInterp -> PackSpec(id, VERSION, "interp.tflite", "ml_interp")
        PackId.Track -> PackSpec(id, VERSION, "track.tflite", "ml_track")
    }

    fun all(): List<PackSpec> = PackId.entries.map(::spec)
}
