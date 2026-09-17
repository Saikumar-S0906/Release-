package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipMixTest {
    @Test
    fun constantVolumeHasNoEnvelope() {
        assertEquals(1f, ClipMix.gain(0, 4000, 1f, 0, 0), 0.001f)
        assertEquals(0.4f, ClipMix.gain(2000, 4000, 0.4f, 0, 0), 0.001f)
        assertEquals(0f, ClipMix.gain(0, 4000, 0f, 0, 0), 0.001f)
    }

    @Test
    fun fadeInRampsFromSilence() {
        assertEquals(0f, ClipMix.gain(0, 4000, 1f, 1000, 0), 0.001f)
        assertEquals(0.5f, ClipMix.gain(500, 4000, 1f, 1000, 0), 0.001f)
        assertEquals(1f, ClipMix.gain(1000, 4000, 1f, 1000, 0), 0.001f)
        assertEquals(1f, ClipMix.gain(2500, 4000, 1f, 1000, 0), 0.001f)
    }

    @Test
    fun fadeOutRampsToSilence() {
        assertEquals(1f, ClipMix.gain(0, 4000, 1f, 0, 1000), 0.001f)
        assertEquals(1f, ClipMix.gain(3000, 4000, 1f, 0, 1000), 0.001f)
        assertEquals(0.5f, ClipMix.gain(3500, 4000, 1f, 0, 1000), 0.001f)
        assertEquals(0f, ClipMix.gain(4000, 4000, 1f, 0, 1000), 0.001f)
    }

    @Test
    fun overlappingFadesShareDuration() {
        assertEquals(0f, ClipMix.gain(0, 1000, 1f, 800, 800), 0.001f)
        assertEquals(0.5f, ClipMix.gain(250, 1000, 1f, 800, 800), 0.001f)
        assertEquals(1f, ClipMix.gain(500, 1000, 1f, 800, 800), 0.001f)
        assertEquals(0f, ClipMix.gain(1000, 1000, 1f, 800, 800), 0.001f)
    }

    @Test
    fun volumeScalesTheEnvelope() {
        assertEquals(0.25f, ClipMix.gain(500, 4000, 0.5f, 1000, 0), 0.001f)
    }

    @Test
    fun duckDropsInsideDialogueAndRampsAtEdges() {
        val windows = listOf(ClipMix.Window(1000, 3000))
        assertEquals(1f, ClipMix.duck(0, windows, 0.25f, 200), 0.001f)
        assertEquals(0.25f, ClipMix.duck(2000, windows, 0.25f, 200), 0.001f)
        assertEquals(0.625f, ClipMix.duck(900, windows, 0.25f, 200), 0.02f)
        assertEquals(1f, ClipMix.duck(4000, windows, 0.25f, 200), 0.001f)
        assertEquals(1f, ClipMix.duck(2000, emptyList(), 0.25f, 200), 0.001f)
    }
}
