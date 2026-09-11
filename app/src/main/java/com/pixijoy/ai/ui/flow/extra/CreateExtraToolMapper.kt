package com.pixijoy.ai.ui.flow.extra

import com.pixijoy.ai.domain.model.PhotoTool
import com.pixijoy.ai.ml.PortraitEffect

/** Pure mapping helpers for C-series tools — unit-tested. */
object CreateExtraToolMapper {
    fun needsConsent(tool: CreateExtraTool): Boolean =
        tool == CreateExtraTool.FaceSwap || tool == CreateExtraTool.People

    fun requiresNetwork(tool: CreateExtraTool): Boolean =
        tool == CreateExtraTool.Scene || tool == CreateExtraTool.Remix

    fun creditCost(tool: CreateExtraTool): Int = when (tool) {
        // Keep costs within daily pool (5) so tools remain usable; UI must match deduction.
        CreateExtraTool.RemoveBg -> 1
        CreateExtraTool.Restore -> 2
        CreateExtraTool.FaceSwap -> 3
        CreateExtraTool.Scene -> 2
        CreateExtraTool.People -> 3
        CreateExtraTool.Portrait -> 3
        CreateExtraTool.Expand -> 1
        CreateExtraTool.Talking -> 3
        CreateExtraTool.Greeting -> 1
        CreateExtraTool.Remix -> 2
        CreateExtraTool.EmptyHome -> 1
    }

    fun scenePrompt(optionIndex: Int): String = listOf(
        "sunny beach", "city night lights", "mountain landscape", "cozy cafe",
        "professional studio", "outer space nebula", "festival lights", "custom scenic backdrop"
    ).getOrElse(optionIndex) { "professional studio" }

    fun portraitEffect(optionIndex: Int): PortraitEffect = when (optionIndex % PortraitEffect.entries.size) {
        0 -> PortraitEffect.GLOW
        1 -> PortraitEffect.RELIGHT
        2 -> PortraitEffect.YOUTHFUL
        3 -> PortraitEffect.TEETH
        else -> PortraitEffect.HAIR
    }

    fun photoToolHint(tool: CreateExtraTool): PhotoTool? = when (tool) {
        CreateExtraTool.RemoveBg -> PhotoTool.BACKGROUND_REMOVE
        CreateExtraTool.Restore, CreateExtraTool.Expand, CreateExtraTool.Remix, CreateExtraTool.EmptyHome ->
            PhotoTool.ENHANCE
        CreateExtraTool.Scene -> PhotoTool.BACKGROUND_REPLACE
        else -> null
    }

    fun expandPadFractions(optionIndex: Int): Float = when (optionIndex) {
        4, 5 -> 0.18f
        1 -> 0.06f
        else -> 0.12f
    }
}
