package com.pixijoy.ai.ui.board.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.components.AdaptiveContent
import com.pixijoy.ai.ui.components.PixiTwoPane
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.pixiHingeSafePadding
import com.pixijoy.ai.ui.components.rememberPixiGridColumns
import com.pixijoy.ai.ui.components.rememberWindowWidthSizeClass
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Board content: hinge-safe + max width + board background.
 * Compact/Medium: AdaptiveContent default. Expanded: also cap phone-like column when [phoneMaxWidth].
 */
@Composable
fun BoardAdaptiveFrame(
    modifier: Modifier = Modifier,
    phoneMaxWidth: Dp = 480.dp,
    content: @Composable () -> Unit
) {
    val width = rememberWindowWidthSizeClass()
    val heightDp = LocalConfiguration.current.screenHeightDp
    val colors = BoardTheme.colors
    // Cap to a phone column on tablets only — landscape phones are Expanded-width but short.
    val capPhoneColumn = width == WindowWidthSizeClass.Expanded && heightDp >= 520
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .pixiHingeSafePadding()
        ) {
            if (capPhoneColumn) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.fillMaxSize().widthIn(max = phoneMaxWidth)) {
                        content()
                    }
                }
            } else {
                content()
            }
        }
    }
}

/** Full-bleed on Expanded (for two-pane / wide galleries) — no phone max width. */
@Composable
fun BoardWideFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = BoardTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AdaptiveContent(
            modifier = Modifier
                .fillMaxWidth()
                .pixiHingeSafePadding()
        ) {
            content()
        }
    }
}

@Composable
fun boardGridColumns(
    compact: Int = 2,
    medium: Int = 3,
    expanded: Int = 4
): Int = rememberPixiGridColumns(compact = compact, medium = medium, expanded = expanded)

/** True on a real tablet work surface — not a phone rotated to landscape. */
@Composable
fun rememberBoardTwoPane(): Boolean {
    val width = rememberWindowWidthSizeClass()
    val heightDp = LocalConfiguration.current.screenHeightDp
    return width == WindowWidthSizeClass.Expanded && heightDp >= 520
}

@Composable
fun BoardListDetail(
    showDetail: Boolean,
    list: @Composable ColumnScope.() -> Unit,
    detail: @Composable ColumnScope.() -> Unit
) {
    if (rememberBoardTwoPane()) {
        PixiTwoPane(
            showDetail = showDetail,
            list = list,
            detail = detail
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showDetail) detail() else list()
            }
        }
    }
}

/** Responsive section grid for chooser tiles / option cards. */
@Composable
fun <T> BoardSectionGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    compactColumns: Int = 2,
    mediumColumns: Int = 3,
    expandedColumns: Int = 4,
    itemContent: @Composable (T) -> Unit
) {
    val columns = boardGridColumns(compactColumns, mediumColumns, expandedColumns)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PixiSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        items(items) { item -> itemContent(item) }
    }
}

/** Preview | controls split on Expanded; stacked on phone. */
@Composable
fun BoardPreviewControls(
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit
) {
    val width = rememberWindowWidthSizeClass()
    if (width == WindowWidthSizeClass.Expanded) {
        androidx.compose.foundation.layout.Row(
            modifier = modifier
                .fillMaxSize()
                .pixiHingeSafePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxSize()
                    .padding(PixiSpacing.md)
            ) {
                preview()
            }
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxSize()
                    .padding(PixiSpacing.lg),
                content = controls
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(PixiSpacing.md)
            ) {
                preview()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.lg),
                content = controls
            )
        }
    }
}
