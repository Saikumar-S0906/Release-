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
    val expectedSha256: String? = null
)

object PackCatalog {
    const val VERSION = "1"

    fun spec(id: PackId): PackSpec = when (id) {
        PackId.Inpaint -> PackSpec(id, VERSION, "inpaint.tflite", "ml_inpaint")
        PackId.SuperRes -> PackSpec(id, VERSION, "superres.tflite", "ml_superres")
        PackId.Denoise -> PackSpec(id, VERSION, "denoise.tflite", "ml_denoise")
        PackId.FrameInterp -> PackSpec(id, VERSION, "interp.tflite", "ml_interp")
        PackId.Track -> PackSpec(id, VERSION, "track.tflite", "ml_track")
    }

    fun all(): List<PackSpec> = PackId.entries.map(::spec)
}
