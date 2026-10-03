package com.pixijoy.movie

import android.content.Context
import android.util.Log
import com.pixijoy.core.common.StudioDiagnostics
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.transformer.CompositionPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CompositionPreviewGraphMode {
    SingleInput,
    MultipleInput
}

enum class CompositionPreviewCapability {
    LiveSingle,
    LiveWysiwyg,
    ExportOnly
}

enum class CompositionPreviewSeekResult {
    Applied,
    Unsupported,
    Unavailable,
    Failed
}

data class CompositionPreviewResult(
    val capability: CompositionPreviewCapability,
    val detail: String? = null
)

data class CompositionPreviewState(
    val player: Player? = null,
    val result: CompositionPreviewResult = CompositionPreviewResult(
        capability = CompositionPreviewCapability.ExportOnly,
        detail = "Preview has not been prepared."
    )
)

object CompositionPreviewPolicy {
    fun graphMode(layout: VideoLayout): CompositionPreviewGraphMode =
        if (layout.videoInputs > 1) {
            CompositionPreviewGraphMode.MultipleInput
        } else {
            CompositionPreviewGraphMode.SingleInput
        }

    fun supportsSeeking(mode: CompositionPreviewGraphMode): Boolean =
        mode == CompositionPreviewGraphMode.SingleInput
}

class CompositionPreview(
    context: Context,
    private val engine: MovieEngine
) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(CompositionPreviewState())
    val state: StateFlow<CompositionPreviewState> = _state.asStateFlow()

    val player: Player?
        get() = _state.value.player

    private var compositionPlayer: CompositionPlayer? = null
    private var graphMode: CompositionPreviewGraphMode? = null
    private var released = false
    val mix = PreviewMix()

    fun bind(
        paths: List<String>,
        timeline: Timeline,
        playWhenReady: Boolean
    ): CompositionPreviewResult =
        bind(paths, MultiTrackTimeline.fromFlatTimeline(timeline), playWhenReady)

    fun bind(
        paths: List<String>,
        timeline: MultiTrackTimeline,
        playWhenReady: Boolean
    ): CompositionPreviewResult {
        if (released) {
            return publishExportOnly("Preview has already been released.")
        }

        val targetMode = CompositionPreviewPolicy.graphMode(timeline.layout)
        val oldPlayer = compositionPlayer
        val oldPositionMs = previewValue(0L) { oldPlayer?.currentPosition ?: 0L }
        val resumeWhenReady = previewValue(playWhenReady) {
            oldPlayer?.playWhenReady ?: playWhenReady
        }
        val targetPlayer = if (oldPlayer == null || graphMode != targetMode) {
            releasePlayer(oldPlayer)
            createPlayer(targetMode) ?: return _state.value.result
        } else {
            oldPlayer
        }

        return try {
            // Export passes this same unmodified composition shape to Transformer.
            val composition = engine.composition(paths, timeline, mix)
            targetPlayer.setComposition(composition)
            targetPlayer.repeatMode = Player.REPEAT_MODE_ALL
            targetPlayer.prepare()
            val endMs = timeline.totalMs().coerceAtLeast(1)
            if (CompositionPreviewPolicy.supportsSeeking(targetMode) &&
                oldPositionMs in 1 until endMs
            ) {
                targetPlayer.seekTo(oldPositionMs)
            }
            targetPlayer.playWhenReady = resumeWhenReady
            val result = CompositionPreviewResult(
                capability = if (targetMode == CompositionPreviewGraphMode.MultipleInput) {
                    CompositionPreviewCapability.LiveWysiwyg
                } else {
                    CompositionPreviewCapability.LiveSingle
                },
                detail = if (targetMode == CompositionPreviewGraphMode.MultipleInput) {
                    "Live multi-input preview; seeking is unavailable for multiple video sequences."
                } else {
                    null
                }
            )
            _state.value = CompositionPreviewState(targetPlayer, result)
            result
        } catch (error: RuntimeException) {
            failPlayer(targetPlayer, error)
        } catch (error: LinkageError) {
            failPlayer(targetPlayer, error)
        }
    }

    fun seekMs(ms: Long): CompositionPreviewSeekResult {
        val activePlayer = compositionPlayer ?: return CompositionPreviewSeekResult.Unavailable
        val mode = graphMode ?: return CompositionPreviewSeekResult.Unavailable
        if (!CompositionPreviewPolicy.supportsSeeking(mode)) {
            return CompositionPreviewSeekResult.Unsupported
        }
        return try {
            activePlayer.seekTo(ms.coerceAtLeast(0))
            CompositionPreviewSeekResult.Applied
        } catch (error: RuntimeException) {
            failPlayer(activePlayer, error)
            CompositionPreviewSeekResult.Failed
        } catch (error: LinkageError) {
            failPlayer(activePlayer, error)
            CompositionPreviewSeekResult.Failed
        }
    }

    fun release() {
        released = true
        releasePlayer(compositionPlayer)
        _state.value = CompositionPreviewState()
    }

    private fun createPlayer(mode: CompositionPreviewGraphMode): CompositionPlayer? {
        return try {
            val builder = CompositionPlayer.Builder(appContext)
            if (mode == CompositionPreviewGraphMode.MultipleInput) {
                builder.setVideoGraphFactory(MultipleInputVideoGraph.Factory())
            }
            builder.build().also { player ->
                graphMode = mode
                compositionPlayer = player
                player.addListener(
                    object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            if (compositionPlayer === player) {
                                failPlayer(player, error)
                            }
                        }
                    }
                )
            }
        } catch (error: RuntimeException) {
            publishExportOnly(error.previewMessage())
            null
        } catch (error: LinkageError) {
            publishExportOnly(error.previewMessage())
            null
        }
    }

    private fun failPlayer(player: CompositionPlayer, error: Throwable): CompositionPreviewResult {
        Log.w(TAG, "Composition preview unavailable; export is unaffected.", error)
        if (compositionPlayer === player) {
            compositionPlayer = null
            graphMode = null
        }
        releasePlayer(player)
        return publishExportOnly(error.previewMessage())
    }

    private fun releasePlayer(player: CompositionPlayer?) {
        if (compositionPlayer === player) {
            compositionPlayer = null
            graphMode = null
        }
        if (player != null) {
            try {
                player.release()
            } catch (error: RuntimeException) {
                Log.w(TAG, "Failed to release composition preview.", error)
            }
        }
    }

    private fun publishExportOnly(detail: String): CompositionPreviewResult {
        StudioDiagnostics.note("preview", detail)
        val result = CompositionPreviewResult(
            capability = CompositionPreviewCapability.ExportOnly,
            detail = detail
        )
        _state.value = CompositionPreviewState(player = null, result = result)
        return result
    }

    private inline fun <T> previewValue(fallback: T, block: () -> T): T =
        try {
            block()
        } catch (_: RuntimeException) {
            fallback
        }

    private fun Throwable.previewMessage(): String =
        localizedMessage?.takeIf { it.isNotBlank() }
            ?: javaClass.simpleName

    private companion object {
        const val TAG = "CompositionPreview"
    }
}
