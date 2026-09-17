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

fun isStudioTwoPane(widthDp: Int, heightDp: Int): Boolean =
    widthDp >= 840 && heightDp >= 520

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
