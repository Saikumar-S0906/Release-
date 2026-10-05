package com.pixijoy.movie

/**
 * Lighter preview frames while a finger is down on a long 4K timeline.
 * Same playhead, no mode, and export height is unchanged.
 * The lighter frame is not labeled.
 */
object ProxyScrub {
    const val LONG_4K_MS: Long = 30L * 60L * 1000L
    const val LIGHTER_HEIGHT: Int = 540

    fun applies(durationMs: Long, outputHeight: Int): Boolean =
        durationMs >= LONG_4K_MS && MovieExportSize.normalize(outputHeight) == 2160

    fun previewHeight(durationMs: Long, outputHeight: Int, fingerDown: Boolean): Int {
        val exportHeight = MovieExportSize.normalize(outputHeight)
        return if (fingerDown && applies(durationMs, exportHeight)) {
            LIGHTER_HEIGHT
        } else {
            exportHeight
        }
    }

    /** Scrubbing stays on only while the finger is down on a timeline that still qualifies. */
    fun scrubbingEnabled(fingerDown: Boolean, durationMs: Long, outputHeight: Int): Boolean =
        fingerDown && applies(durationMs, outputHeight)

    /**
     * Position to restore after a rebind. The end is included so a finger-up
     * there does not jump the picture back before the total duration.
     */
    fun restorePositionMs(positionMs: Long, endMs: Long): Long? =
        positionMs.takeIf { it in 1..endMs }
}
