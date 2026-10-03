package com.pixijoy.studio.ui.session

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity

/**
 * Slider and curves share this path. Done commits the number and hides the IME.
 * On device that hide arrives as Back after the inset is already 0, and it can
 * land after a short guard (Enter on 0.4 and on curves 0.55 both left the hub).
 * [SLIDER_IME_DISMISS_GUARD_MS] holds the open tool through that Back.
 * Swallowing it must not hide the keyboard again, or a second Back escapes.
 */
internal const val SLIDER_IME_DISMISS_GUARD_MS = 2_500L

internal object SliderImeDismiss {
    @Volatile
    var guardUntilElapsedMs: Long = 0L

    @Volatile
    var editorOpen: Boolean = false

    private var commit: (() -> Unit)? = null

    fun arm(nowElapsedMs: Long) {
        guardUntilElapsedMs = nowElapsedMs + SLIDER_IME_DISMISS_GUARD_MS
    }

    /** Slider and curves bind the same field. A Back while it is open must commit, not pop. */
    fun bindCommit(action: (() -> Unit)?) {
        commit = action
        editorOpen = action != null
    }

    fun commitOpenEditor() {
        val action = commit ?: return
        commit = null
        editorOpen = false
        action()
    }

    fun clear() {
        guardUntilElapsedMs = 0L
        commit = null
        editorOpen = false
    }
}

internal enum class SessionBackDecision {
    /** IME-hide echo. Stay on the open tool and do not hide again. */
    Stay,
    /** Keyboard is still up and this was not a slider commit. Close it, stay. */
    StayHideIme,
    Leave
}

/**
 * One dismiss decision for every editor. Slider and curves both arm
 * [SliderImeDismiss] from the shared value field, then come through here.
 */
internal fun decideSessionBack(
    imeBottomPx: Int,
    nowElapsedMs: Long,
    dismissGuardUntilElapsedMs: Long
): SessionBackDecision = when {
    nowElapsedMs < dismissGuardUntilElapsedMs -> SessionBackDecision.Stay
    imeBottomPx > 0 -> SessionBackDecision.StayHideIme
    else -> SessionBackDecision.Leave
}

/** Leave the editor only when the keyboard is down and no slider just closed it. */
internal fun sessionBackShouldLeave(
    imeBottomPx: Int,
    dismissGuardUntilElapsedMs: Long,
    nowElapsedMs: Long
): Boolean = decideSessionBack(imeBottomPx, nowElapsedMs, dismissGuardUntilElapsedMs) == SessionBackDecision.Leave

internal fun swallowSessionBackForIme(
    imeBottomPx: Int,
    guardUntilElapsedMs: Long,
    nowElapsedMs: Long
): Boolean = !sessionBackShouldLeave(imeBottomPx, guardUntilElapsedMs, nowElapsedMs)

/**
 * Shared editor Back. Exposure and curves use one field: if it is open, Done commits
 * the typed number and stays. The hide-echo is covered by the guard. A later Back leaves.
 */
internal fun resolveSessionBack(imeBottomPx: Int, nowElapsedMs: Long): SessionBackDecision {
    if (nowElapsedMs < SliderImeDismiss.guardUntilElapsedMs) return SessionBackDecision.Stay
    if (SliderImeDismiss.editorOpen) {
        SliderImeDismiss.commitOpenEditor()
        return SessionBackDecision.Stay
    }
    return decideSessionBack(imeBottomPx, nowElapsedMs, dismissGuardUntilElapsedMs = 0L)
}

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
