package com.pixijoy.ai.ui.board.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardGateScreen

/** Wave 4 — Flows 13–17 stickers / move / editor / collage / export. */
@Composable
fun BoardMediaStudioHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when {
        screenId.startsWith("b13_") -> BoardFlow13Host(screenId, onBack, onNavigate)
        screenId.startsWith("b14_") -> BoardFlow14Host(screenId, onBack, onNavigate)
        screenId.startsWith("b15_") -> BoardFlow15Host(screenId, onBack, onNavigate)
        screenId.startsWith("b16_") -> BoardFlow16Host(screenId, onBack, onNavigate)
        screenId.startsWith("b17_") -> BoardFlow17Host(screenId, onBack, onNavigate)
        else -> BoardGateScreen(
            title = stringResource(R.string.board_placeholder_title),
            body = screenId,
            primary = stringResource(R.string.cd_back),
            onPrimary = onBack
        )
    }
}
