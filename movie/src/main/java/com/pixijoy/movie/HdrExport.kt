package com.pixijoy.movie

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import androidx.media3.common.MimeTypes

/** How a movie export writes HDR. Keep HDR never uses H.264. */
data class HdrExportChoice(
    val mode: MovieHdrMode,
    val videoMimeType: String,
    val fellBackToSdr: Boolean
)

data class MovieExportFile(
    val uri: String,
    val hdrFellBackToSdr: Boolean = false
)

fun chooseHdrExport(requested: MovieHdrMode, deviceCanWriteHdr: Boolean): HdrExportChoice {
    if (requested == MovieHdrMode.KeepHdr && deviceCanWriteHdr) {
        return HdrExportChoice(
            mode = MovieHdrMode.KeepHdr,
            videoMimeType = MimeTypes.VIDEO_H265,
            fellBackToSdr = false
        )
    }
    return HdrExportChoice(
        mode = MovieHdrMode.Sdr,
        videoMimeType = MimeTypes.VIDEO_H264,
        fellBackToSdr = requested == MovieHdrMode.KeepHdr
    )
}

object HdrEncoderSupport {
    /** True only when an HEVC encoder on this device advertises HDR. */
    fun canWriteHdr(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        return try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                if (!info.isEncoder) return@any false
                val hevc = info.supportedTypes.any { it.equals(MimeTypes.VIDEO_H265, ignoreCase = true) }
                if (!hevc) return@any false
                val caps = info.getCapabilitiesForType(MimeTypes.VIDEO_H265)
                caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing) ||
                    caps.profileLevels.any {
                        it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
                    }
            }
        } catch (_: Throwable) {
            false
        }
    }
}
