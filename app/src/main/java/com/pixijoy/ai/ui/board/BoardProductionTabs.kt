package com.pixijoy.ai.ui.board

import androidx.compose.runtime.compositionLocalOf
import com.pixijoy.ai.ui.board.components.BoardShellTab

/**
 * Canonical production tab → board screenId map.
 * Bible chrome labels: Home · Explore · Create · Community · My Stuff
 * (Discover destination = Explore tab; Profile destination = My Stuff tab).
 */
object BoardProductionTabs {
    const val HOME = "b2_home"
    /** Empty / first-creation Home (C00) when the user has no creations yet. */
    const val HOME_EMPTY = "b_c00_empty"
    const val CREATE = "b_create_hub"
    const val DISCOVER = "b2_discover"
    const val COMMUNITY = "b27_feed"
    /** Kids Space blocks public Community; show this instead of [COMMUNITY]. */
    const val COMMUNITY_KIDS_BLOCKED = "b27_kids_blocked"
    const val MY_STUFF = "b18_browse"

    /** @deprecated Use [DISCOVER] (Explore chrome label). */
    const val EXPLORE = DISCOVER

    fun homeRoot(hasUserCreations: Boolean): String =
        if (hasUserCreations) HOME else HOME_EMPTY

    fun communityRoot(kidsSpace: Boolean): String =
        if (kidsSpace) COMMUNITY_KIDS_BLOCKED else COMMUNITY

    fun screenIdFor(tab: BoardShellTab): String = when (tab) {
        BoardShellTab.Home -> HOME
        BoardShellTab.Discover -> DISCOVER
        BoardShellTab.Create -> CREATE
        BoardShellTab.Community -> COMMUNITY
        BoardShellTab.MyStuff -> MY_STUFF
    }

    fun tabForRoot(screenId: String): BoardShellTab? = when (screenId) {
        HOME, HOME_EMPTY -> BoardShellTab.Home
        DISCOVER -> BoardShellTab.Discover
        CREATE -> BoardShellTab.Create
        COMMUNITY, COMMUNITY_KIDS_BLOCKED -> BoardShellTab.Community
        MY_STUFF -> BoardShellTab.MyStuff
        else -> null
    }

    fun isTabRoot(screenId: String): Boolean = tabForRoot(screenId) != null

    fun tabHandler(onNavigate: (String) -> Unit): (BoardShellTab) -> Unit = { tab ->
        onNavigate(screenIdFor(tab))
    }
}

/**
 * When true, [com.pixijoy.ai.ui.board.components.BoardMainShell] skips its own chrome
 * because [com.pixijoy.ai.ui.MainScaffold] already owns the tab bar / rail.
 */
val LocalBoardEmbeddedInMainShell = compositionLocalOf { false }

/**
 * True when the current board host can pop an in-tab / onboard step (or Nav back).
 * Tab roots in [MainScaffold] set this false so misleading Back chrome can hide.
 */
val LocalBoardCanPop = compositionLocalOf { true }
