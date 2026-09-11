package com.pixijoy.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

enum class WindowWidthSizeClass {
    Compact, Medium, Expanded
}

@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return when {
        screenWidth < 600.dp -> WindowWidthSizeClass.Compact
        screenWidth < 840.dp -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}

/** Grid column count for style/template/creation tiles. */
@Composable
fun rememberPixiGridColumns(
    compact: Int = 2,
    medium: Int = 3,
    expanded: Int = 4
): Int {
    val heightDp = LocalConfiguration.current.screenHeightDp
    val raw = when (rememberWindowWidthSizeClass()) {
        WindowWidthSizeClass.Compact -> compact
        WindowWidthSizeClass.Medium -> medium
        WindowWidthSizeClass.Expanded -> expanded
    }
    // Phone landscape is a short viewport — never 3–4 postage stamps.
    return if (heightDp < 520) minOf(raw, 2) else raw
}

/**
 * Fold / large tablet: keep primary CTAs out of the center hinge band (~48dp).
 * On phones this is a no-op.
 */
@Composable
fun Modifier.pixiHingeSafePadding(): Modifier {
    val widthClass = rememberWindowWidthSizeClass()
    if (widthClass != WindowWidthSizeClass.Expanded) return this
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // Approximate fold hinge as center strip; pad sides so content can avoid mid-screen critical controls.
    val hingePad = if (screenWidth >= 840.dp) 12.dp else 0.dp
    return this.padding(horizontal = hingePad)
}

/** Caps preview height to ~40% of screen while keeping aspect ratio on all devices. */
@Composable
fun Modifier.responsivePreviewHeight(
    aspectRatio: Float = 4f / 3f,
    maxHeight: Dp = 320.dp
): Modifier {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val capped = min(maxHeight.value, screenHeight.value * 0.4f).dp
    return this
        .fillMaxWidth()
        .heightIn(max = capped)
        .then(if (aspectRatio > 0f) Modifier.aspectRatio(aspectRatio) else Modifier)
}

@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 840.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.widthIn(max = maxContentWidth)) {
            content()
        }
    }
}

/**
 * List | detail two-pane for tablet / unfolded fold.
 * Compact/Medium: shows [list] only; call [onShowDetail] to navigate.
 * Expanded: shows both side-by-side when [showDetail] is true.
 */
@Composable
fun PixiTwoPane(
    showDetail: Boolean,
    modifier: Modifier = Modifier,
    listWeight: Float = 0.42f,
    detailWeight: Float = 0.58f,
    list: @Composable ColumnScope.() -> Unit,
    detail: @Composable ColumnScope.() -> Unit
) {
    val widthClass = rememberWindowWidthSizeClass()
    when (widthClass) {
        WindowWidthSizeClass.Expanded -> {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .pixiHingeSafePadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(listWeight)
                        .fillMaxHeight()
                        .padding(end = 8.dp)
                ) { list() }
                VerticalDivider()
                Column(
                    modifier = Modifier
                        .weight(detailWeight)
                        .fillMaxHeight()
                        .padding(start = 8.dp)
                ) {
                    if (showDetail) {
                        detail()
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "Select an item",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        else -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (showDetail) detail() else list()
            }
        }
    }
}

@Composable
fun <T> PixiAdaptiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = rememberPixiGridColumns(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    itemContent: @Composable (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        items(items) { item ->
            itemContent(item)
        }
    }
}

/** Snapshot of device / QA-relevant UI environment for the Responsive QA screen. */
data class PixiResponsiveSnapshot(
    val widthClass: WindowWidthSizeClass,
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
    val isRtl: Boolean,
    val darkTheme: Boolean
)

@Composable
fun rememberPixiResponsiveSnapshot(darkTheme: Boolean): PixiResponsiveSnapshot {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val widthClass = rememberWindowWidthSizeClass()
    return remember(config, density.fontScale, rtl, darkTheme, widthClass) {
        PixiResponsiveSnapshot(
            widthClass = widthClass,
            widthDp = config.screenWidthDp,
            heightDp = config.screenHeightDp,
            fontScale = density.fontScale,
            isRtl = rtl,
            darkTheme = darkTheme
        )
    }
}
