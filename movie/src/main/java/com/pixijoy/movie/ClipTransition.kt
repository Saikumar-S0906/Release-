package com.pixijoy.movie

/**
 * D2 — transition applied at the cut *after* this clip into the next.
 * Soft fades the picture through black and fades the audio, across [TimelineClip.transitionMs].
 * The two shots are not optically overlapped.
 */
enum class ClipTransition {
    None,
    Soft;

    companion object {
        fun parse(raw: String?): ClipTransition =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: None
    }
}
