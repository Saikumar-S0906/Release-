package com.pixijoy.ai.ui.board.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Design Bible §57 system states — PixiSkeleton / Empty / Error / Loading
 * mapped to Board kits (see docs/BOARD_COMPONENT_MAP.md).
 */

/** Skeleton placeholder block (Bible §22 — prefer over full-screen spinner). */
@Composable
fun BoardSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val loading = stringResource(R.string.board_loading)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(radii.sm))
            .background(colors.surfaceMuted)
            .semantics { contentDescription = loading }
    )
}

@Composable
fun BoardSkeletonCard(modifier: Modifier = Modifier) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surface)
            .padding(PixiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        BoardSkeleton(height = 120.dp)
        BoardSkeleton(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
        BoardSkeleton(modifier = Modifier.fillMaxWidth(0.5f), height = 12.dp)
    }
}

/**
 * Intentional empty collection (Bible §23).
 * Explains why empty + one primary CTA; optional B3 empty hero.
 */
@Composable
fun BoardEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    ctaTestTag: String? = null,
    @androidx.annotation.DrawableRes heroRes: Int? = BoardFlowArt.aiEmpty
) {
    val colors = BoardTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PixiSpacing.xl)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        if (heroRes != null) {
            BoardHeroImage(resId = heroRes, aspectRatio = 16f / 9f)
            Spacer(modifier = Modifier.height(PixiSpacing.md))
        }
        Text(
            text = title,
            modifier = Modifier.boardHeading(),
            style = BoardType.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            style = BoardType.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (ctaLabel != null && onCta != null) {
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(
                text = ctaLabel,
                onClick = onCta,
                modifier = if (ctaTestTag != null) Modifier.testTag(ctaTestTag) else Modifier
            )
        }
    }
}

/** Human-readable error + Retry (Bible §24 — never expose HTTP codes). */
@Composable
fun BoardErrorState(
    title: String = stringResource(R.string.board_error_title),
    body: String = stringResource(R.string.board_error_body),
    modifier: Modifier = Modifier,
    retryLabel: String = stringResource(R.string.board_error_retry),
    onRetry: (() -> Unit)? = null
) {
    val colors = BoardTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PixiSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        Text(
            text = title,
            style = BoardType.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            style = BoardType.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(text = retryLabel, onClick = onRetry)
        }
    }
}

/** Compact inline loading (Bible PixiLoading). */
@Composable
fun BoardLoadingIndicator(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val colors = BoardTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = colors.primary,
            strokeWidth = 3.dp
        )
        if (label != null) {
            Text(text = label, style = BoardType.caption, color = colors.textSecondary)
        }
    }
}
