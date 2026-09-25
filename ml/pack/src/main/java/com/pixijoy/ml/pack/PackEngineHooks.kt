package com.pixijoy.ml.pack

import android.graphics.Bitmap

/**
 * Real neural runners register here. The base APK leaves every hook null so
 * [PackRuntime.engineReady] stays false until a bound interpreter exists.
 * Telea / bicubic / frame-hold must never register as neural.
 */
object PackEngineHooks {
    @Volatile
    var inpaint: ((Bitmap, BooleanArray) -> Bitmap?)? = null

    @Volatile
    var superRes: ((Bitmap, Int) -> Bitmap?)? = null

    @Volatile
    var denoise: ((Bitmap, Float) -> Bitmap?)? = null

    @Volatile
    var frameInterpBound: Boolean = false

    @Volatile
    var trackBound: Boolean = false

    fun hasEngine(id: PackId): Boolean = when (id) {
        PackId.Inpaint -> inpaint != null
        PackId.SuperRes -> superRes != null
        PackId.Denoise -> denoise != null
        PackId.FrameInterp -> frameInterpBound
        PackId.Track -> trackBound
    }

    fun resetForTests() {
        inpaint = null
        superRes = null
        denoise = null
        frameInterpBound = false
        trackBound = false
    }
}
