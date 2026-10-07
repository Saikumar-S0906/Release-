package com.pixijoy.ml.pack

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Device tier for ML packs. Flagship phones (≥ 8 GB RAM, 64-bit, Android 12+) automatically get
 * the HQ pack files (512² tensors: depth @518, MI-GAN 512, MODNet 512). Every other phone gets the
 * standard 256² files. The [PackId] stays the same, so the UI and engines do not change.
 */
object PackTier {
    const val HQ_MIN_RAM_BYTES = 7_500_000_000L // marketed "8 GB" phones report ~7.5 GB

    @Volatile
    var hq: Boolean = false
        private set

    fun detect(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        hq = isFlagship(info.totalMem, Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(), Build.VERSION.SDK_INT, am.isLowRamDevice)
    }

    fun isFlagship(totalMemBytes: Long, is64Bit: Boolean, sdk: Int, lowRam: Boolean): Boolean =
        !lowRam && is64Bit && sdk >= 31 && totalMemBytes >= HQ_MIN_RAM_BYTES

    /** Test hook. */
    fun overrideForTest(value: Boolean) {
        hq = value
    }
}
