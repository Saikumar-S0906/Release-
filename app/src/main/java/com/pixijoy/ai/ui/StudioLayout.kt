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
 * Catalog | canvas for tablet / fold-inner (hub). Phone landscape stays a
 * single-pane hub so an open session can go full-bleed with side tools.
 *
 * Pixel Tablet AVD is ~800×500 dp (2560×1600 @ 320dpi). The older 840×520
 * Expanded gate skipped that device entirely.
 */
fun isStudioTwoPane(widthDp: Int, heightDp: Int): Boolean =
    widthDp >= 800 && heightDp >= 480

/**
 * Session tools beside the canvas: tablet / fold-inner, and wide phone
 * landscape so the short axis stays for the picture (not bottom glass).
 */
fun isStudioSessionSideTools(widthDp: Int, heightDp: Int): Boolean {
    if (isStudioTwoPane(widthDp, heightDp)) return true
    return widthDp >= 700 && widthDp > heightDp && heightDp in 1 until 560
}

/**
 * Side dock width. Tablet / fold keep 360 dp; phone landscape stays ~36% so
 * the canvas remains the majority of a ~900 dp-wide stage.
 */
fun studioSideDockWidthDp(widthDp: Int, heightDp: Int): Int {
    if (isStudioTwoPane(widthDp, heightDp)) return 360
    return (widthDp * 36 / 100).coerceIn(280, 320)
}

/**
 * Bottom glass max height on single-pane. Short landscape viewports keep more
 * of the canvas visible instead of a fixed 280 dp panel eating most of the
 * screen. (Wide phone landscape normally uses [isStudioSessionSideTools]; this
 * still covers narrow / multi-window landscape.)
 *
 * [fontScale] above 1 grows the glass so large-type chips/sliders stay usable
 * without swallowing more than ~55% of the viewport.
 */
fun studioGlassMaxDp(heightDp: Int, fontScale: Float = 1f): Int {
    if (heightDp <= 0) return 280
    val base = if (heightDp < 560) {
        // Leave ~top chrome + canvas: cap around 42% of height, floor 160.
        (heightDp * 42 / 100).coerceIn(160, 220)
    } else {
        280
    }
    val scale = fontScale.coerceIn(1f, 1.6f)
    if (scale <= 1.05f) return base
    val bumped = (base * scale).toInt()
    val hardCap = (heightDp * 55 / 100).coerceAtLeast(base)
    val absCap = if (heightDp < 560) 260 else 360
    return bumped.coerceAtMost(hardCap).coerceAtMost(absCap)
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
