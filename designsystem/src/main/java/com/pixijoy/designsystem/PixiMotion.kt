package com.pixijoy.designsystem

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val PixiMotionEmphasizedMs = 240
const val PixiMotionStandardMs = 120
const val PixiMotionChipMs = 160
const val PixiMotionSheetMs = 280

/** Selected chip scale bump — signature micro-motion. */
const val PixiChipSelectedScale = 1.04f

val PixiGhostThumbSize = 72.dp

@Composable
fun pixiMotionMs(emphasized: Boolean = true): Int {
    val context = LocalContext.current
    val scale = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    if (scale == 0f) return 0
    return if (emphasized) PixiMotionEmphasizedMs else PixiMotionStandardMs
}

@Composable
fun pixiChipTween() = tween<Float>(
    durationMillis = pixiMotionMs(emphasized = false).let { if (it == 0) 0 else PixiMotionChipMs },
    easing = FastOutSlowInEasing
)

@Composable
fun pixiGroupTween() = tween<Float>(
    durationMillis = pixiMotionMs(emphasized = true),
    easing = FastOutSlowInEasing
)

@Composable
fun pixiSheetSpring() = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
