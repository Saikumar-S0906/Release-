package com.pixijoy.ai.ui.board

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardEditHistoryTest {
    @Test
    fun undo_redo_stack_toggles() {
        val history = BoardEditHistory(maxDepth = 4)
        val a = BoardEditHistory.Snapshot(
            bitmap = null,
            brightness = 10f,
            contrast = 10f,
            saturation = 10f,
            warmth = 10f,
            sharpness = 10f,
            filterIndex = 0,
            aspectIndex = 1,
            textOverlay = "a"
        )
        val b = a.copy(textOverlay = "b", brightness = 12f)
        history.push(a)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        val undone = history.undo(b)
        assertTrue(undone?.textOverlay == "a")
        assertTrue(history.canRedo)
        val redone = history.redo(undone!!)
        assertTrue(redone?.textOverlay == "b")
    }
}
