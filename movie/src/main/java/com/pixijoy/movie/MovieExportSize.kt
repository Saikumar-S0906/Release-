package com.pixijoy.movie

/** HD / FHD / 4K export heights for movie timeline + Ken Burns / Transformer. */
object MovieExportSize {
    val heights: List<Int> = listOf(720, 1080, 2160)

    fun normalize(height: Int): Int = when {
        height >= 1800 -> 2160
        height >= 900 -> 1080
        else -> 720
    }

    fun width(height: Int): Int = when (normalize(height)) {
        2160 -> 3840
        1080 -> 1920
        else -> 1280
    }

    fun bitrate(height: Int): Int = when (normalize(height)) {
        2160 -> 16_000_000
        1080 -> 8_000_000
        else -> 4_000_000
    }

    fun label(height: Int): String = when (normalize(height)) {
        2160 -> "4K"
        1080 -> "FHD"
        else -> "HD"
    }
}