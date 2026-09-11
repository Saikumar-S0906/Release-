package com.pixijoy.ai.ui.board

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Actions for Flow 1 onboarding (guest session, version chalk).
 * Provided from [com.pixijoy.ai.ui.PixiJoyAppRoot] on the Onboarding route.
 */
data class BoardOnboardingActions(
    val markPriorLaunch: () -> Unit = {},
    val continueAsGuest: (onDone: () -> Unit) -> Unit = { it() },
    val dismissWhatsNew: () -> Unit = {}
)

val LocalBoardOnboardingActions = staticCompositionLocalOf { BoardOnboardingActions() }
