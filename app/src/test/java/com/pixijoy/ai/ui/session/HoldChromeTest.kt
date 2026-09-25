package com.pixijoy.ai.ui.session

import com.pixijoy.ml.pack.PackId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldChromeTest {
    @Test
    fun frameInterpChip_isLockedUntilEngineBinds() {
        val locked = frameInterpNeuralChip(engineReady = false)
        assertEquals(HoldControl.ComingWithPack, locked)
        assertFalse(locked.enabled)
        val bound = frameInterpNeuralChip(engineReady = true)
        assertEquals(HoldControl.NeuralFrames, bound)
        assertTrue(bound.enabled)
    }

    @Test
    fun exportLine_staysFrameHoldUntilNeuralChipIsBound() {
        val locked = frameInterpNeuralChip(engineReady = false)
        assertEquals(HoldControl.FrameHold, slowMoExportChip(locked, resolvedMl = true))
        assertEquals(HoldControl.FrameHold, slowMoExportChip(locked, resolvedMl = false))
        val bound = frameInterpNeuralChip(engineReady = true)
        assertEquals(HoldControl.NeuralFrames, slowMoExportChip(bound, resolvedMl = true))
        assertEquals(HoldControl.FrameHold, slowMoExportChip(bound, resolvedMl = false))
    }

    @Test
    fun frameHold_staysSeparateFromTheNeuralChip() {
        val hold = frameHoldChip()
        assertEquals(HoldControl.FrameHold, hold)
        assertTrue(hold.enabled)
        assertFalse(hold == frameInterpNeuralChip(engineReady = false))
        assertFalse(hold == frameInterpNeuralChip(engineReady = true))
    }

    @Test
    fun trackToggle_isLockedUntilEngineBinds() {
        val locked = trackToggle(engineReady = false, trackActive = false)
        assertEquals(HoldControl.ComingWithPack, locked)
        assertFalse(locked.enabled)
        val clear = trackToggle(engineReady = false, trackActive = true)
        assertEquals(HoldControl.ClearTrack, clear)
        assertTrue(clear.enabled)
        val enable = trackToggle(engineReady = true, trackActive = false)
        assertEquals(HoldControl.EnableTrack, enable)
        assertTrue(enable.enabled)
    }

    @Test
    fun holdPacks_doNotSpeakDownloadedOrNeuralReady() {
        assertTrue(isHoldPack(PackId.FrameInterp))
        assertTrue(isHoldPack(PackId.Track))
        assertFalse(isHoldPack(PackId.Inpaint))
        assertFalse(isHoldPack(PackId.SuperRes))
        assertEquals(HoldReadyLine.ComingWithPack, holdReadyLine(engineReady = false))
        assertEquals(HoldReadyLine.NeuralReady, holdReadyLine(engineReady = true))

        val kind = packTalkBackKind(fileReady = true, engineReady = false, holdPack = true)
        assertEquals(PackTalkBackKind.ComingWithPack, kind)
        val phrase = packTalkBackPhrase(
            kind,
            downloaded = "Downloaded",
            readyToApply = "Ready to apply",
            comingWithPack = "Coming with pack"
        )
        val spoken = checkNotNull(phrase)
        assertEquals("Coming with pack", spoken)
        assertFalse(spoken.contains("Downloaded"))
        assertFalse(spoken.contains("Neural ready"))
        assertFalse(spoken.contains("neural", ignoreCase = true))

        assertEquals(
            PackTalkBackKind.Downloaded,
            packTalkBackKind(fileReady = true, engineReady = false, holdPack = false)
        )
        assertEquals(
            PackTalkBackKind.ReadyToApply,
            packTalkBackKind(fileReady = true, engineReady = true, holdPack = true)
        )
    }

    @Test
    fun holdCopy_matchesTheLockedPhrase() {
        assertEquals("Coming with pack", stringRes("studio_hold_coming"))
        assertEquals("Frame hold", stringRes("studio_slowmo_heuristic"))
        assertFalse(stringRes("studio_slowmo_heuristic").contains("neural", ignoreCase = true))
        val hint = stringRes("studio_movie_d10_hint")
        assertTrue(hint.contains("holds frames"))
        assertTrue(hint.contains("coming with the pack"))
        assertNoHostedClaim(hint)
        val track = stringRes("studio_track_hint")
        assertTrue(track.contains("coming with the pack"))
        assertTrue(track.contains("No motion follow"))
        assertNoHostedClaim(track)
        val ready = stringRes("studio_pack_ready_no_engine")
        assertTrue(ready.contains("Coming with pack"))
        assertTrue(ready.contains("Not neural"))
        assertNoHostedClaim(ready)
        assertFalse(stringRes("studio_slowmo_ml").contains("Coming with pack"))
    }

    private fun assertNoHostedClaim(copy: String) {
        assertFalse(copy.contains("Downloaded"))
        assertFalse(copy.contains("Download"))
        assertFalse(copy.contains("Neural ready"))
    }

    private fun stringRes(name: String): String {
        val xml = File("src/main/res/values/strings.xml").readText()
        val raw = Regex("""<string name="$name">(.*?)</string>""").find(xml)?.groupValues?.get(1)
            ?: error("missing string $name")
        return raw.replace("\\'", "'")
    }
}
