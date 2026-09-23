package com.pixijoy.movie

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.transformer.CompositionPlayer

class CompositionPreview(
    context: Context,
    private val engine: Media3MovieEngine = Media3MovieEngine()
) {
    val player: Player = CompositionPlayer.Builder(context.applicationContext).build()

    fun bind(paths: List<String>, timeline: Timeline, playWhenReady: Boolean) {
        bind(paths, MultiTrackTimeline.fromFlatTimeline(timeline), playWhenReady)
    }

    fun bind(paths: List<String>, timeline: MultiTrackTimeline, playWhenReady: Boolean) {
        val compositionPlayer = player as CompositionPlayer
        val pos = compositionPlayer.currentPosition
        // Media3 1.5 CompositionPlayer is single-input preview; multi layouts composite on export.
        val previewTimeline = if (timeline.layout.isMulti()) {
            timeline.copy(layout = VideoLayout.Single)
        } else {
            timeline
        }
        compositionPlayer.setComposition(engine.composition(paths, previewTimeline))
        compositionPlayer.repeatMode = Player.REPEAT_MODE_ALL
        compositionPlayer.prepare()
        val end = timeline.totalMs().coerceAtLeast(1)
        if (pos in 1 until end) compositionPlayer.seekTo(pos)
        compositionPlayer.playWhenReady = playWhenReady
    }

    fun seekMs(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0))
    }

    fun release() {
        player.release()
    }
}
