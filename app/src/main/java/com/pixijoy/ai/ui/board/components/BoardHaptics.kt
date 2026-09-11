package com.pixijoy.ai.ui.board.components

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.getSystemService

/**
 * Selective haptics (Bible §44) — success / selection only.
 * Skipped when reduce-motion or TalkBack touch exploration is on.
 */
class BoardHaptics(
    private val view: View,
    private val enabled: Boolean
) {
    fun selection() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun success() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
}

@Composable
fun rememberBoardHaptics(enabled: Boolean = true): BoardHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    val reduce = rememberBoardReduceMotion()
    val touchExploration = remember(context) {
        context.getSystemService<AccessibilityManager>()?.isTouchExplorationEnabled == true
    }
    return remember(view, enabled, reduce, touchExploration) {
        BoardHaptics(view, enabled && !reduce && !touchExploration)
    }
}
