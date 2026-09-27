package com.pixijoy.feature.ffmpeg

import com.pixijoy.movie.FfmpegExportBackend
import com.pixijoy.movie.MovieExportBackends
import java.io.File

/**
 * F5 FFmpeg bridge. Registers into [MovieExportBackends] when this split loads.
 *
 * `com.arthenica:ffmpeg-kit-min` was removed from Maven Central. Until a pinned
 * AAR / fork is re-hosted on Release-, [isAvailable] stays false and Media3
 * remains the only export path (GIF/WebM gaps stay documented).
 */
object FfmpegFeatureEntry {
    @JvmStatic
    fun install() {
        MovieExportBackends.ffmpeg = UnavailableFfmpegBackend
    }
}

object UnavailableFfmpegBackend : FfmpegExportBackend {
    override fun isAvailable(): Boolean = false

    override fun transcodeGap(input: File, output: File, format: String): Boolean = false
}
