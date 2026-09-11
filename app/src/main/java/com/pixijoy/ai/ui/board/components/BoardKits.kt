package com.pixijoy.ai.ui.board.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.BoardFeatureFlags
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiMotionMs
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Board kit K-TRUST — footer strip on most boards. */
private val TrustFooterLabels = listOf(
    R.string.board_trust_private,
    R.string.board_trust_everyone,
    R.string.board_trust_ai,
    R.string.board_trust_happiness
)

@Composable
fun BoardTrustFooter(modifier: Modifier = Modifier) {
    val colors = BoardTheme.colors
    val combined = stringResource(R.string.board_a11y_trust)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PixiSpacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = combined
            },
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TrustFooterLabels.forEach { res ->
            Text(
                text = stringResource(res),
                style = BoardType.caption,
                color = colors.textSecondary
            )
        }
    }
}

/** Board kit K-TIPS — instruction / pro tip callout (dark-friendly). */
@Composable
fun BoardTipCallout(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .padding(PixiSpacing.lg)
    ) {
        Text(
            text = title,
            style = BoardType.title,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(PixiSpacing.xs))
        Text(
            text = body,
            style = BoardType.body,
            color = colors.textSecondary
        )
    }
}

/** Board kit K-CREDIT — cost chip before generate. */
@Composable
fun BoardCreditChip(
    credits: Int,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(radii.pill))
            .background(colors.surfaceMuted)
            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
    ) {
        BoardAiSparkleIcon(modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.board_credit_chip, credits),
            style = BoardType.caption,
            color = colors.textPrimary
        )
    }
}

/** AI sparkle mark — only on AI paths (Identity / Bible). */
@Composable
fun BoardAiSparkleIcon(
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val colors = BoardTheme.colors
    Icon(
        painter = painterResource(R.drawable.pj_ic_sparkles),
        contentDescription = null,
        modifier = modifier.size(20.dp),
        tint = tint ?: colors.accentPink
    )
}

/** Primary board CTA — pill. Set [aiAccent] for purple→pink AI gradient (Generate / Talk). */
@Composable
fun BoardPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    aiAccent: Boolean = false
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val bgModifier = when {
        !enabled -> Modifier.background(colors.surfaceMuted)
        aiAccent -> Modifier.background(colors.aiGradient)
        else -> Modifier.background(colors.primary)
    }
    BoardPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(radii.pill))
            .then(bgModifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.7f }
    ) {
        Text(
            text = text,
            color = if (enabled) colors.onPrimary else colors.textSecondary,
            style = BoardType.cta
        )
    }
}

@Composable
fun BoardSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(radii.pill))
            .background(colors.surfaceMuted)
            .graphicsLayer { alpha = if (enabled) 1f else 0.7f }
    ) {
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
            style = BoardType.cta
        )
    }
}

/** Low-emphasis action (Bible tertiary). */
@Composable
fun BoardTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = BoardTheme.colors
    BoardPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .padding(horizontal = PixiSpacing.sm)
            .graphicsLayer { alpha = if (enabled) 1f else 0.6f }
    ) {
        Text(
            text = text,
            color = if (enabled) colors.primary else colors.textSecondary,
            style = BoardType.cta
        )
    }
}

/** Irreversible action — solid danger fill. */
@Composable
fun BoardDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(radii.pill))
            .background(if (enabled) colors.danger else colors.surfaceMuted)
            .graphicsLayer { alpha = if (enabled) 1f else 0.7f }
    ) {
        Text(
            text = text,
            color = if (enabled) colors.onPrimary else colors.textSecondary,
            style = BoardType.cta
        )
    }
}

/** Credit chip + generate CTA used on create / tool adjust screens. */
@Composable
fun BoardGenerateBlock(
    credits: Int,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
    generateLabel: String = stringResource(R.string.board_f19_generate),
    enabled: Boolean = true,
    showTrust: Boolean = false,
    showDemoCaption: Boolean = BoardFeatureFlags.showDemoCaption
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        BoardCreditChip(credits = credits)
        BoardPrimaryButton(
            text = generateLabel,
            onClick = onGenerate,
            enabled = enabled,
            aiAccent = true,
            modifier = Modifier.testTag(com.pixijoy.ai.testing.TestTags.AI_GENERATE)
        )
        if (showDemoCaption) {
            BoardCaptionText(text = stringResource(R.string.board_soft_preview_title))
            BoardCaptionText(text = stringResource(R.string.board_soft_preview_tip))
        }
        if (showTrust) {
            BoardTrustFooter()
        }
    }
}

/** Shared back + title chrome used across board hosts. */
@Composable
fun BoardScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    lightContent: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = BoardTheme.colors
    val fg = if (lightContent) Color.White else colors.textPrimary
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.pj_ic_back),
                contentDescription = stringResource(R.string.cd_back),
                tint = fg
            )
        }
        BoardTitleText(
            text = title,
            modifier = Modifier
                .weight(1f)
                .boardHeading(),
            color = fg
        )
        actions()
    }
}

/** Screen / section title — [BoardType.title]. */
@Composable
fun BoardTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = BoardType.title,
        color = color ?: BoardTheme.colors.textPrimary,
        textAlign = textAlign
    )
}

/** Supporting copy — [BoardType.body]. */
@Composable
fun BoardBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = BoardType.body,
        color = color ?: BoardTheme.colors.textSecondary,
        textAlign = textAlign
    )
}

/** Branded filter / segment pill — 40dp min, one look across tabs. */
@Composable
fun BoardFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val reduceMotion = rememberBoardReduceMotion()
    val ms = if (reduceMotion) 0 else PixiMotionMs.Tap
    val bg by animateColorAsState(
        if (selected) colors.primary else colors.surfaceMuted,
        animationSpec = tween(ms),
        label = "pill_bg"
    )
    val fg by animateColorAsState(
        if (selected) colors.onPrimary else colors.textSecondary,
        animationSpec = tween(ms),
        label = "pill_fg"
    )
    BoardPressable(
        onClick = onClick,
        hapticOnClick = true,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(radii.pill))
            .background(bg)
            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
    ) {
        Text(
            text = text,
            style = BoardType.caption,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            textAlign = TextAlign.Center
        )
    }
}

/** Chips / meta / trust — [BoardType.caption]. */
@Composable
fun BoardCaptionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = BoardType.caption,
        color = color ?: BoardTheme.colors.textSecondary,
        textAlign = textAlign
    )
}

/** Canonical progress track height for board processing UIs. */
val BoardProgressTrackHeight: Dp = 6.dp

@Composable
fun BoardProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    darkSurface: Boolean = false,
    label: String = stringResource(R.string.board_a11y_progress)
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val clamped = progress.coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { clamped },
        modifier = modifier
            .fillMaxWidth()
            .height(BoardProgressTrackHeight)
            .clip(RoundedCornerShape(radii.pill))
            .boardProgressSemantics(clamped, label),
        color = if (darkSurface) colors.primary else colors.primary,
        trackColor = if (darkSurface) Color.White.copy(0.15f) else colors.surfaceMuted
    )
}

/** Onboarding page dots — board Flow 1 pagination. */
@Composable
fun BoardPageDots(
    total: Int,
    selected: Int,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val selectedLabel = stringResource(R.string.cd_selected)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val active = i == selected
            val width by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (active) 20.dp else 8.dp,
                animationSpec = androidx.compose.animation.core.tween(220),
                label = "board_dot_width_$i"
            )
            val tint by androidx.compose.animation.animateColorAsState(
                targetValue = if (active) colors.primary else colors.surfaceMuted,
                animationSpec = androidx.compose.animation.core.tween(220),
                label = "board_dot_color_$i"
            )
            val pageLabel = stringResource(R.string.board_a11y_page_of, i + 1, total)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width, height = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tint)
                    .semantics {
                        contentDescription = pageLabel
                        if (active) {
                            stateDescription = selectedLabel
                        }
                    }
            )
        }
    }
}

/** Outlined auth / secondary method button (Google / Apple / Email). */
@Composable
fun BoardOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardPressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(radii.pill))
            .background(colors.surfaceMuted)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(radii.pill))
            .background(colors.surface)
            .graphicsLayer { alpha = if (enabled) 1f else 0.7f }
    ) {
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
            style = BoardType.cta.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun BoardGradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val headerBrush = remember(colors.primary, colors.accentPink) {
        Brush.verticalGradient(
            listOf(colors.primary.copy(alpha = 0.28f), colors.accentPink.copy(alpha = 0.12f))
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(headerBrush)
            .padding(PixiSpacing.xl)
    ) {
        BoardTitleText(text = title, color = colors.textPrimary)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardBodyText(text = subtitle, color = colors.textSecondary)
        }
    }
}

/**
 * Soft breathing pulse for processing accents (demo motion only).
 * Pass [active] = false on idle hubs so the infinite transition is not composed.
 * Honors system "Remove animations" via [rememberBoardReduceMotion].
 */
@Composable
fun BoardSoftPulse(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberBoardReduceMotion()
    if (!active || reduceMotion) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            content()
        }
        return
    }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "board_soft_pulse")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1100,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "board_soft_pulse_scale"
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Modern thin scrollbar + “more below” cue when content can scroll.
 */
@Composable
fun BoardScrollCue(
    canScrollForward: Boolean,
    modifier: Modifier = Modifier
) {
    if (!canScrollForward) return
    val colors = BoardTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, colors.surface.copy(alpha = 0.92f))
                    )
                )
        )
        Text(
            text = stringResource(R.string.board_scroll_for_more),
            style = BoardType.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = PixiSpacing.sm)
        )
    }
}

@Composable
fun BoardLazyScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val info = listState.layoutInfo
    val total = info.totalItemsCount
    if (total <= 1 || info.visibleItemsInfo.isEmpty()) return
    val first = info.visibleItemsInfo.first().index
    val last = info.visibleItemsInfo.last().index
    val fraction = ((first + last) / 2f) / (total - 1).coerceAtLeast(1)
    BoxWithConstraints(
        modifier = modifier
            .width(3.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(999.dp))
            .background(colors.border.copy(alpha = 0.35f))
    ) {
        val thumbH = maxHeight * 0.22f
        val travel = (maxHeight - thumbH).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .padding(top = travel * fraction)
                .fillMaxWidth()
                .height(thumbH)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.primary.copy(alpha = 0.85f))
        )
    }
}
