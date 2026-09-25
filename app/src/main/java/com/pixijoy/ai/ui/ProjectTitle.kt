package com.pixijoy.ai.ui

/**
 * Names a project at create time. A real source name wins. Camera stamps and
 * generic labels fall through to "Photo · Sep 25" style defaults.
 */
internal object ProjectTitle {
    const val MAX_LENGTH = 48

    private val generic = setOf(
        "sample",
        "incoming",
        "image",
        "video",
        "photo",
        "movie",
        "img",
        "mov",
        "clip",
        "capture",
        "untitled",
        "pixijoy"
    )
    private val captureStamp = Regex("^capture-\\d+$", RegexOption.IGNORE_CASE)
    private val unsafe = Regex("""[\\/:*?"<>|]""")

    fun resolve(sourceName: String?, kindLabel: String, dateLabel: String): String =
        fromSourceName(sourceName) ?: dated(kindLabel, dateLabel)

    fun fromSourceName(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val stem = trimmed.substringBeforeLast('.').trim().ifEmpty { trimmed }
        val collapsed = stem
            .replace(unsafe, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (collapsed.isEmpty()) return null
        if (generic.contains(collapsed.lowercase())) return null
        if (captureStamp.matches(collapsed)) return null
        return collapsed.take(MAX_LENGTH)
    }

    fun dated(kindLabel: String, dateLabel: String): String {
        val kind = kindLabel.trim().ifEmpty { "Project" }
        val date = dateLabel.trim()
        return if (date.isEmpty()) kind else "$kind · $date"
    }
}
