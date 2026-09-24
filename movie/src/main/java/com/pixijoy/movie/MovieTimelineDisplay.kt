package com.pixijoy.movie

import kotlin.math.ceil

object MovieTimelineDisplay {
    fun formatTimecode(timeMs: Long): String {
        val totalTenths = timeMs.coerceAtLeast(0) / 100
        val tenths = totalTenths % 10
        val totalSeconds = totalTenths / 10
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val hours = totalMinutes / 60
        return if (hours > 0) {
            "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}.$tenths"
        } else {
            "$totalMinutes:${seconds.twoDigits()}.$tenths"
        }
    }

    fun clipLabel(index: Int, durationMs: Long): String =
        "Clip ${index.coerceAtLeast(0) + 1} · ${formatTimecode(durationMs)}"

    fun clipWidthDp(
        durationMs: Long,
        dpPerSecond: Float = 44f,
        minDp: Float = 104f,
        maxDp: Float = 300f
    ): Float = (durationMs.coerceAtLeast(1) / 1000f * dpPerSecond).coerceIn(minDp, maxDp)

    fun thumbnailCount(widthDp: Float): Int =
        ceil(widthDp.coerceAtLeast(1f) / 64f).toInt().coerceIn(2, 8)

    private fun Long.twoDigits(): String = if (this < 10) "0$this" else toString()
}
