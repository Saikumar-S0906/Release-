package com.pixijoy.movie

/**
 * F1 classical track-attach: normalized image-space anchors (0..1) sampled over a
 * clip's local 0..1 timeline. Preview and export share the same sampler.
 * Does not invent a path — seed-only when the Track pack is unbound.
 */
data class TrackAnchorKeyframe(
    val t: Float,
    val x: Float,
    val y: Float
) {
    fun normalized(): TrackAnchorKeyframe = copy(
        t = t.coerceIn(0f, 1f),
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f)
    )
}

data class TrackAnchorPath(
    val keys: List<TrackAnchorKeyframe> = emptyList()
) {
    val isEmpty: Boolean get() = keys.isEmpty()

    fun sample(localT: Float): Pair<Float, Float>? {
        val sorted = keys.map { it.normalized() }.sortedBy { it.t }
        if (sorted.isEmpty()) return null
        val u = localT.coerceIn(0f, 1f)
        if (u <= sorted.first().t) return sorted.first().x to sorted.first().y
        if (u >= sorted.last().t) return sorted.last().x to sorted.last().y
        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (u in a.t..b.t) {
                val span = (b.t - a.t).coerceAtLeast(1e-4f)
                val w = (u - a.t) / span
                return (a.x + (b.x - a.x) * w) to (a.y + (b.y - a.y) * w)
            }
        }
        return sorted.last().x to sorted.last().y
    }

    companion object {
        fun seed(x: Float, y: Float): TrackAnchorPath =
            TrackAnchorPath(
                listOf(
                    TrackAnchorKeyframe(0f, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)),
                    TrackAnchorKeyframe(1f, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
                )
            )

        val None = TrackAnchorPath()
    }
}

/**
 * Resolves an F1 image-space anchor at composition time for CG / VFX follow.
 * Shared by preview and export overlays.
 */
fun interface TrackAnchorResolver {
    fun at(compositionMs: Long): Pair<Float, Float>?

    companion object {
        val None = TrackAnchorResolver { null }

        fun fixed(x: Float, y: Float): TrackAnchorResolver = TrackAnchorResolver { x to y }

        /**
         * Per-clip bake when Track ML is bound; otherwise the clip's seed point.
         * Outside a tracked clip, returns the nearest active track seed (or null).
         */
        fun forTimeline(paths: List<String>, timeline: Timeline): TrackAnchorResolver {
            val seedFallback = timeline.clips.firstOrNull { it.track.isActive }?.track?.let {
                it.x to it.y
            }
            return TrackAnchorResolver { ms ->
                val hit = timeline.clipAt(ms)
                if (hit != null && hit.first.track.isActive) {
                    val (clip, start) = hit
                    val local = (ms - start).coerceAtLeast(0L)
                    val localT = local.toFloat() / clip.durationMs.coerceAtLeast(1L).toFloat()
                    val mediaPath = paths.getOrNull(clip.mediaIndex)
                    if (mediaPath != null) {
                        TrackPathBake.anchors(mediaPath, clip.track, clip.durationMs)
                            .sample(localT)
                            ?: (clip.track.x to clip.track.y)
                    } else {
                        clip.track.x to clip.track.y
                    }
                } else {
                    seedFallback
                }
            }
        }
    }
}

/**
 * Maps image-normalized track (nx, ny) into CG overlay NDC offset, then adds
 * the layer's manual x/y. Same formula for PropCube / DustTrail / PropGltf plate.
 */
object TrackAttach {
    fun cgOffset(
        layerX: Float,
        layerY: Float,
        imageX: Float,
        imageY: Float,
        strength: Float = 0.35f
    ): Pair<Float, Float> {
        val nx = (imageX * 2f - 1f).coerceIn(-0.95f, 0.95f)
        val ny = (1f - imageY * 2f).coerceIn(-0.95f, 0.95f)
        val s = strength.coerceIn(0f, 1f)
        return (layerX + nx * s).coerceIn(-0.95f, 0.95f) to
            (layerY + ny * s).coerceIn(-0.95f, 0.95f)
    }

    fun vfxNdc(imageX: Float, imageY: Float): Pair<Float, Float> {
        val nx = (imageX * 2f - 1f).coerceIn(-0.95f, 0.95f)
        val ny = (1f - imageY * 2f).coerceIn(-0.95f, 0.95f)
        return nx to ny
    }
}
