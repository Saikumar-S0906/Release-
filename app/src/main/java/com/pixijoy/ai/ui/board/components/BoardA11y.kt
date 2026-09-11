package com.pixijoy.ai.ui.board.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.LiveRegionMode

/** Hide decorative art from TalkBack (logos, collage fills next to visible labels). */
fun Modifier.boardDecorative(): Modifier =
    clearAndSetSemantics { invisibleToUser() }

fun Modifier.boardHeading(): Modifier =
    semantics { heading() }

fun Modifier.boardButtonRole(): Modifier =
    semantics { role = Role.Button }

/**
 * Progress announcement for board soft-AI tracks.
 * Keep polite so step ticks don't shout over navigation.
 */
fun Modifier.boardProgressSemantics(
    progress: Float,
    label: String,
    announce: Boolean = true
): Modifier = semantics(mergeDescendants = true) {
    val clamped = progress.coerceIn(0f, 1f)
    progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
    contentDescription = label
    stateDescription = "${(clamped * 100).toInt()}%"
    if (announce) {
        liveRegion = LiveRegionMode.Polite
    }
}

/** True when system animator duration scale is 0 (Remove animations). */
@Composable
fun rememberBoardReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}
