package com.pixijoy.ai.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.create.BoardCreateHub
import com.pixijoy.ai.ui.board.create.BoardCreateRematchHost
import com.pixijoy.ai.ui.board.create.BoardF19Host
import com.pixijoy.ai.ui.board.ctools.BoardCTool
import com.pixijoy.ai.ui.board.ctools.BoardCToolsHost
import com.pixijoy.ai.ui.board.ctools.BoardFlowC04Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC05Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC06Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC07Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC08Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC09Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC10Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC11Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC12Host
import com.pixijoy.ai.ui.board.ctools.BoardFlowC14Host
import com.pixijoy.ai.ui.board.home.BoardC00Host
import com.pixijoy.ai.ui.board.home.BoardFlow2Host
import com.pixijoy.ai.ui.board.library.BoardLibraryHost
import com.pixijoy.ai.ui.board.me.BoardMeTrustHost
import com.pixijoy.ai.ui.board.media.BoardFlow3Host
import com.pixijoy.ai.ui.board.media.BoardFlow4Host
import com.pixijoy.ai.ui.board.media.BoardMediaStudioHost
import com.pixijoy.ai.ui.board.onboarding.BoardFlow1Host
import com.pixijoy.ai.ui.board.social.BoardSocialRecoveryHost
import com.pixijoy.ai.ui.board.theme.BoardColors
import com.pixijoy.ai.ui.board.theme.BoardThemeProvider
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Dispatches board screen IDs to flow hosts. Unbuilt screens show a placeholder.
 * Theme tracks [MaterialTheme] (PixiJoyTheme dark default) so shell + boards share Identity canvas.
 */
@Composable
fun BoardScreenHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    LaunchedEffect(Unit) { BoardMemory.hydrate(context) }
    LaunchedEffect(screenId) { BoardMemory.rememberTool(context, screenId) }
    BoardThemeProvider(
        colors = if (dark) BoardColors.dark() else BoardColors.light(),
        darkTheme = dark
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.boardScreen(screenId))
        ) {
            com.pixijoy.ai.ui.board.components.BoardFadeIn(key = screenId) {
                BoardScreenHostContent(screenId, onBack, onNavigate)
            }
        }
    }
}

@Composable
private fun BoardScreenHostContent(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when {
        screenId.startsWith("b1_") -> BoardFlow1Host(screenId, onBack, onNavigate)
        screenId.startsWith("b2_") -> BoardFlow2Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c00") -> BoardC00Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c04") -> BoardFlowC04Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c05") -> BoardFlowC05Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c06") -> BoardFlowC06Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c07") -> BoardFlowC07Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c08") -> BoardFlowC08Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c09") -> BoardFlowC09Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c10") -> BoardFlowC10Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c11") -> BoardFlowC11Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c12") -> BoardFlowC12Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_c14") -> BoardFlowC14Host(screenId, onBack, onNavigate)
        BoardCTool.fromScreenId(screenId) != null -> BoardCToolsHost(screenId, onBack, onNavigate)
        screenId.startsWith("b3_") -> BoardFlow3Host(screenId, onBack, onNavigate)
        screenId.startsWith("b4_") -> BoardFlow4Host(screenId, onBack, onNavigate)
        screenId.startsWith("b13_") ||
            screenId.startsWith("b14_") ||
            screenId.startsWith("b15_") ||
            screenId.startsWith("b16_") ||
            screenId.startsWith("b17_") -> BoardMediaStudioHost(screenId, onBack, onNavigate)
        screenId.startsWith("b8_") ||
            screenId.startsWith("b18_") ||
            screenId.startsWith("b26_") ||
            screenId.startsWith("b_collab") ||
            screenId == "b0_hub" -> BoardLibraryHost(screenId, onBack, onNavigate)
        screenId.startsWith("b9_") ||
            screenId.startsWith("b10_") ||
            screenId.startsWith("b11_") ||
            screenId.startsWith("b20_") ||
            screenId.startsWith("b21_") ||
            screenId.startsWith("b22_") ||
            screenId.startsWith("b23_") ||
            screenId.startsWith("b24_") ||
            screenId.startsWith("b25_") -> BoardMeTrustHost(screenId, onBack, onNavigate)
        screenId.startsWith("b12_") ||
            screenId.startsWith("b27_") ||
            screenId.startsWith("b28_") ||
            screenId.startsWith("b29_") ||
            screenId.startsWith("b30_") -> BoardSocialRecoveryHost(screenId, onBack, onNavigate)
        screenId.startsWith("b19_") -> BoardF19Host(screenId, onBack, onNavigate)
        screenId == "b_create_hub" -> BoardCreateHub(onBack = onBack, onNavigate = onNavigate)
        screenId.startsWith("b_pack") -> com.pixijoy.ai.ui.board.create.BoardCompetePackHost(screenId, onBack, onNavigate)
        screenId.startsWith("b5_") ||
            screenId.startsWith("b6_") ||
            screenId.startsWith("b7_") ||
            screenId.startsWith("b_diff") ||
            screenId == "b_different" -> BoardCreateRematchHost(screenId, onBack, onNavigate)
        else -> BoardPlaceholderScreen(screenId = screenId, onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardPlaceholderScreen(screenId: String, onBack: () -> Unit) {
    val entry = BoardUiCatalog.find(screenId)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(entry?.title ?: screenId) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardTipCallout(
                title = stringResource(R.string.board_placeholder_title),
                body = stringResource(
                    R.string.board_placeholder_body,
                    entry?.id ?: screenId,
                    entry?.status?.releaseLabel ?: "Pending"
                )
            )
            BoardPrimaryButton(text = stringResource(R.string.cd_back), onClick = onBack)
            BoardTrustFooter()
        }
    }
}
