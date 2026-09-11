package com.pixijoy.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.pixijoy.ai.R
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.ui.theme.BrandPink
import com.pixijoy.ai.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@Composable
fun rememberPulseScale(
    min: Float = 0.88f,
    max: Float = 1f,
    durationMs: Int = 900
): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    ).value
}

@Composable
fun PulseBox(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    content: @Composable () -> Unit
) {
    val scale = rememberPulseScale()
    Box(
        modifier = modifier.size(size * scale),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun ScaleInContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(300)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
        ),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
    ) {
        content()
    }
}

@Composable
fun SuccessBurst(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    var show by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            show = true
            scale.snapTo(0.3f)
            scale.animateTo(1.2f, spring(dampingRatio = 0.5f, stiffness = 500f))
            scale.animateTo(1f, spring(dampingRatio = 0.7f))
            delay(600)
            show = false
            onFinished()
        }
    }

    if (show) {
        Box(
            modifier = modifier
                .size(72.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    delayMs: Int = 60,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index * delayMs).toLong().coerceAtMost(400))
        visible = true
    }
    ScaleInContent(visible = visible, modifier = modifier) {
        content()
    }
}

@Composable
fun CardExpandIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(350)) + scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(400, easing = LinearEasing)
        ),
        exit = fadeOut(tween(200))
    ) {
        content()
    }
}
