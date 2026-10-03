package com.pixijoy.movie

import android.os.Build
import androidx.media3.transformer.Composition

/**
 * Movie HDR export intent (D11) — beyond HD/FHD/4K SDR.
 *
 * [Sdr] tone-maps HDR inputs to SDR (OpenGL) for wide sharing.
 * [KeepHdr] preserves HDR when the device can write it (HEVC, not H.264).
 * Otherwise the export is SDR and the UI says so.
 */
enum class MovieHdrMode {
    Sdr,
    KeepHdr;

    fun encode(): String = when (this) {
        Sdr -> "sdr"
        KeepHdr -> "keep"
    }

    fun isDefault(): Boolean = this == Sdr

    /** Media3 [Composition.HdrMode] constant. */
    fun toCompositionHdrMode(): Int = when (this) {
        Sdr -> Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
        KeepHdr -> Composition.HDR_MODE_KEEP_HDR
    }

    /** Apply Ultra HDR gain maps when keeping HDR (API 34+; ignored earlier). */
    fun retainUltraHdrGainMap(): Boolean = this == KeepHdr && Build.VERSION.SDK_INT >= 34

    companion object {
        fun parse(raw: String?): MovieHdrMode =
            when (raw?.lowercase()) {
                "keep", "hdr", "keephdr" -> KeepHdr
                else -> Sdr
            }

        /** Prefer Keep HDR on devices that typically support HDR editing (API 33+). */
        fun preferred(): MovieHdrMode =
            if (Build.VERSION.SDK_INT >= 33) KeepHdr else Sdr
    }
}
