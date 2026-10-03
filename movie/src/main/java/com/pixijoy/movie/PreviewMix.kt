package com.pixijoy.movie

/**
 * Gains the preview composition reads on each audio buffer.
 * Export does not use this. Moving a slider updates the fields in place
 * so the player does not rebuild the composition.
 */
class PreviewMix {
    @Volatile var bed: Float = 1f
    @Volatile var voice: Float = 1f
}
