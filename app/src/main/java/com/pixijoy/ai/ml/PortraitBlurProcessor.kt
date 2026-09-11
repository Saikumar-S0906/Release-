package com.pixijoy.ai.ml

import android.graphics.Bitmap
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortraitBlurProcessor @Inject constructor(
    private val backgroundRemover: BackgroundRemover
) {
    suspend fun applyBlur(bitmap: Bitmap, radius: Float = 18f): Bitmap {
        val mask = backgroundRemover.extractMask(bitmap)
        return BitmapUtils.blurRegion(bitmap, mask, radius).also {
            mask.recycle()
        }
    }
}
