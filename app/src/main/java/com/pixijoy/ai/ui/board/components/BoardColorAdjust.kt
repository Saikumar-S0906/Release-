package com.pixijoy.ai.ui.board.components

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter

/**
 * Maps Beautiful / Editor sliders (0..20, neutral 10) into a Compose [ColorMatrix].
 * Includes HSL + curves extras for Flow 15.
 */
object BoardColorAdjust {
    const val Neutral = 10f

    fun matrix(
        brightness: Float = Neutral,
        contrast: Float = Neutral,
        saturation: Float = Neutral,
        warmth: Float = Neutral,
        hue: Float = Neutral,
        lightness: Float = Neutral,
        shadows: Float = Neutral,
        midtones: Float = Neutral,
        highlights: Float = Neutral,
        boostLighting: Boolean = false,
        boostColor: Boolean = false,
        softenNoise: Boolean = false
    ): ColorMatrix {
        var b = brightness
        var c = contrast
        var s = saturation
        if (boostLighting) b += 2f
        if (boostColor) s += 2f
        if (softenNoise) c -= 1.5f

        // Curves: shadows/midtones/highlights fold into contrast + brightness bias
        val shadowBias = ((shadows - Neutral) / Neutral) * 18f
        val midBias = ((midtones - Neutral) / Neutral) * 22f
        val hiBias = ((highlights - Neutral) / Neutral) * 16f
        b += midBias * 0.35f + hiBias * 0.2f
        c += ((shadows - Neutral) / Neutral) * 2f + ((midtones - Neutral) / Neutral) * 1.5f

        val sat = (s / Neutral).coerceIn(0f, 2f)
        val contrastScale = (1f + (c - Neutral) / Neutral).coerceIn(0.25f, 2f)
        val bright = ((b - Neutral) / Neutral) * 40f + shadowBias * 0.4f
        val light = ((lightness - Neutral) / Neutral) * 36f
        val warm = ((warmth - Neutral) / Neutral).coerceIn(-1f, 1f)
        val hueDeg = ((hue - Neutral) / Neutral) * 180f
        val translate = ((1f - contrastScale) / 2f) * 255f

        val out = ColorMatrix()
        out.setToSaturation(sat)
        out.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    contrastScale, 0f, 0f, 0f, translate,
                    0f, contrastScale, 0f, 0f, translate,
                    0f, 0f, contrastScale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        out.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, bright + light,
                    0f, 1f, 0f, 0f, bright + light,
                    0f, 0f, 1f, 0f, bright + light,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        out.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    1f + warm * 0.18f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f - warm * 0.18f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        if (kotlin.math.abs(hueDeg) > 0.5f) {
            out.timesAssign(hueRotateMatrix(hueDeg))
        }
        // Mild highlight lift via white point nudge
        if (kotlin.math.abs(hiBias) > 0.5f) {
            val g = 1f + hiBias / 255f
            out.timesAssign(
                ColorMatrix(
                    floatArrayOf(
                        g, 0f, 0f, 0f, 0f,
                        0f, g, 0f, 0f, 0f,
                        0f, 0f, g, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        return out
    }

    /** Approximate RGB hue rotation (degrees). */
    private fun hueRotateMatrix(degrees: Float): ColorMatrix {
        val rad = Math.toRadians(degrees.toDouble())
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        return ColorMatrix(
            floatArrayOf(
                lumR + cos * (1 - lumR) + sin * (-lumR),
                lumG + cos * (-lumG) + sin * (-lumG),
                lumB + cos * (-lumB) + sin * (1 - lumB),
                0f, 0f,
                lumR + cos * (-lumR) + sin * 0.143f,
                lumG + cos * (1 - lumG) + sin * 0.140f,
                lumB + cos * (-lumB) + sin * (-0.283f),
                0f, 0f,
                lumR + cos * (-lumR) + sin * (-(1 - lumR)),
                lumG + cos * (-lumG) + sin * lumG,
                lumB + cos * (1 - lumB) + sin * lumB,
                0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun colorFilter(
        brightness: Float = Neutral,
        contrast: Float = Neutral,
        saturation: Float = Neutral,
        warmth: Float = Neutral,
        hue: Float = Neutral,
        lightness: Float = Neutral,
        shadows: Float = Neutral,
        midtones: Float = Neutral,
        highlights: Float = Neutral,
        boostLighting: Boolean = false,
        boostColor: Boolean = false,
        softenNoise: Boolean = false
    ): ColorFilter = ColorMatrixColorFilter(
        matrix(
            brightness, contrast, saturation, warmth,
            hue, lightness, shadows, midtones, highlights,
            boostLighting, boostColor, softenNoise
        )
    )

    /**
     * Live Compose ColorFilter for look presets 0..7
     * (NONE / VIVID / WARM / COOL / BW / VINTAGE / CINEMATIC / AI_GLOW),
     * plus category packs: 0 Filters · 1 Effects · 2 Color · 3 Artistic.
     */
    fun lookColorFilter(filterIndex: Int, category: Int = 0): ColorFilter? {
        val i = filterIndex.coerceIn(0, 7)
        val cat = category.coerceIn(0, 3)
        if (i == 0 && cat == 0) return null
        return ColorMatrixColorFilter(ColorMatrix(lookAndroidMatrix(i, cat).array))
    }

    /** Bake the same look into a bitmap via [android.graphics.ColorMatrix]. */
    fun lookAndroidMatrix(filterIndex: Int, category: Int = 0): android.graphics.ColorMatrix {
        val i = filterIndex.coerceIn(0, 7)
        val cat = category.coerceIn(0, 3)
        val base = when (i) {
            0 -> android.graphics.ColorMatrix()
            1 -> android.graphics.ColorMatrix().also { it.setSaturation(1.45f) }
            2 -> android.graphics.ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, 0f, 0f, 12f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.88f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            3 -> android.graphics.ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, -6f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 1.15f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            4 -> android.graphics.ColorMatrix().also { it.setSaturation(0f) }
            5 -> android.graphics.ColorMatrix(
                floatArrayOf(
                    0.95f, 0.05f, 0f, 0f, 18f,
                    0.05f, 0.9f, 0.05f, 0f, 8f,
                    0f, 0.1f, 0.75f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            6 -> android.graphics.ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0f, 0f, -8f,
                    0f, 0.95f, 0f, 0f, -4f,
                    0f, 0f, 0.85f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            else -> android.graphics.ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, 10f,
                    0f, 1.1f, 0f, 0f, 8f,
                    0f, 0f, 1.2f, 0f, 16f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        if (cat > 0) base.postConcat(categoryMatrix(cat, i))
        return base
    }

    /** 0=80s neon · 1=90s · 2=2000s flash · 3=film · 4=now (gentle enhance). */
    fun eraAndroidMatrix(era: Int): android.graphics.ColorMatrix = when (era.coerceIn(0, 4)) {
        0 -> android.graphics.ColorMatrix(
            floatArrayOf(
                1.25f, 0.05f, 0.15f, 0f, 18f,
                0f, 0.85f, 0.08f, 0f, -6f,
                0.12f, 0f, 1.35f, 0f, 22f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        1 -> android.graphics.ColorMatrix(
            floatArrayOf(
                0.92f, 0.08f, 0f, 0f, 8f,
                0.05f, 0.95f, 0.08f, 0f, 4f,
                0f, 0.12f, 1.18f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        2 -> android.graphics.ColorMatrix(
            floatArrayOf(
                1.22f, 0.04f, 0f, 0f, 20f,
                0.04f, 1.08f, 0f, 0f, 10f,
                0f, 0.02f, 0.9f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        3 -> android.graphics.ColorMatrix(
            floatArrayOf(
                1.08f, 0.04f, 0f, 0f, -6f,
                0.04f, 0.98f, 0.02f, 0f, -2f,
                0f, 0.06f, 0.82f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        else -> android.graphics.ColorMatrix(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, 8f,
                0f, 1.08f, 0f, 0f, 6f,
                0f, 0f, 1.06f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun eraColorFilter(era: Int): ColorFilter =
        ColorMatrixColorFilter(ColorMatrix(eraAndroidMatrix(era).array))

    private fun categoryMatrix(category: Int, index: Int): android.graphics.ColorMatrix {
        val punch = 1f + (index % 8) * 0.04f
        return when (category) {
            1 -> android.graphics.ColorMatrix(
                floatArrayOf(
                    punch, 0f, 0f, 0f, 10f,
                    0f, punch, 0f, 0f, 8f,
                    0f, 0f, punch * 0.95f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            2 -> android.graphics.ColorMatrix(
                floatArrayOf(
                    1.05f + index * 0.03f, 0f, 0.04f, 0f, 6f,
                    0.02f, 1.02f, 0.04f, 0f, 2f,
                    0f, 0.03f, 1.12f - index * 0.02f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            else -> android.graphics.ColorMatrix(
                floatArrayOf(
                    1.18f, 0.08f, 0.04f, 0f, 12f,
                    0.06f, 1.02f, 0.06f, 0f, 4f,
                    0.04f, 0.08f, 0.88f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }

    /** Android [android.graphics.ColorMatrix] for baking preview grades into gallery bitmaps. */
    fun androidMatrix(
        brightness: Float = Neutral,
        contrast: Float = Neutral,
        saturation: Float = Neutral,
        warmth: Float = Neutral,
        hue: Float = Neutral,
        lightness: Float = Neutral,
        shadows: Float = Neutral,
        midtones: Float = Neutral,
        highlights: Float = Neutral,
        boostLighting: Boolean = false,
        boostColor: Boolean = false,
        softenNoise: Boolean = false
    ): android.graphics.ColorMatrix =
        android.graphics.ColorMatrix(
            matrix(
                brightness, contrast, saturation, warmth,
                hue, lightness, shadows, midtones, highlights,
                boostLighting, boostColor, softenNoise
            ).values
        )
}
