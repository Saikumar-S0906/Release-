package com.pixijoy.ai.ui.board

import com.pixijoy.ai.BuildConfig

/**
 * Phase 7 board feature switches.
 *
 * Live generate stays **off** unless [BuildConfig.BOARD_LIVE_GENERATE] is true **and**
 * a real Gemini key is present. Board hosts always keep a soft drawable fallback.
 */
object BoardFeatureFlags {
    /** Product intent: attempt live Beautiful/Funny when key is configured. */
    val liveGenerateRequested: Boolean
        get() = BuildConfig.BOARD_LIVE_GENERATE

    val geminiKeyConfigured: Boolean
        get() {
            val key = BuildConfig.GEMINI_API_KEY.trim()
            return key.isNotEmpty() &&
                key != "your_gemini_api_key_here" &&
                !key.startsWith("dummy", ignoreCase = true)
        }

    /** Flag + key — safe to hide soft Demo captions and shorten soft delay. */
    val liveGenerateConfigured: Boolean
        get() = liveGenerateRequested && geminiKeyConfigured

    /** Soft preview captions under generate CTAs (Bible honesty until live default). */
    val showDemoCaption: Boolean
        get() = !liveGenerateConfigured

    /**
     * Offline / credits / safety chips on processing screens.
     * Keep **off** in product — SoftEdge overlays are need-based (real offline/credits/safety)
     * or catalog QA deep-links, never static chrome on every generate.
     */
    val showSoftEdgeDemoChips: Boolean
        get() = false

    /** Discover/Create/Stickers may resolve CDN keys; drawables always backstop. */
    val cdnEnabled: Boolean
        get() = BuildConfig.BOARD_CDN_ENABLED
}
