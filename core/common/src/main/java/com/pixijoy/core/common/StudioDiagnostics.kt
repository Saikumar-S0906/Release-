package com.pixijoy.core.common

import java.io.File

/**
 * On-device ring of the last studio failures. No network, no SDK.
 * Lines are short and stripped of paths so a later session can show what failed
 * without user media, image bytes, or file locations.
 */
object StudioDiagnostics {
    private const val CAP = 24
    private const val MAX_CHARS = 160
    private const val FILE_NAME = "studio-diagnostics.log"
    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private var store: File? = null

    private val pathLike = Regex(
        """(?i)(?:[a-z]:\\[^\s]+|content://\S+|file://\S+|/(?:[\w.\-]+/)+[^\s]+)"""
    )

    /** Load the last lines from app storage. Safe to call once at process start. */
    fun bind(directory: File) {
        synchronized(lock) {
            val file = File(directory, FILE_NAME)
            store = file
            lines.clear()
            if (!file.isFile) return
            val kept = runCatching { file.readLines() }.getOrDefault(emptyList())
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .takeLast(CAP)
                .map { it.take(MAX_CHARS) }
            lines.addAll(kept)
        }
    }

    fun note(kind: String, detail: String) {
        val safeKind = kind.trim().lowercase().ifBlank { "studio" }.take(24)
        val line = "$safeKind: ${sanitize(detail)}"
        synchronized(lock) {
            lines.addLast(line)
            while (lines.size > CAP) lines.removeFirst()
            val file = store ?: return
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(lines.joinToString("\n"))
            }
        }
    }

    fun latest(): String? = synchronized(lock) { lines.lastOrNull() }

    fun snapshot(): List<String> = synchronized(lock) { lines.toList() }

    /** Test-only. Drops memory and the file binding so cases do not leak. */
    fun clearForTest() {
        synchronized(lock) {
            lines.clear()
            store = null
        }
    }

    internal fun sanitize(raw: String): String {
        val oneLine = raw.replace(Regex("\\s+"), " ").trim()
        if (oneLine.isEmpty()) return "failed"
        return pathLike.replace(oneLine, "[path]").take(MAX_CHARS)
    }
}