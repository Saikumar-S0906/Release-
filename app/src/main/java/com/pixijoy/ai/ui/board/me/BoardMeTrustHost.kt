package com.pixijoy.ai.ui.board.me

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardTrySoftEdgeGate

/** Wave 6 — Profile, Pro, Settings, Safety, Privacy, Auth, Credits, Subscription, Notifications. */
@Composable
fun BoardMeTrustHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (BoardTrySoftEdgeGate(screenId, onBack, onNavigate)) return
    when {
        screenId.startsWith("b9_") -> BoardFlow9Host(screenId, onBack, onNavigate)
        screenId.startsWith("b10_") -> BoardFlow10Host(screenId, onBack, onNavigate)
        screenId.startsWith("b11_") -> BoardFlow11Host(screenId, onBack, onNavigate)
        screenId.startsWith("b20_") -> BoardFlow20Host(screenId, onBack, onNavigate)
        screenId.startsWith("b21_") -> BoardFlow21Host(screenId, onBack, onNavigate)
        screenId.startsWith("b22_") -> BoardFlow22Host(screenId, onBack, onNavigate)
        screenId.startsWith("b23_") -> BoardFlow23Host(screenId, onBack, onNavigate)
        screenId.startsWith("b24_") -> BoardFlow24Host(screenId, onBack, onNavigate)
        screenId.startsWith("b25_") -> BoardFlow25Host(screenId, onBack, onNavigate)
        else -> BoardGateScreen(
            title = stringResource(R.string.board_placeholder_title),
            body = screenId,
            primary = stringResource(R.string.cd_back),
            onPrimary = onBack
        )
    }
}
