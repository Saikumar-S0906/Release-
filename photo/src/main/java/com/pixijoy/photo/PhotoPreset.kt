package com.pixijoy.photo

/**
 * C6 — named still-edit presets (serialized layer stack, local files).
 * Batch export uses WorkManager over project ids; presets never leave the device.
 */
data class PhotoPreset(
    val id: String,
    val name: String,
    val layersEncoded: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun encode(): String = buildString {
        appendLine("PR,1,${id.encodePayload()},${name.encodePayload()},$updatedAt")
        append(layersEncoded.trim())
    }

    companion object {
        fun decode(raw: String): PhotoPreset? {
            val lines = raw.lineSequence().toList()
            val header = lines.firstOrNull() ?: return null
            if (!header.startsWith("PR,")) return null
            val parts = header.split(',')
            if (parts.size < 5) return null
            return PhotoPreset(
                id = parts[2].decodePayload(),
                name = parts[3].decodePayload(),
                layersEncoded = lines.drop(1).joinToString("\n"),
                updatedAt = parts[4].toLongOrNull() ?: 0L
            )
        }
    }
}
