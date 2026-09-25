package com.pixijoy.ml

import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat

/** Play Feature Delivery install hook. Call from [android.app.Application.attachBaseContext]. */
object PackSplits {
    fun install(context: Context) {
        runCatching { SplitCompat.install(context) }
    }
}
