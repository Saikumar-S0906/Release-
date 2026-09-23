package com.pixijoy.photo

import java.io.File

/**
 * Imported LUT keys in the edit graph: `lut:<safeFileName>`.
 * Files live under `{projectDir}/luts/<safeFileName>`.
 */
object ImportedLuts {
    const val PREFIX = "lut:"

    fun isImported(key: String): Boolean = key.startsWith(PREFIX)

    fun fileName(key: String): String = key.removePrefix(PREFIX)

    fun keyFor(fileName: String): String = PREFIX + sanitizeFileName(fileName)

    fun resolve(projectDir: File?, key: String): File? {
        if (projectDir == null || !isImported(key)) return null
        val name = fileName(key)
        if (name.isBlank() || name != sanitizeFileName(name)) return null
        val file = File(File(projectDir, "luts"), name)
        return file.takeIf { it.isFile }
    }

    fun sanitizeFileName(raw: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = base.map { ch ->
            when {
                ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-' -> ch
                else -> '_'
            }
        }.joinToString("")
        val lower = cleaned.lowercase()
        val withExt = when {
            lower.endsWith(".cube") || lower.endsWith(".3dl") -> cleaned
            else -> "$cleaned.cube"
        }
        return withExt.take(120).ifBlank { "import.cube" }
    }
}
