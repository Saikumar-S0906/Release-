package com.pixijoy.ai.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixijoy.ai.R
import com.pixijoy.ai.data.UserStats
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.ads.BannerAdView
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.board.LocalBoardCanPop
import com.pixijoy.ai.ui.board.LocalBoardEmbeddedInMainShell
import com.pixijoy.ai.ui.board.inTab
import com.pixijoy.ai.ui.components.AdaptiveContent
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.pixiHingeSafePadding
import com.pixijoy.ai.ui.components.rememberWindowWidthSizeClass
import com.pixijoy.ai.ui.theme.PixiMotionMs

/**
 * Locked UI shell (PixiJoy bible): Home · Explore · Create · Community · My Stuff.
 * Discover/Profile in nav docs map to Explore/My Stuff labels — do not rename chrome.
 */
@Composable
fun MainScaffold(
    stats: UserStats,
    onOpenBoardScreen: (String) -> Unit = {},
    boardPipeline: com.pixijoy.ai.ui.board.BoardPipelineCallbacks =
        com.pixijoy.ai.ui.board.BoardPipelineCallbacks()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val widthClass = rememberWindowWidthSizeClass()
    val colors = MaterialTheme.colorScheme
    val isExpanded = widthClass == WindowWidthSizeClass.Expanded
    var tabResetTicks by remember {
        mutableStateOf(BottomNavItem.entries.associateWith { 0 })
    }

    fun goTab(route: String, forceRoot: Boolean = false) {
        val item = BottomNavItem.entries.find { it.route == route }
        if (forceRoot && item != null && currentRoute == route) {
            tabResetTicks = tabResetTicks.toMutableMap().also { it[item] = (it[item] ?: 0) + 1 }
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openBoardFromTab(screenId: String) {
        BoardProductionTabs.tabForRoot(screenId)?.let { tab ->
            val route = when (tab) {
                com.pixijoy.ai.ui.board.components.BoardShellTab.Home -> BottomNavItem.Home.route
                com.pixijoy.ai.ui.board.components.BoardShellTab.Discover -> BottomNavItem.Discover.route
                com.pixijoy.ai.ui.board.components.BoardShellTab.Create -> BottomNavItem.Create.route
                com.pixijoy.ai.ui.board.components.BoardShellTab.Community -> BottomNavItem.Community.route
                com.pixijoy.ai.ui.board.components.BoardShellTab.MyStuff -> BottomNavItem.MyStuff.route
            }
            goTab(route)
        } ?: onOpenBoardScreen(screenId)
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isExpanded) {
                Column {
                    if (!stats.isPro) {
                        BannerAdView()
                    }
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.safeDrawing
                    ) {
                        BottomNavItem.entries.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                modifier = Modifier.testTag(item.testTag),
                                selected = selected,
                                onClick = { goTab(item.route, forceRoot = selected) },
                                icon = {
                                    Icon(
                                        painter = painterResource(item.iconRes),
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.primary,
                                    selectedTextColor = colors.primary,
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant,
                                    indicatorColor = colors.primary.copy(alpha = 0.18f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        val tabHost: @Composable () -> Unit = {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = if (isExpanded) {
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                } else {
                    Modifier.padding(padding)
                },
                enterTransition = { fadeIn(tween(PixiMotionMs.Fast)) },
                exitTransition = { fadeOut(tween(PixiMotionMs.Fast)) },
                popEnterTransition = { fadeIn(tween(PixiMotionMs.Fast)) },
                popExitTransition = { fadeOut(tween(PixiMotionMs.Fast)) }
            ) {
                composable(BottomNavItem.Home.route) {
                    EmbeddedBoardTab(
                        rootScreenId = BoardProductionTabs.homeRoot(stats.hasUserCreations),
                        resetTick = tabResetTicks[BottomNavItem.Home] ?: 0,
                        onNavigate = ::openBoardFromTab,
                        onLeaveRoot = { },
                        boardPipeline = boardPipeline,
                        interceptRootSystemBack = false
                    )
                }
                composable(BottomNavItem.Discover.route) {
                    EmbeddedBoardTab(
                        rootScreenId = BoardProductionTabs.DISCOVER,
                        resetTick = tabResetTicks[BottomNavItem.Discover] ?: 0,
                        onNavigate = ::openBoardFromTab,
                        onLeaveRoot = { goTab(BottomNavItem.Home.route) },
                        boardPipeline = boardPipeline,
                        interceptRootSystemBack = true
                    )
                }
                composable(BottomNavItem.Create.route) {
                    EmbeddedBoardTab(
                        rootScreenId = BoardProductionTabs.CREATE,
                        resetTick = tabResetTicks[BottomNavItem.Create] ?: 0,
                        onNavigate = ::openBoardFromTab,
                        onLeaveRoot = { goTab(BottomNavItem.Home.route) },
                        boardPipeline = boardPipeline,
                        interceptRootSystemBack = true
                    )
                }
                composable(BottomNavItem.Community.route) {
                    EmbeddedBoardTab(
                        rootScreenId = BoardProductionTabs.communityRoot(stats.kidsSpace),
                        resetTick = tabResetTicks[BottomNavItem.Community] ?: 0,
                        onNavigate = ::openBoardFromTab,
                        onLeaveRoot = { goTab(BottomNavItem.Home.route) },
                        boardPipeline = boardPipeline,
                        interceptRootSystemBack = true
                    )
                }
                composable(BottomNavItem.MyStuff.route) {
                    EmbeddedBoardTab(
                        rootScreenId = BoardProductionTabs.MY_STUFF,
                        resetTick = tabResetTicks[BottomNavItem.MyStuff] ?: 0,
                        onNavigate = ::openBoardFromTab,
                        onLeaveRoot = { goTab(BottomNavItem.Home.route) },
                        boardPipeline = boardPipeline,
                        interceptRootSystemBack = true
                    )
                }
            }
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pixiHingeSafePadding()
            ) {
                NavigationRail(containerColor = colors.surface) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentRoute == item.route
                        val label = stringResource(item.labelRes)
                        NavigationRailItem(
                            modifier = Modifier.testTag(item.testTag),
                            selected = selected,
                            onClick = { goTab(item.route, forceRoot = selected) },
                            icon = {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                                indicatorColor = colors.primary.copy(alpha = 0.18f)
                            )
                        )
                    }
                }
                AdaptiveContent(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    tabHost()
                }
            }
        } else {
            tabHost()
        }
    }
}

/**
 * In-tab board navigation with a real stack (one step per Back) and system Back parity.
 *
 * Navigate-as-back (`onNavigate` to an earlier id) pops to that id when it is already
 * on the stack instead of pushing a duplicate.
 */
@Composable
private fun EmbeddedBoardTab(
    rootScreenId: String,
    resetTick: Int = 0,
    onNavigate: (String) -> Unit,
    onLeaveRoot: () -> Unit,
    boardPipeline: com.pixijoy.ai.ui.board.BoardPipelineCallbacks,
    interceptRootSystemBack: Boolean
) {
    var stack by rememberSaveable(rootScreenId) {
        mutableStateOf(listOf(rootScreenId).ifEmpty { listOf(rootScreenId) })
    }
    LaunchedEffect(resetTick) {
        if (resetTick > 0) {
            stack = listOf(rootScreenId)
        }
    }
    // Guard against corrupted saveable state (empty list → NoSuchElementException).
    val screenId = stack.lastOrNull() ?: rootScreenId
    val canPop = stack.size > 1

    fun popOrLeave() {
        if (stack.size > 1) {
            stack = stack.dropLast(1).ifEmpty { listOf(rootScreenId) }
        } else {
            onLeaveRoot()
        }
    }

    BackHandler(enabled = canPop || interceptRootSystemBack) {
        popOrLeave()
    }

    CompositionLocalProvider(
        LocalBoardEmbeddedInMainShell provides true,
        LocalBoardCanPop provides canPop
    ) {
        fun pushBoard(next: String) {
            if (next == screenId) return
            if (BoardProductionTabs.isTabRoot(next)) {
                if (next == rootScreenId) {
                    stack = listOf(rootScreenId)
                } else {
                    onNavigate(next)
                }
            } else {
                val existing = stack.indexOf(next)
                stack = if (existing >= 0) {
                    stack.take(existing + 1)
                } else {
                    stack + next
                }
            }
        }
        CompositionLocalProvider(
            com.pixijoy.ai.ui.board.LocalBoardPipeline provides boardPipeline.inTab(::pushBoard)
        ) {
            com.pixijoy.ai.ui.board.components.BoardFadeIn(
                key = screenId,
                modifier = Modifier.fillMaxSize()
            ) {
                BoardScreenHost(
                    screenId = screenId,
                    onBack = { popOrLeave() },
                    onNavigate = { next -> pushBoard(next) }
                )
            }
        }
    }
}

private enum class BottomNavItem(
    val route: String,
    val testTag: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    Home("tab_home", TestTags.NAV_HOME, R.string.nav_home, R.drawable.pj_ic_home),
    Discover("tab_discover", TestTags.NAV_DISCOVER, R.string.nav_explore, R.drawable.pj_ic_explore),
    Create("tab_create", TestTags.NAV_CREATE, R.string.nav_create, R.drawable.pj_ic_add),
    Community("tab_community", TestTags.NAV_COMMUNITY, R.string.nav_community, R.drawable.pj_ic_community),
    MyStuff("tab_my_stuff", TestTags.NAV_MY_STUFF, R.string.nav_my_stuff, R.drawable.pj_ic_folder)
}
