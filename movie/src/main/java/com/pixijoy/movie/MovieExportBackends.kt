package com.pixijoy.movie

import java.io.File

/**
 * F5 optional FFmpeg export. Default path is always Media3 Transformer.
 * [FfmpegExportBackend] is installed only from `:feature_ffmpeg` (outside base APK).
 *
 * Documented Media3 gaps this backend may cover when the split is present:
 * - Animated GIF encode from a composition
 * - WebM/VP9 encode when the device encoder path is missing
 * - Edge remux cases Media3 refuses
 */
interface FfmpegExportBackend {
    fun isAvailable(): Boolean
    fun transcodeGap(input: File, output: File, format: String): Boolean
}

object MovieExportBackends {
    @Volatile
    var ffmpeg: FfmpegExportBackend? = null

    fun ffmpegAvailable(): Boolean = ffmpeg?.isAvailable() == true

    fun tryGapExport(input: File, output: File, format: String): Boolean =
        ffmpeg?.transcodeGap(input, output, format) == true

    fun tryBindFfmpeg() {
        runCatching {
            Class.forName("com.pixijoy.feature.ffmpeg.FfmpegFeatureEntry")
                .getMethod("install")
                .invoke(null)
        }
    }
}
