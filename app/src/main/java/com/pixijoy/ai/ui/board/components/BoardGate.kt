package com.pixijoy.ai.ui.board.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

enum class BoardEdgeKind {
    Offline,
    LowCredits,
    Safety
}

/** Parse soft-edge kind from screen id suffixes used across board hosts. */
fun boardEdgeKindOrNull(screenId: String): BoardEdgeKind? = when {
    screenId.endsWith("_offline") -> BoardEdgeKind.Offline
    screenId.endsWith("_low_credits") -> BoardEdgeKind.LowCredits
    screenId.endsWith("_safety") -> BoardEdgeKind.Safety
    else -> null
}

/**
 * Shared SoftEdge routing for library / me / social (and any host that opts in).
 * Returns true when [screenId] was handled as an edge gate.
 */
@Composable
fun BoardTrySoftEdgeGate(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    retryScreenId: String? = null,
    chooseScreenId: String? = null,
    modifier: Modifier = Modifier
): Boolean {
    val kind = boardEdgeKindOrNull(screenId) ?: return false
    val stem = screenId
        .removeSuffix("_offline")
        .removeSuffix("_low_credits")
        .removeSuffix("_safety")
    val retry = retryScreenId ?: defaultSoftEdgeRetry(stem)
    val choose = chooseScreenId ?: retry
    BoardSoftEdgeGate(
        kind = kind,
        onPrimary = {
            when (kind) {
                BoardEdgeKind.Offline -> onNavigate(retry)
                BoardEdgeKind.LowCredits -> onNavigate("b23_dash")
                BoardEdgeKind.Safety -> onNavigate(choose)
            }
        },
        onSecondary = onBack,
        heroRes = softEdgeHero(kind),
        modifier = modifier
    )
    return true
}

@DrawableRes
fun softEdgeHero(kind: BoardEdgeKind): Int = when (kind) {
    BoardEdgeKind.Offline -> BoardFlowArt.offlineHero
    BoardEdgeKind.LowCredits -> BoardFlowArt.creditsHero
    BoardEdgeKind.Safety -> BoardFlowArt.safetyHero
}

private fun defaultSoftEdgeRetry(stem: String): String = when {
    stem.startsWith("b8") -> "b8_ready"
    stem.startsWith("b18") -> "b18_browse"
    stem.startsWith("b26") -> "b26_open"
    stem.startsWith("b_collab") -> "b_collab_select"
    stem.startsWith("b9") -> "b9_open"
    stem.startsWith("b10") -> "b10_discover"
    stem.startsWith("b11") -> "b11_hub"
    stem.startsWith("b12") -> "b12_intro"
    stem.startsWith("b20") -> "b20_input"
    stem.startsWith("b21") -> "b21_hub"
    stem.startsWith("b22") -> "b22_welcome"
    stem.startsWith("b23") -> "b23_dash"
    stem.startsWith("b24") -> "b24_plans"
    stem.startsWith("b25") -> "b25_hub"
    stem.startsWith("b27") -> "b27_feed"
    stem.startsWith("b29") -> "b29_home"
    stem.startsWith("b30") -> "b30_home"
    stem.startsWith("b4") -> "b4_analyzing"
    stem.startsWith("b19") -> "b19_start"
    else -> stem
}

/**
 * Product-weight recovery gate for offline / credits / safety.
 * Includes an honest soft-preview tip so walkthroughs stay trustworthy.
 */
@Composable
fun BoardSoftEdgeGate(
    kind: BoardEdgeKind,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    secondary: String = stringResource(R.string.cd_back),
    @DrawableRes heroRes: Int? = softEdgeHero(kind),
    modifier: Modifier = Modifier
) {
    val title: String
    val body: String
    val primary: String
    when (kind) {
        BoardEdgeKind.Offline -> {
            title = stringResource(R.string.board_f4_offline_title)
            body = stringResource(R.string.board_ct_offline_body)
            primary = stringResource(R.string.board_f3_try_again)
        }
        BoardEdgeKind.LowCredits -> {
            title = stringResource(R.string.board_f19_low_credits_title)
            body = stringResource(R.string.board_f19_low_credits_body)
            primary = stringResource(R.string.board_f19_get_credits)
        }
        BoardEdgeKind.Safety -> {
            title = stringResource(R.string.board_f4_safety_title)
            body = stringResource(R.string.board_f4_safety_body)
            primary = stringResource(R.string.board_f3_try_another)
        }
    }
    BoardGateScreen(
        title = title,
        body = body,
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        tipTitle = null,
        tipBody = null,
        heroRes = heroRes,
        modifier = modifier
    )
}

/**
 * Compact SoftEdge recovery chips during processing.
 * Product default: hidden ([BoardFeatureFlags.showSoftEdgeDemoChips]).
 * Real offline / credits / safety use [BoardSoftEdgeGate] when criteria are met.
 */
@Composable
fun BoardEdgeActions(
    onOffline: () -> Unit,
    onLowCredits: (() -> Unit)? = null,
    onSafety: (() -> Unit)? = null,
    darkSurface: Boolean = false,
    modifier: Modifier = Modifier,
    showCaption: Boolean = true
) {
    if (!com.pixijoy.ai.ui.board.BoardFeatureFlags.showSoftEdgeDemoChips) return
    val colors = BoardTheme.colors
    val captionColor = if (darkSurface) Color.White.copy(0.7f) else colors.textSecondary
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        if (showCaption) {
            Text(
                stringResource(R.string.board_soft_edge_caption),
                style = BoardType.cta.copy(fontWeight = FontWeight.SemiBold),
                color = if (darkSurface) Color.White.copy(0.85f) else colors.textPrimary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardEdgeChip(
                icon = R.drawable.pj_ic_cloud_off,
                label = stringResource(R.string.board_edge_offline_action),
                onClick = onOffline,
                darkSurface = darkSurface,
                modifier = Modifier.weight(1f)
            )
            if (onLowCredits != null) {
                BoardEdgeChip(
                    sparkle = true,
                    label = stringResource(R.string.board_edge_credits_action),
                    onClick = onLowCredits,
                    darkSurface = darkSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (onSafety != null) {
            BoardEdgeChip(
                icon = R.drawable.pj_ic_shield,
                label = stringResource(R.string.board_edge_safety_action),
                onClick = onSafety,
                darkSurface = darkSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showCaption) {
            Text(
                stringResource(R.string.board_soft_preview_tip),
                style = BoardType.body,
                color = captionColor
            )
        }
    }
}

@Composable
private fun BoardEdgeChip(
    label: String,
    onClick: () -> Unit,
    darkSurface: Boolean,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    sparkle: Boolean = false
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val bg = if (darkSurface) Color.White.copy(0.08f) else colors.surfaceMuted
    val fg = if (darkSurface) Color.White else colors.textPrimary
    val stroke = if (darkSurface) Color.White.copy(0.2f) else colors.border
    BoardPressable(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(radii.md))
            .border(1.dp, stroke, RoundedCornerShape(radii.md))
            .background(bg)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PixiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            when {
                sparkle -> BoardAiSparkleIcon(modifier = Modifier.size(18.dp), tint = colors.primary)
                icon != null -> Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                color = fg,
                style = BoardType.caption.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

/** Full-screen gate: title, body, optional hero, primary/secondary, optional tip, trust. */
@Composable
fun BoardGateScreen(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    tipTitle: String? = null,
    tipBody: String? = null,
    @DrawableRes heroRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                BoardTitleText(
                    text = title,
                    modifier = Modifier.boardHeading()
                )
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                BoardBodyText(text = body)
                if (heroRes != null) {
                    Spacer(modifier = Modifier.height(PixiSpacing.lg))
                    BoardHeroImage(resId = heroRes, aspectRatio = 16f / 9f)
                }
                if (tipTitle != null && tipBody != null) {
                    Spacer(modifier = Modifier.height(PixiSpacing.lg))
                    BoardTipCallout(title = tipTitle, body = tipBody)
                }
            }
            Column {
                BoardPrimaryButton(text = primary, onClick = onPrimary)
                if (secondary != null && onSecondary != null) {
                    Spacer(modifier = Modifier.height(PixiSpacing.sm))
                    BoardSecondaryButton(text = secondary, onClick = onSecondary)
                }
                BoardTrustFooter()
            }
        }
    }
}

@Composable
fun BoardPhotoPlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = BoardTheme.colors.primary.copy(alpha = 0.28f)
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(radii.card))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.textSecondary, textAlign = TextAlign.Center, style = BoardType.caption)
    }
}

@Composable
fun BoardProgressBlock(
    title: String,
    steps: List<String>,
    progressHint: String,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.processingSurface)
            .padding(PixiSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardAiSparkleIcon(tint = colors.accentPink)
            BoardTitleText(text = title, color = Color.White)
        }
        Spacer(modifier = Modifier.height(PixiSpacing.lg))
        steps.forEach { step ->
            BoardBodyText(
                text = "• $step",
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PixiSpacing.xs)
            )
        }
        Spacer(modifier = Modifier.height(PixiSpacing.lg))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(BoardProgressTrackHeight)
                .clip(RoundedCornerShape(radii.pill)),
            color = colors.accentPink,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        BoardCaptionText(text = progressHint, color = Color.White.copy(alpha = 0.7f))
    }
}
