package com.pixijoy.ai.ui.session

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity

/**
 * Typing a slider value hides the IME. On this device that hide arrives as Back
 * and would pop the editor. [SLIDER_IME_DISMISS_GUARD_MS] covers that extra Back.
 */
internal const val SLIDER_IME_DISMISS_GUARD_MS = 500L

internal object SliderImeDismiss {
    @Volatile
    var guardUntilElapsedMs: Long = 0L

    fun arm(nowElapsedMs: Long) {
        guardUntilElapsedMs = nowElapsedMs + SLIDER_IME_DISMISS_GUARD_MS
    }

    fun clear() {
        guardUntilElapsedMs = 0L
    }
}

/** Leave the editor only when the keyboard is down and no slider just closed it. */
internal fun sessionBackShouldLeave(
    imeBottomPx: Int,
    dismissGuardUntilElapsedMs: Long,
    nowElapsedMs: Long
): Boolean = imeBottomPx <= 0 && nowElapsedMs >= dismissGuardUntilElapsedMs

internal fun swallowSessionBackForIme(
    imeBottomPx: Int,
    guardUntilElapsedMs: Long,
    nowElapsedMs: Long
): Boolean = !sessionBackShouldLeave(imeBottomPx, guardUntilElapsedMs, nowElapsedMs)

/** First Back while the rename field is focused clears the field. The next one dismisses. */
internal fun renameBackShouldDismiss(fieldFocused: Boolean): Boolean = !fieldFocused

internal fun sliderCommitKey(key: Key): Boolean =
    key == Key.Enter || key == Key.NumPadEnter

@Composable
internal fun rememberSessionImeBottomPx(): IntArray {
    val px = WindowInsets.ime.getBottom(LocalDensity.current)
    val box = remember { intArrayOf(0) }
    SideEffect { box[0] = px }
    return box
}
