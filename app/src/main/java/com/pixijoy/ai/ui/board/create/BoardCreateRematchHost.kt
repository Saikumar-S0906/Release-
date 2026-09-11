package com.pixijoy.ai.ui.board.create

import androidx.compose.runtime.Composable

/**
 * REMATCH create intents: Beautiful (5), Funny (6), Tell (7), Different (REMATCH_EXTRA).
 */
@Composable
fun BoardCreateRematchHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when {
        screenId.startsWith("b5_") -> BoardFlow5Host(screenId, onBack, onNavigate)
        screenId.startsWith("b6_") -> BoardFlow6Host(screenId, onBack, onNavigate)
        screenId.startsWith("b7_") -> BoardFlow7Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_diff") || screenId == "b_different" ->
            BoardFlowDifferentHost(screenId, onBack, onNavigate)
        else -> BoardFlow5Host("b5_choose", onBack, onNavigate)
    }
}
