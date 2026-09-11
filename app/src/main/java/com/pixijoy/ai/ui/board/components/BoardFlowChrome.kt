package com.pixijoy.ai.ui.board.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Shared in-flow chrome: always Back on non-tab screens; optional step label;
 * sticky primary Next/Continue/Generate when [onPrimary] is set.
 *
 * System / gesture back invokes the same [onBack] as the header.
 */
@Composable
fun BoardFlowChrome(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    stepLabel: String? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    /** Gradient accent only for AI Generate / Talk CTAs — Continue/Next stay solid. */
    primaryAiAccent: Boolean = false,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = BoardTheme.colors
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PixiSpacing.sm, vertical = PixiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.pj_ic_back),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colors.textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.boardHeading()
                )
                if (stepLabel != null) {
                    Text(
                        text = stepLabel,
                        style = BoardType.caption,
                        color = colors.textSecondary
                    )
                }
            }
            actions()
        }
        val bodyModifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
        Column(
            modifier = bodyModifier.padding(horizontal = PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            content = content
        )
        if (onPrimary != null && primaryLabel != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardPrimaryButton(
                    text = primaryLabel,
                    onClick = onPrimary,
                    enabled = primaryEnabled,
                    aiAccent = primaryAiAccent,
                    modifier = Modifier.testTag(
                        if (primaryAiAccent) {
                            com.pixijoy.ai.testing.TestTags.AI_GENERATE
                        } else {
                            com.pixijoy.ai.testing.TestTags.TOOL_CONTINUE
                        }
                    )
                )
                if (onSecondary != null && secondaryLabel != null) {
                    BoardSecondaryButton(text = secondaryLabel, onClick = onSecondary)
                }
            }
        }
    }
}

/** Shorter tab-root heroes so primary CTAs stay above the fold. */
object BoardTabViewport {
    const val HeroAspect = 2.2f
    const val HeroMaxDp = 148
}

/**
 * Soft bottom fade hint that more content exists (use under scrollable tab roots).
 */
@Composable
fun BoardScrollPeekOverlay(modifier: Modifier = Modifier) {
    val colors = BoardTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = PixiSpacing.sm),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = stringResource(R.string.board_scroll_for_more),
            style = BoardType.caption,
            color = colors.textSecondary
        )
    }
}
