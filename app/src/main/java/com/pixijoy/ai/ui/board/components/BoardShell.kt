package com.pixijoy.ai.ui.board.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.LocalBoardEmbeddedInMainShell
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.components.AdaptiveContent
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.pixiHingeSafePadding
import com.pixijoy.ai.ui.components.rememberWindowWidthSizeClass

/**
 * Root chrome (PixiJoy bible labels): Home · Explore · Create · Community · My Stuff.
 * Nav-spec destinations map Discover→Explore, Profile→My Stuff — do not rename labels.
 */
enum class BoardShellTab { Home, Discover, Create, Community, MyStuff }

/**
 * K-SHELL — bible chrome: Home · Explore · Create · Community · My Stuff.
 * Compact: bottom bar. Expanded: navigation rail. Embedded under MainScaffold: content only.
 * Icons: `pj_ic_*` (Design Bible / Identity — not Material Defaults).
 */
@Composable
fun BoardMainShell(
    selected: BoardShellTab,
    onTab: (BoardShellTab) -> Unit,
    content: @Composable () -> Unit
) {
    val embedded = LocalBoardEmbeddedInMainShell.current
    if (embedded) {
        AdaptiveContent(modifier = Modifier.fillMaxSize()) {
            content()
        }
        return
    }

    val colors = BoardTheme.colors
    val widthClass = rememberWindowWidthSizeClass()
    val isExpanded = widthClass == WindowWidthSizeClass.Expanded

    if (isExpanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .pixiHingeSafePadding()
        ) {
            BoardNavRail(selected = selected, onTab = onTab)
            AdaptiveContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    } else {
        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    containerColor = colors.surface,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.safeDrawing
                ) {
                    BoardNavBarItems(selected = selected, onTab = onTab)
                }
            }
        ) { padding ->
            AdaptiveContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BoardNavRail(
    selected: BoardShellTab,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    NavigationRail(containerColor = colors.surface) {
        BoardShellTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes())
            val selectedTab = selected == tab
            NavigationRailItem(
                selected = selectedTab,
                onClick = { onTab(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes()),
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        tint = if (selectedTab) colors.primary else colors.textSecondary
                    )
                },
                label = {
                    Text(
                        label,
                        style = BoardType.caption,
                        color = if (selectedTab) colors.primary else colors.textSecondary
                    )
                }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BoardNavBarItems(
    selected: BoardShellTab,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    BoardShellTab.entries.forEach { tab ->
        val label = stringResource(tab.labelRes())
        val selectedTab = selected == tab
        NavigationBarItem(
            selected = selectedTab,
            onClick = { onTab(tab) },
            icon = {
                Icon(
                    painter = painterResource(tab.iconRes()),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = if (selectedTab) colors.primary else colors.textSecondary
                )
            },
            label = {
                Text(
                    label,
                    style = BoardType.caption,
                    color = if (selectedTab) colors.primary else colors.textSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colors.primary,
                selectedTextColor = colors.primary,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary,
                indicatorColor = colors.primarySoft
            )
        )
    }
}

private fun BoardShellTab.labelRes(): Int = when (this) {
    BoardShellTab.Home -> R.string.nav_home
    BoardShellTab.Discover -> R.string.nav_explore
    BoardShellTab.Create -> R.string.nav_create
    BoardShellTab.Community -> R.string.nav_community
    BoardShellTab.MyStuff -> R.string.nav_my_stuff
}

private fun BoardShellTab.iconRes(): Int = when (this) {
    BoardShellTab.Home -> R.drawable.pj_ic_home
    BoardShellTab.Discover -> R.drawable.pj_ic_explore
    BoardShellTab.Create -> R.drawable.pj_ic_add
    BoardShellTab.Community -> R.drawable.pj_ic_community
    BoardShellTab.MyStuff -> R.drawable.pj_ic_folder
}
