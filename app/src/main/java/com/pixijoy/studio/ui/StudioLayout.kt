package com.pixijoy.studio.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * How much room the window has, independent of a hinge.
 *
 * Compact: phone portrait, fold cover, and narrow multi-window.
 * PhoneLandscape: phone turned sideways, including shorter 640 dp-wide screens.
 * Medium: 7–8 inch tablet portrait and a fold opened like a book but under 800 dp.
 * Expanded: tablet landscape, large fold inner, and the Pixel Tablet stage.
 */
enum class StudioWindow {
    Compact,
    PhoneLandscape,
    Medium,
    Expanded
}

/** Physical crease. Flat includes a fold that is open and not separating. */
enum class StudioPosture { Flat, Book, Tabletop }

/** Where session tools sit. Tabletop wins over a wide window so the hinge stays clear. */
enum class StudioChrome { Bottom, Side, Tabletop }

/** Pixel bounds of one [FoldingFeature], in the window's coordinate space. */
data class StudioFold(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
    val vertical: Boolean,
    val separating: Boolean,
    val halfOpened: Boolean
)

data class StudioFrame(
    val widthDp: Int,
    val heightDp: Int,
    val window: StudioWindow,
    val posture: StudioPosture,
    val chrome: StudioChrome,
    val hinge: Dp,
    /** Distance from the top (tabletop) or left (book) to the crease. Zero when flat. */
    val hingeStart: Dp,
    val fold: StudioFold? = null
)

/** Picture that must remain when a side dock is showing. */
const val STUDIO_MIN_CANVAS_DP = 240

/** Group-rail item width before the rail scrolls instead of crushing labels. */
const val STUDIO_GROUP_MIN_DP = 64

/**
 * Crease position clamped so neither side of a fold is a sliver.
 * [rawStartDp] is the distance from the pane's own top or left, not the window.
 */
fun studioHingeStartDp(rawStartDp: Int, spanDp: Int): Int {
    if (spanDp <= 0) return 0
    val floor = minOf(120, spanDp / 3)
    val cap = maxOf(floor, spanDp * 72 / 100)
    if (rawStartDp <= 0) return floor
    return rawStartDp.coerceIn(floor, cap)
}

/**
 * Hinge offset inside a pane. [originXPx] / [originYPx] are that pane's
 * window coordinates, so a navigation rail does not shift the crease.
 */
fun sessionHingeStartDp(
    fold: StudioFold?,
    posture: StudioPosture,
    originXPx: Float,
    originYPx: Float,
    pxToDp: (Float) -> Float,
    paneWidthDp: Int,
    paneHeightDp: Int
): Int {
    if (fold == null || posture == StudioPosture.Flat) return 0
    val rawPx = if (posture == StudioPosture.Book) {
        fold.leftPx.toFloat() - originXPx
    } else {
        fold.topPx.toFloat() - originYPx
    }
    val rawDp = pxToDp(rawPx.coerceAtLeast(0f)).toInt()
    val span = if (posture == StudioPosture.Book) paneWidthDp else paneHeightDp
    return studioHingeStartDp(rawDp, span)
}

/**
 * Session chrome for the pane the editor actually occupies.
 * A full-bleed phone uses the window. A session sitting in the tablet or
 * fold detail pane uses that pane, and only owns the hinge when the hub
 * did not already split on it.
 */
fun studioSessionChrome(
    paneWidthDp: Int,
    paneHeightDp: Int,
    posture: StudioPosture
): StudioChrome {
    if (posture == StudioPosture.Tabletop && paneHeightDp >= 420) {
        return StudioChrome.Tabletop
    }
    if (posture == StudioPosture.Book && paneWidthDp >= 480) {
        return StudioChrome.Side
    }
    if (!isStudioSessionSideTools(paneWidthDp, paneHeightDp)) {
        return StudioChrome.Bottom
    }
    val dock = studioSideDockWidthDp(paneWidthDp, paneHeightDp)
    return if (paneWidthDp - dock >= STUDIO_MIN_CANVAS_DP) {
        StudioChrome.Side
    } else {
        StudioChrome.Bottom
    }
}

fun studioWindow(widthDp: Int, heightDp: Int): StudioWindow {
    if (widthDp <= 0 || heightDp <= 0) return StudioWindow.Compact
    val shortSide = minOf(widthDp, heightDp)
    val landscape = widthDp > heightDp
    return when {
        widthDp >= 800 && heightDp >= 480 -> StudioWindow.Expanded
        shortSide >= 600 -> StudioWindow.Medium
        landscape && widthDp >= 640 && heightDp < 560 -> StudioWindow.PhoneLandscape
        else -> StudioWindow.Compact
    }
}

fun studioPosture(fold: StudioFold?): StudioPosture {
    if (fold == null) return StudioPosture.Flat
    if (!fold.separating && !fold.halfOpened) return StudioPosture.Flat
    return if (fold.vertical) StudioPosture.Book else StudioPosture.Tabletop
}

fun studioChrome(widthDp: Int, heightDp: Int, posture: StudioPosture): StudioChrome {
    if (posture == StudioPosture.Tabletop) return StudioChrome.Tabletop
    return if (isStudioSessionSideTools(widthDp, heightDp)) StudioChrome.Side else StudioChrome.Bottom
}

/**
 * Tool-grid columns for the width of the pane itself, not the whole window.
 * A catalog narrower than 300 dp is one column so titles are not a sliver.
 */
fun studioPaneColumns(paneWidthDp: Int): Int = when {
    paneWidthDp >= 520 -> 3
    paneWidthDp >= 300 -> 2
    else -> 1
}

/**
 * True when six or eight group labels cannot keep a 64 dp target.
 * Phone portrait still fits Photo (6). Movie (8) and every side dock scroll.
 */
fun studioGroupRailScrolls(widthDp: Int, itemCount: Int): Boolean {
    if (itemCount <= 1 || widthDp <= 0) return false
    return widthDp < STUDIO_GROUP_MIN_DP * itemCount
}

/**
 * Short side-tool stages cannot spare a full transport stack under the picture.
 * Bottom glass already caps itself; this only shortens the docked timeline.
 */
fun studioTimelineCompact(sideTools: Boolean, paneHeightDp: Int): Boolean =
    sideTools && paneHeightDp in 1 until 560

/**
 * An open editor covers the hub on a phone and in tabletop flex.
 * Flat tablet and book stay catalog | canvas so the hinge is split once.
 * Tabletop must not park the tool catalog on the lower half while the
 * picture and the real controls fight for the upper half.
 */
fun sessionOwnsStudioWindow(twoPane: Boolean, posture: StudioPosture): Boolean =
    !twoPane || posture == StudioPosture.Tabletop

/**
 * Catalog | canvas for tablet and fold-inner. Phone landscape stays a
 * single-pane hub so an open session can go full-bleed with side tools.
 */
fun isStudioTwoPane(widthDp: Int, heightDp: Int): Boolean {
    val window = studioWindow(widthDp, heightDp)
    return window == StudioWindow.Medium || window == StudioWindow.Expanded
}

/**
 * Session tools beside the canvas on tablet, fold-inner, and phone landscape.
 * A 640×360 phone qualifies. A 600 dp-wide multi-window strip does not.
 */
fun isStudioSessionSideTools(widthDp: Int, heightDp: Int): Boolean =
    when (studioWindow(widthDp, heightDp)) {
        StudioWindow.PhoneLandscape, StudioWindow.Medium, StudioWindow.Expanded -> true
        StudioWindow.Compact -> false
    }

/**
 * Side dock width. Expanded keeps 360 dp. Medium portrait is narrower so the
 * picture still has a majority. Phone landscape stays ~36% of the stage.
 */
fun studioSideDockWidthDp(widthDp: Int, heightDp: Int): Int =
    when (studioWindow(widthDp, heightDp)) {
        StudioWindow.Expanded -> 360
        StudioWindow.Medium -> (widthDp * 34 / 100).coerceIn(220, 320)
        StudioWindow.PhoneLandscape -> {
            val raw = widthDp * 36 / 100
            if (widthDp >= 700) raw.coerceIn(280, 320) else raw.coerceIn(220, 279)
        }
        StudioWindow.Compact -> (widthDp * 36 / 100).coerceIn(280, 320)
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
fun rememberStudioFrame(): StudioFrame {
    val config = LocalConfiguration.current
    val fold = currentStudioFold()
    val posture = studioPosture(fold)
    val density = LocalDensity.current
    val hinge = if (fold == null || posture == StudioPosture.Flat) {
        0.dp
    } else {
        val px = if (posture == StudioPosture.Book) {
            (fold.rightPx - fold.leftPx).coerceAtLeast(0)
        } else {
            (fold.bottomPx - fold.topPx).coerceAtLeast(0)
        }
        with(density) { px.toDp() }.coerceAtLeast(12.dp)
    }
    val hingeStart = if (fold == null || posture == StudioPosture.Flat) {
        0.dp
    } else {
        val raw = with(density) {
            if (posture == StudioPosture.Book) fold.leftPx.toDp() else fold.topPx.toDp()
        }.value.toInt()
        val span = if (posture == StudioPosture.Book) config.screenWidthDp else config.screenHeightDp
        studioHingeStartDp(raw, span).dp
    }
    return StudioFrame(
        widthDp = config.screenWidthDp,
        heightDp = config.screenHeightDp,
        window = studioWindow(config.screenWidthDp, config.screenHeightDp),
        posture = posture,
        chrome = studioChrome(config.screenWidthDp, config.screenHeightDp, posture),
        hinge = hinge,
        hingeStart = hingeStart,
        fold = fold
    )
}

@Composable
private fun currentStudioFold(): StudioFold? {
    val activity = LocalContext.current as? Activity ?: return null
    val layoutInfo = WindowInfoTracker.getOrCreate(activity)
        .windowLayoutInfo(activity)
        .collectAsState(initial = null)
        .value
    val feature = layoutInfo?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull()
        ?: return null
    return StudioFold(
        leftPx = feature.bounds.left,
        topPx = feature.bounds.top,
        rightPx = feature.bounds.right,
        bottomPx = feature.bounds.bottom,
        vertical = feature.orientation == FoldingFeature.Orientation.VERTICAL,
        separating = feature.isSeparating,
        halfOpened = feature.state == FoldingFeature.State.HALF_OPENED
    )
}

/**
 * Row that keeps a vertical hinge on the physical left, even in RTL.
 * Children still lay out in the user's writing direction.
 */
@Composable
fun StudioDirectedRow(
    physicalStart: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    if (!physicalStart) {
        Row(modifier = modifier, content = content)
        return
    }
    val userDirection = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = modifier) {
            val scope = this
            CompositionLocalProvider(LocalLayoutDirection provides userDirection) {
                scope.content()
            }
        }
    }
}

@Composable
fun StudioTwoPane(
    onPickTool: (StudioTool) -> Unit,
    detail: @Composable () -> Unit
) {
    val fold = currentStudioFold()
    val posture = studioPosture(fold)
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
    ) {
        val paneW = maxWidth.value.toInt().coerceAtLeast(1)
        val paneH = maxHeight.value.toInt().coerceAtLeast(1)
        if (posture == StudioPosture.Tabletop && fold != null) {
            val hinge = with(density) { (fold.bottomPx - fold.topPx).toDp() }.coerceAtLeast(12.dp)
            val top = sessionHingeStartDp(
                fold = fold,
                posture = posture,
                originXPx = origin.x,
                originYPx = origin.y,
                pxToDp = { px -> with(density) { px.toDp().value } },
                paneWidthDp = paneW,
                paneHeightDp = paneH
            ).dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("studio_two_pane")
            ) {
                Box(Modifier.height(top).fillMaxWidth()) { detail() }
                Spacer(Modifier.height(hinge).fillMaxWidth().testTag("studio_hinge_gap"))
                StudioPane(
                    onPickTool = onPickTool,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            return@BoxWithConstraints
        }
        if (posture == StudioPosture.Book && fold != null) {
            val hinge = with(density) { (fold.rightPx - fold.leftPx).toDp() }.coerceAtLeast(12.dp)
            val left = sessionHingeStartDp(
                fold = fold,
                posture = posture,
                originXPx = origin.x,
                originYPx = origin.y,
                pxToDp = { px -> with(density) { px.toDp().value } },
                paneWidthDp = paneW,
                paneHeightDp = paneH
            ).dp
            StudioDirectedRow(
                physicalStart = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("studio_two_pane")
            ) {
                StudioPane(
                    onPickTool = onPickTool,
                    modifier = Modifier
                        .width(left)
                        .fillMaxHeight()
                )
                Spacer(
                    Modifier
                        .width(hinge)
                        .fillMaxHeight()
                        .testTag("studio_hinge_gap")
                )
                Box(Modifier.weight(1f).fillMaxHeight()) { detail() }
            }
            return@BoxWithConstraints
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .testTag("studio_two_pane")
        ) {
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
