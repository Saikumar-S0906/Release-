package com.pixijoy.photo

import android.graphics.ColorMatrix

/** Bundled Looks (ColorMatrix LUTs). No file picker this slice. */
object BundledLooks {
    const val ORIGINAL = "original"
    const val CLEAN = "clean"
    const val WARM = "warm"
    const val COOL = "cool"
    const val FILM = "film"
    const val NIGHT = "night"

    val keys: List<String> = listOf(ORIGINAL, CLEAN, WARM, COOL, FILM, NIGHT)

    fun isIdentity(key: String): Boolean =
        key.isBlank() || key.equals(ORIGINAL, ignoreCase = true)

    fun colorMatrix(key: String): FloatArray = when (key.lowercase()) {
        CLEAN -> floatArrayOf(
            1.08f, -0.02f, -0.02f, 0f, 6f,
            -0.02f, 1.08f, -0.02f, 0f, 6f,
            -0.02f, -0.02f, 1.05f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        )
        WARM -> floatArrayOf(
            1.12f, 0.04f, 0f, 0f, 8f,
            0.02f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.92f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f
        )
        COOL -> floatArrayOf(
            0.92f, 0f, 0.04f, 0f, -4f,
            0f, 1.02f, 0.04f, 0f, 2f,
            0.04f, 0.04f, 1.14f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
        FILM -> floatArrayOf(
            1.05f, 0.06f, 0.02f, 0f, 4f,
            0.02f, 1.00f, 0.04f, 0f, 2f,
            0.04f, 0.08f, 0.95f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f
        )
        NIGHT -> floatArrayOf(
            0.78f, 0.02f, 0.08f, 0f, -12f,
            0f, 0.82f, 0.10f, 0f, -8f,
            0.06f, 0.04f, 1.10f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )
        else -> floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun androidMatrix(key: String): ColorMatrix = ColorMatrix(colorMatrix(key))
}