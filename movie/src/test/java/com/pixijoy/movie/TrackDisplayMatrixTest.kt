package com.pixijoy.movie

import android.graphics.Matrix
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Greptile #30 P2: overlay pan/zoom must match the clip matrix exactly.
 * Compares TrackAttach.mapThroughDisplay to ClipMotion.toMatrixTransformation
 * the same way export stacks user motion then track follow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class TrackDisplayMatrixTest {
    @Test
    fun mapThroughDisplay_matchesClipMatrix_rotationAndStackedMotion() {
        val user = TransformKeyframe(
            t = 0f,
            scale = 1.12f,
            tx = 0.08f,
            ty = -0.04f,
            rotationDeg = 12f
        )
        val follow = TransformKeyframe(
            t = 0f,
            scale = 1.08f,
            tx = TrackPathBake.panX(0.72f),
            ty = TrackPathBake.panY(0.31f),
            rotationDeg = -6f
        )
        val imageX = 0.72f
        val imageY = 0.31f
        val viaDisplay = TrackAttach.mapThroughDisplay(imageX, imageY, listOf(user, follow))
        val viaMatrix = mapLikeClipEffects(imageX, imageY, listOf(user, follow))
        assertEquals(viaMatrix.first, viaDisplay.first, 0.0005f)
        assertEquals(viaMatrix.second, viaDisplay.second, 0.0005f)
    }

    private fun mapLikeClipEffects(
        imageX: Float,
        imageY: Float,
        transforms: List<TransformKeyframe>
    ): Pair<Float, Float> {
        var x = imageX * 2f - 1f
        var y = 1f - imageY * 2f
        val durationUs = 1_000_000L
        for (raw in transforms) {
            val motion = ClipMotion(listOf(raw))
            val matrix: Matrix = motion.toMatrixTransformation(durationUs).getMatrix(0L)
            val pts = floatArrayOf(x, y)
            matrix.mapPoints(pts)
            x = pts[0]
            y = pts[1]
        }
        val outX = ((x + 1f) / 2f).coerceIn(0f, 1f)
        val outY = ((1f - y) / 2f).coerceIn(0f, 1f)
        return outX to outY
    }
}
