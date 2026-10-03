package com.pixijoy.studio.ui.session

import com.pixijoy.ml.pack.PackId

/**
 * E4 frame-interp and F1 track chrome. FrameInterp unlocks when LiteRT binds FILM;
 * Track stays unbound (no artifact). Neural wording is only [HoldControl.NeuralFrames].
 * Frame hold is a separate control and is never that chip.
 */
internal sealed class HoldControl {
    abstract val enabled: Boolean

    data object ComingWithPack : HoldControl() {
        override val enabled: Boolean = false
    }

    data object NeuralFrames : HoldControl() {
        override val enabled: Boolean = true
    }

    data object FrameHold : HoldControl() {
        override val enabled: Boolean = true
    }

    data object EnableTrack : HoldControl() {
        override val enabled: Boolean = true
    }

    data object ClearTrack : HoldControl() {
        override val enabled: Boolean = true
    }
}

internal enum class HoldReadyLine {
    ComingWithPack,
    NeuralReady
}

internal fun isHoldPack(packId: PackId): Boolean =
    packId == PackId.FrameInterp || packId == PackId.Track

/** Neural interpolation chip. Disabled until the pack engine is bound. */
internal fun frameInterpNeuralChip(engineReady: Boolean): HoldControl =
    if (engineReady) HoldControl.NeuralFrames else HoldControl.ComingWithPack

/** Classical slow-mo. Always available, and not the neural chip. */
internal fun frameHoldChip(): HoldControl = HoldControl.FrameHold

/**
 * Track toggle. A saved point can still be cleared while the engine is unbound.
 * Enabling follow waits on the pack.
 */
internal fun trackToggle(engineReady: Boolean, trackActive: Boolean): HoldControl = when {
    trackActive -> HoldControl.ClearTrack
    engineReady -> HoldControl.EnableTrack
    else -> HoldControl.ComingWithPack
}

/** Ready file on a HOLD pack is not neural until the engine is bound. */
internal fun holdReadyLine(engineReady: Boolean): HoldReadyLine =
    if (engineReady) HoldReadyLine.NeuralReady else HoldReadyLine.ComingWithPack

/**
 * Export line. "Neural frames" only when the neural chip itself is enabled
 * and the clip resolved to that engine. Otherwise the line is frame hold.
 */
internal fun slowMoExportChip(neural: HoldControl, resolvedMl: Boolean): HoldControl =
    if (neural is HoldControl.NeuralFrames && resolvedMl) {
        HoldControl.NeuralFrames
    } else {
        HoldControl.FrameHold
    }
