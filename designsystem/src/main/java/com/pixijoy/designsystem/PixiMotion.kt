package com.pixijoy.designsystem

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

const val PixiMotionEmphasizedMs = 240
const val PixiMotionStandardMs = 120

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
