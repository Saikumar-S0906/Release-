package com.pixijoy.ai.ui.board.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiMotionMs
import kotlinx.coroutines.launch

/**
 * Soft press scale for board CTAs / chips (Bible §26 micro-feedback).
 * Duration ≈ [PixiMotionMs.Tap].
 */
@Composable
fun BoardPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticOnClick: Boolean = false,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = rememberBoardReduceMotion()
    val haptics = rememberBoardHaptics()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) 0.97f else 1f,
        animationSpec = if (reduceMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "board_press_scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    if (hapticOnClick) haptics.selection()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Progress track with soft tween — use instead of stepped LinearProgressIndicator jumps.
 */
@Composable
fun BoardSoftProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    darkSurface: Boolean = false
) {
    val reduceMotion = rememberBoardReduceMotion()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else PixiMotionMs.Component,
            easing = FastOutSlowInEasing
        ),
        label = "board_soft_progress"
    )
    BoardProgressTrack(
        progress = animated,
        modifier = modifier,
        darkSurface = darkSurface
    )
}

/**
 * Short fade + lift when [key] changes (screen swaps). Bible §27 screen ~250–400ms.
 */
@Composable
fun BoardFadeIn(
    key: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberBoardReduceMotion()
    if (reduceMotion) {
        Box(modifier = modifier) { content() }
        return
    }
    val alpha = remember(key) { Animatable(0f) }
    val lift = remember(key) { Animatable(8f) }
    LaunchedEffect(key) {
        alpha.snapTo(0f)
        lift.snapTo(8f)
        launch {
            alpha.animateTo(1f, tween(PixiMotionMs.Fast, easing = FastOutSlowInEasing))
        }
        lift.animateTo(0f, tween(PixiMotionMs.Fast, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = lift.value
        }
    ) {
        content()
    }
}

/** Soft reveal for processing checklist rows. */
@Composable
fun BoardStepReveal(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberBoardReduceMotion()
    val ms = if (reduceMotion) 0 else PixiMotionMs.Normal
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(ms)) + expandVertically(tween(ms)),
        exit = fadeOut(tween(ms / 2)) + shrinkVertically(tween(ms / 2))
    ) {
        content()
    }
}

/**
 * Favorite / like micro-interaction (Bible §26) — pop + selective haptic.
 */
@Composable
fun BoardFavoriteToggle(
    favored: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val colors = BoardTheme.colors
    val reduceMotion = rememberBoardReduceMotion()
    val haptics = rememberBoardHaptics()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(favored) {
        if (!reduceMotion && favored) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, tween(PixiMotionMs.Normal, easing = FastOutSlowInEasing))
        } else {
            scale.snapTo(1f)
        }
    }
    IconButton(
        onClick = {
            haptics.selection()
            onToggle()
        },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(com.pixijoy.ai.R.drawable.pj_ic_heart),
            contentDescription = contentDescription,
            tint = if (favored) colors.accentPink else colors.textSecondary,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}
