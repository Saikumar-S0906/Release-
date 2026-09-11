package com.pixijoy.ai.ui.board.ctools

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardPhase1PipelineIndexTest {
    @Test
    fun c10_upscale4x_maps_to_option_3() {
        BoardFlowC10Session.upscale4x = true
        BoardFlowC10Session.toolIndex = 13
        BoardFlowC10Session.aspectIndex = 0
        assertEquals(3, BoardFlowC10Session.pipelineOptionIndex())
    }

    @Test
    fun c10_square_aspect_maps_to_pad_when_not_4x() {
        BoardFlowC10Session.upscale4x = false
        BoardFlowC10Session.aspectIndex = 1
        BoardFlowC10Session.toolIndex = 13
        assertEquals(4, BoardFlowC10Session.pipelineOptionIndex())
    }

    @Test
    fun c10_expand_tool_maps_to_expand_when_2x_free() {
        BoardFlowC10Session.upscale4x = false
        BoardFlowC10Session.aspectIndex = 0
        BoardFlowC10Session.toolIndex = 13
        assertEquals(1, BoardFlowC10Session.pipelineOptionIndex())
    }
}
