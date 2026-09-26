package com.pixijoy.ai.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Preview | tools for tablet / fold-inner. Phone landscape stays single-pane
 * even when width is large — short height keeps the glass under the canvas.
 *
 * Pixel Tablet AVD is ~800×500 dp (2560×1600 @ 320dpi). The older 840×520
 * Expanded gate skipped that device entirely.
 */
fun isStudioTwoPane(widthDp: Int, heightDp: Int): Boolean =
    widthDp >= 800 && heightDp >= 480

/**
 * Bottom glass max height on single-pane. Short landscape viewports keep more
 * of the canvas visible instead of a fixed 280 dp panel eating most of the
 * screen.
 */
fun studioGlassMaxDp(heightDp: Int): Int {
    if (heightDp <= 0) return 280
    if (heightDp < 560) {
        // Leave ~top chrome + canvas: cap around 42% of height, floor 160.
        return (heightDp * 42 / 100).coerceIn(160, 220)
    }
    return 280
}

@Composable
fun StudioTwoPane(
    onPickTool: (StudioTool) -> Unit,
    detail: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val layoutInfo = if (activity != null) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collectAsState(initial = null).value
    } else {
        null
    }
    val fold = layoutInfo?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull()
    val density = LocalDensity.current
    val bookFold = fold != null &&
        fold.orientation == FoldingFeature.Orientation.VERTICAL &&
        (fold.isSeparating || fold.state == FoldingFeature.State.HALF_OPENED)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .testTag("studio_two_pane")
    ) {
        if (bookFold) {
            val feature = checkNotNull(fold)
            val left = with(density) { feature.bounds.left.toDp() }.coerceAtLeast(280.dp)
            val hinge = with(density) { feature.bounds.width().toDp() }.coerceAtLeast(16.dp)
            StudioPane(
                onPickTool = onPickTool,
                modifier = Modifier
                    .width(left)
                    .fillMaxHeight()
            )
            Spacer(Modifier.width(hinge).testTag("studio_hinge_gap"))
            Box(Modifier.weight(1f).fillMaxHeight()) { detail() }
        } else {
            StudioPane(
                onPickTool = onPickTool,
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
            ) { detail() }
        }
    }
}
