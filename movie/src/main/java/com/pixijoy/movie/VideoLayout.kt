package com.pixijoy.movie

import androidx.media3.common.util.Size
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.VideoCompositorSettings

/**
 * Multi-video frame layouts (D7). [Single] keeps the existing sequential composition.
 * Other layouts run parallel video sequences through [VideoCompositorSettings].
 */
enum class VideoLayout {
    Single,
    Pip,
    Split,
    Grid;

    val videoInputs: Int
        get() = when (this) {
            Single -> 1
            Pip, Split -> 2
            Grid -> 4
        }

    fun isMulti(): Boolean = this != Single

    fun encode(): String = name

    fun compositor(): VideoCompositorSettings = when (this) {
        Single -> VideoCompositorSettings.DEFAULT
        Pip -> PipCompositor
        Split -> SplitCompositor
        Grid -> GridCompositor
    }

    companion object {
        fun parse(raw: String?): VideoLayout =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Single
    }
}

/** Input 0 = PiP inset (top-right); input 1 = full-frame background. */
private object PipCompositor : VideoCompositorSettings {
    override fun getOutputSize(inputSizes: List<Size>): Size =
        inputSizes.getOrNull(1) ?: inputSizes.firstOrNull() ?: Size(1280, 720)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings =
        when (inputId) {
            0 -> OverlaySettings.Builder()
                .setScale(0.34f, 0.34f)
                .setBackgroundFrameAnchor(0.62f, 0.62f)
                .build()
            else -> OverlaySettings.Builder().build()
        }
}

/** Side-by-side: input 0 left, input 1 right. */
private object SplitCompositor : VideoCompositorSettings {
    override fun getOutputSize(inputSizes: List<Size>): Size =
        inputSizes.firstOrNull() ?: Size(1280, 720)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings =
        when (inputId) {
            0 -> OverlaySettings.Builder()
                .setScale(0.5f, 1f)
                .setBackgroundFrameAnchor(-0.5f, 0f)
                .build()
            1 -> OverlaySettings.Builder()
                .setScale(0.5f, 1f)
                .setBackgroundFrameAnchor(0.5f, 0f)
                .build()
            else -> OverlaySettings.Builder().build()
        }
}

/** 2×2 grid for up to four inputs. */
private object GridCompositor : VideoCompositorSettings {
    override fun getOutputSize(inputSizes: List<Size>): Size =
        inputSizes.firstOrNull() ?: Size(1280, 720)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings =
        when (inputId) {
            0 -> OverlaySettings.Builder()
                .setScale(0.5f, 0.5f)
                .setBackgroundFrameAnchor(-0.5f, 0.5f)
                .build()
            1 -> OverlaySettings.Builder()
                .setScale(0.5f, 0.5f)
                .setBackgroundFrameAnchor(0.5f, 0.5f)
                .build()
            2 -> OverlaySettings.Builder()
                .setScale(0.5f, 0.5f)
                .setBackgroundFrameAnchor(-0.5f, -0.5f)
                .build()
            3 -> OverlaySettings.Builder()
                .setScale(0.5f, 0.5f)
                .setBackgroundFrameAnchor(0.5f, -0.5f)
                .build()
            else -> OverlaySettings.Builder().build()
        }
}

object VideoLayoutPlan {
    /** Paths used as parallel video inputs (primary first). */
    fun inputPaths(paths: List<String>, layout: VideoLayout): List<String> {
        if (paths.isEmpty()) return emptyList()
        val n = layout.videoInputs
        if (n <= 1) return listOf(paths.first())
        return (0 until n).map { i -> paths.getOrElse(i) { paths[i % paths.size] } }
    }
}
