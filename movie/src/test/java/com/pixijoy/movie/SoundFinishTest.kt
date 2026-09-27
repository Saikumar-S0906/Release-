package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundFinishTest {
    @Test
    fun stemBuses_encodeDecode() {
        val buses = StemBuses(
            dialogueMuted = true,
            musicMuted = false,
            sfxMuted = true,
            solo = StemSolo.Music
        )
        val back = StemBuses.decodeLine(buses.encodeLine().split(','))!!
        assertTrue(back.dialogueMuted)
        assertFalse(back.musicMuted)
        assertTrue(back.sfxMuted)
        assertEquals(StemSolo.Music, back.solo)
    }

    @Test
    fun stemBuses_soloMutesOthers() {
        val buses = StemBuses(solo = StemSolo.Dialogue)
        assertFalse(buses.forcesMute(StemSolo.Dialogue))
        assertTrue(buses.forcesMute(StemSolo.Music))
        assertTrue(buses.forcesMute(StemSolo.Sfx))
    }

    @Test
    fun withStemBusesApplied_mutesMusicBus() {
        val t = Timeline(
            clips = listOf(TimelineClip("c0", 0, 0, 1000)),
            bed = AudioBed("bed.m4a"),
            voice = AudioBed("vo.m4a", volume = 1f, duck = 1f),
            extraStems = listOf(AudioStem("s1", "hit.wav", role = "sfx")),
            stemBuses = StemBuses(musicMuted = true)
        ).withStemBusesApplied()
        assertTrue(t.bed!!.muted)
        assertFalse(t.voice!!.muted)
        assertFalse(t.extraStems[0].muted)
    }

    @Test
    fun loudness_applyMaster_enablesLimiter() {
        val fx = LoudnessTarget.Streaming.applyMaster(AudioMasterFx.Neutral)
        assertTrue(fx.limiterEnabled)
        assertTrue(fx.compRatio > 1f)
        assertEquals(-14, LoudnessTarget.Streaming.lufs)
    }

    @Test
    fun slip_and_loudness_roundTrip_flat() {
        val t = Timeline(
            clips = listOf(TimelineClip("c0", 0, 0, 2000)),
            bed = AudioBed("bed.m4a", slipMs = 250),
            voice = AudioBed("vo.m4a", volume = 1f, duck = 1f, slipMs = -100),
            extraStems = listOf(AudioStem("s1", "whoosh.m4a", role = "sfx", slipMs = 40)),
            stemBuses = StemBuses(solo = StemSolo.Sfx),
            loudness = LoudnessTarget.Podcast,
            masterFx = LoudnessTarget.Podcast.applyMaster(AudioMasterFx.Neutral)
        )
        val back = Timeline.decode(t.encode(), 1) { 2000 }
        assertEquals(250L, back.bed!!.slipMs)
        assertEquals(-100L, back.voice!!.slipMs)
        assertEquals(40L, back.extraStems[0].slipMs)
        assertEquals(StemSolo.Sfx, back.stemBuses.solo)
        assertEquals(LoudnessTarget.Podcast, back.loudness)
    }

    @Test
    fun multiTrack_preservesSoundFinish() {
        val mt = MultiTrackTimeline.fromSingleVideo(
            listOf(TimelineClip("c0", 0, 0, 1000))
        ).copy(
            bed = AudioBed("bed.m4a", slipMs = 80),
            voice = AudioBed("vo.m4a", volume = 1f, duck = 1f),
            stemBuses = StemBuses(dialogueMuted = true),
            loudness = LoudnessTarget.Broadcast,
            masterFx = LoudnessTarget.Broadcast.applyMaster(AudioMasterFx.Neutral)
        )
        val back = MultiTrackTimeline.decode(mt.encode())!!
        assertEquals(80L, back.bed!!.slipMs)
        assertTrue(back.stemBuses.dialogueMuted)
        assertEquals(LoudnessTarget.Broadcast, back.loudness)
        assertTrue(back.masterFx.limiterEnabled)
    }

    @Test
    fun clapOffset_findsTransient() {
        val pcm = ShortArray(8000) { 0 }
        // Clap at ~500ms @ 8kHz
        for (i in 4000 until 4020) pcm[i] = Short.MAX_VALUE
        val ms = AudioSync.clapOffsetMs(pcm, 8000, threshold = 0.5f)
        assertEquals(500L, ms)
    }

    @Test
    fun suggestVoiceSlip_alignsClapToBeat() {
        assertEquals(-200L, AudioSync.suggestVoiceSlip(clapInVoiceMs = 200, pictureBeatMs = 0))
        assertEquals(100L, AudioSync.suggestVoiceSlip(clapInVoiceMs = 0, pictureBeatMs = 100))
    }
}
