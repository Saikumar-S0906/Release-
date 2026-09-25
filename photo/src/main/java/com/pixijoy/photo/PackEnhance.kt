package com.pixijoy.photo

import android.graphics.Bitmap
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.pack.PackId

/**
 * E1–E3 pack-backed photo engines. Missing pack or unbound runner → identity / null.
 * Never substitutes Telea, bicubic, or blur as "neural".
 */
object PackEnhance {
    fun inpaintAvailable(): Boolean = PackEngineHooks.hasEngine(PackId.Inpaint)

    fun superResAvailable(): Boolean = PackEngineHooks.hasEngine(PackId.SuperRes)

    fun denoiseAvailable(): Boolean = PackEngineHooks.hasEngine(PackId.Denoise)

    fun tryInpaint(src: Bitmap, mask: BooleanArray): Bitmap? {
        if (!inpaintAvailable()) return null
        return PackEngineHooks.tryInpaint(src, mask)
    }
}

fun applySuperRes(src: Bitmap, scale: Int): Bitmap {
    val s = if (scale >= 4) 4 else 2
    if (!PackEnhance.superResAvailable()) return src
    return PackEngineHooks.trySuperRes(src, s) ?: src
}

fun applyDenoise(src: Bitmap, amount: Float): Bitmap {
    if (amount <= 0.01f || !PackEnhance.denoiseAvailable()) return src
    return PackEngineHooks.tryDenoise(src, amount.coerceIn(0f, 1f)) ?: src
}
