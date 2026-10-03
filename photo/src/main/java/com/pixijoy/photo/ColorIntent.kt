package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build

/**
 * C7 — export / decode color intent (local-first).
 * Display P3 when the device exposes it; otherwise sRGB. No network profiles.
 */
enum class ColorIntent {
    Srgb,
    DisplayP3;

    companion object {
        fun preferred(): ColorIntent =
            if (Build.VERSION.SDK_INT >= 26 &&
            runCatching { ColorSpace.get(ColorSpace.Named.DISPLAY_P3) }.getOrNull() != null
        ) {
                DisplayP3
            } else {
                Srgb
            }
    }
}

fun Bitmap.withColorIntent(intent: ColorIntent): Bitmap {
    if (Build.VERSION.SDK_INT < 26) return this
    val space = when (intent) {
        ColorIntent.Srgb -> ColorSpace.get(ColorSpace.Named.SRGB)
        ColorIntent.DisplayP3 -> ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
    } ?: return this
    if (colorSpace == space) return this
    val copy = copy(config ?: Bitmap.Config.ARGB_8888, true)
    return try {
        copy.setColorSpace(space)
        copy
    } catch (t: Throwable) {
        if (!copy.isRecycled) copy.recycle()
        throw t
    }
}
