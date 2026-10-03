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
        assertEquals(1_000L + SLIDER_IME_DISMISS_GUARD_MS, SliderImeDismiss.guardUntilElapsedMs)
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
    fun doneBackStaysOnTheOpenToolAfterTheInsetIsGone() {
        SliderImeDismiss.clear()
        SliderImeDismiss.arm(10_000L)
        val hideBack = 10_000L + 1_500L
        assertTrue(hideBack < SliderImeDismiss.guardUntilElapsedMs)
        assertEquals(
            SessionBackDecision.Stay,
            decideSessionBack(imeBottomPx = 0, nowElapsedMs = hideBack, dismissGuardUntilElapsedMs = SliderImeDismiss.guardUntilElapsedMs)
        )
        assertFalse(sessionBackShouldLeave(0, SliderImeDismiss.guardUntilElapsedMs, hideBack))
        assertEquals(
            SessionBackDecision.Leave,
            decideSessionBack(0, SliderImeDismiss.guardUntilElapsedMs, SliderImeDismiss.guardUntilElapsedMs)
        )
        SliderImeDismiss.clear()
    }

    @Test
    fun keyboardBackStillHidesTheImeWhenNoSliderJustCommitted() {
        assertEquals(SessionBackDecision.StayHideIme, decideSessionBack(imeBottomPx = 420, nowElapsedMs = 5_000L, dismissGuardUntilElapsedMs = 0L))
    }


    @Test
    fun curvesEnterCommitsAndStaysOnTheOpenTool() {
        SliderImeDismiss.clear()
        var committed: String? = null
        SliderImeDismiss.bindCommit {
            committed = "0.55"
            SliderImeDismiss.arm(8_000L)
        }
        assertEquals(
            SessionBackDecision.Stay,
            resolveSessionBack(imeBottomPx = 420, nowElapsedMs = 8_000L)
        )
        assertEquals("0.55", committed)
        assertFalse(SliderImeDismiss.editorOpen)
        assertEquals(
            SessionBackDecision.Stay,
            resolveSessionBack(imeBottomPx = 0, nowElapsedMs = 8_000L + 1_500L)
        )
        assertEquals(
            SessionBackDecision.Leave,
            resolveSessionBack(imeBottomPx = 0, nowElapsedMs = SliderImeDismiss.guardUntilElapsedMs)
        )
        SliderImeDismiss.clear()
    }

    @Test
    fun exposureEnterUsesTheSameStayAsCurves() {
        SliderImeDismiss.clear()
        var committed: String? = null
        SliderImeDismiss.bindCommit {
            committed = "0.40"
            SliderImeDismiss.arm(3_000L)
        }
        assertEquals(SessionBackDecision.Stay, resolveSessionBack(imeBottomPx = 0, nowElapsedMs = 4_000L))
        assertEquals("0.40", committed)
        assertEquals(
            SessionBackDecision.Leave,
            resolveSessionBack(0, SliderImeDismiss.guardUntilElapsedMs)
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
