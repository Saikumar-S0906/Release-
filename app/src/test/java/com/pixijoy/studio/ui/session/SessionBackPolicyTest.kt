package com.pixijoy.studio.ui.session

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBackPolicyTest {
    @Test
    fun sessionBackStaysWhileTheKeyboardIsUp() {
        assertFalse(sessionBackShouldLeave(imeBottomPx = 420, dismissGuardUntilElapsedMs = 0L, nowElapsedMs = 1_000L))
        assertTrue(swallowSessionBackForIme(420, guardUntilElapsedMs = 0L, nowElapsedMs = 1_000L))
    }

    @Test
    fun sessionBackStaysDuringTheSliderDismissGuard() {
        assertFalse(sessionBackShouldLeave(imeBottomPx = 0, dismissGuardUntilElapsedMs = 1_500L, nowElapsedMs = 1_200L))
        assertTrue(sessionBackShouldLeave(imeBottomPx = 0, dismissGuardUntilElapsedMs = 1_500L, nowElapsedMs = 1_500L))
    }

    @Test
    fun sessionBackLeavesWhenTheKeyboardIsDownAndTheGuardHasPassed() {
        assertTrue(sessionBackShouldLeave(imeBottomPx = 0, dismissGuardUntilElapsedMs = 0L, nowElapsedMs = 1_000L))
        assertFalse(swallowSessionBackForIme(0, guardUntilElapsedMs = 0L, nowElapsedMs = 1_000L))
    }

    @Test
    fun sliderArmCoversTheFollowingBack() {
        SliderImeDismiss.clear()
        SliderImeDismiss.arm(1_000L)
        assertEquals(1_500L, SliderImeDismiss.guardUntilElapsedMs)
        assertTrue(
            swallowSessionBackForIme(
                imeBottomPx = 0,
                guardUntilElapsedMs = SliderImeDismiss.guardUntilElapsedMs,
                nowElapsedMs = 1_200L
            )
        )
        SliderImeDismiss.clear()
    }

    @Test
    fun renameBackClearsAFocusedFieldBeforeItDismisses() {
        assertFalse(renameBackShouldDismiss(fieldFocused = true))
        assertTrue(renameBackShouldDismiss(fieldFocused = false))
    }

    @Test
    fun enterCommitsASliderValue() {
        assertTrue(sliderCommitKey(Key.Enter))
        assertTrue(sliderCommitKey(Key.NumPadEnter))
        assertFalse(sliderCommitKey(Key.Back))
    }
}
