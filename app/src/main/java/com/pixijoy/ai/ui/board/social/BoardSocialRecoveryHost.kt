package com.pixijoy.ai.ui.board.social

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardTrySoftEdgeGate

/** Wave 7 — E2E Journey, Community, Offline, A11y/L10n, Lifecycle. */
@Composable
fun BoardSocialRecoveryHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (BoardTrySoftEdgeGate(screenId, onBack, onNavigate)) return
    when {
        screenId.startsWith("b12_") -> BoardFlow12Host(screenId, onBack, onNavigate)
        screenId.startsWith("b27_") -> BoardFlow27Host(screenId, onBack, onNavigate)
        screenId.startsWith("b28_") -> BoardFlow28Host(screenId, onBack, onNavigate)
        screenId.startsWith("b29_") -> BoardFlow29Host(screenId, onBack, onNavigate)
        screenId.startsWith("b30_") -> BoardFlow30Host(screenId, onBack, onNavigate)
        else -> BoardGateScreen(
            title = stringResource(R.string.board_placeholder_title),
            body = screenId,
            primary = stringResource(R.string.cd_back),
            onPrimary = onBack
        )
    }
}
