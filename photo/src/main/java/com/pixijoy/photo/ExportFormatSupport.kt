package com.pixijoy.photo

/**
 * Platform capability + chip labels for photo export formats.
 * HEIC uses androidx.heifwriter when present; AVIF has no stock encoder on
 * minSdk 31 devices, so selection falls back to JPEG with a visible chip label.
 */
object ExportFormatSupport {
    fun canEncodeHeic(): Boolean =
        runCatching { Class.forName("androidx.heifwriter.HeifWriter") }.isSuccess

    /** Stock Android does not expose a Bitmap→AVIF encoder; keep false until one lands. */
    fun canEncodeAvif(): Boolean = false

    fun chipFormats(): List<Pair<String, String>> = listOf(
        "jpeg" to "JPEG",
        "png" to "PNG",
        "webp" to "WebP",
        "heic" to if (canEncodeHeic()) "HEIC" else "HEIC→JPEG",
        "avif" to if (canEncodeAvif()) "AVIF" else "AVIF→JPEG"
    )

    fun parseKey(key: String): ExportFormat = when (key.lowercase()) {
        "png" -> ExportFormat.PNG
        "webp" -> ExportFormat.WEBP
        "heic" -> ExportFormat.HEIC
        "avif" -> ExportFormat.AVIF
        else -> ExportFormat.JPEG
    }

    /** Resolve a requested format to one we can actually encode on this device. */
    fun resolve(requested: ExportFormat): ExportFormat = when (requested) {
        ExportFormat.HEIC -> if (canEncodeHeic()) ExportFormat.HEIC else ExportFormat.JPEG
        ExportFormat.AVIF -> if (canEncodeAvif()) ExportFormat.AVIF else ExportFormat.JPEG
        else -> requested
    }

    fun usedFallback(requested: ExportFormat): Boolean =
        resolve(requested) != requested
}
