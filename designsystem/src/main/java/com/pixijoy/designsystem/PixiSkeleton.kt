package com.pixijoy.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Sweep length for the cover sheen. Longer than a chrome fade so the band reads as loading. */
const val PixiSkeletonSweepMs = 1200

/** Width of the moving band as a fraction of the placeholder. */
const val PixiSkeletonSheenFraction = 0.42f

internal val PixiSkeletonSheen = PixiTextMuted.copy(alpha = 0.16f)
internal val PixiSkeletonSheenCore = PixiTextMuted.copy(alpha = 0.38f)

/**
 * Horizontal start of the sheen band.
 * Progress 0 sits the band fully off the leading edge; progress 1 sits it fully past the trailing edge.
 */
fun pixiSkeletonSheenStart(
    progress: Float,
    width: Float,
    fraction: Float = PixiSkeletonSheenFraction
): Float {
    if (width <= 0f) return 0f
    val band = width * fraction.coerceIn(0.05f, 1f)
    val t = progress.coerceIn(0f, 1f)
    return -band + (width + band * 2f) * t
}

/**
 * Placeholder while a bitmap decodes. Fills [modifier] with [PixiSurface2] and, when motion is on,
 * a cool sheen. Animator duration scale 0 keeps a still surface.
 */
@Composable
fun PixiSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val described = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    if (pixiMotionMs(emphasized = false) == 0) {
        Box(
            modifier
                .then(described)
                .background(PixiSurface2)
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "pixiSkeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PixiSkeletonSweepMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pixiSkeletonProgress"
    )
    // Read in composition so the sheen moves even if draw does not observe snapshot state.
    val frame = progress
    Box(
        modifier
            .then(described)
            .drawBehind { drawPixiSkeletonSheen(frame) }
    )
}

private fun DrawScope.drawPixiSkeletonSheen(progress: Float) {
    drawRect(PixiSurface2)
    val width = size.width
    if (width <= 0f || size.height <= 0f) return
    val band = width * PixiSkeletonSheenFraction
    val start = pixiSkeletonSheenStart(progress, width)
    drawRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.22f to PixiSkeletonSheen,
                0.5f to PixiSkeletonSheenCore,
                0.78f to PixiSkeletonSheen,
                1f to Color.Transparent
            ),
            start = Offset(start, size.height),
            end = Offset(start + band, 0f)
        )
    )
}
