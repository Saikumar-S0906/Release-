package com.pixijoy.movie

/**
 * D2 — transition applied at the cut *after* this clip into the next.
 * Soft = sequential fade out / fade in (dissolve). True optical overlap is D7/PiP territory.
 */
enum class ClipTransition {
    None,
    Soft;

    companion object {
        fun parse(raw: String?): ClipTransition =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: None
    }
}
