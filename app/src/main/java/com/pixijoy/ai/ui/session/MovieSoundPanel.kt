package com.pixijoy.ai.ui.session

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiSlider
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.AudioBed
import com.pixijoy.movie.AudioSync
import com.pixijoy.movie.LoudnessTarget
import com.pixijoy.movie.StemBuses
import com.pixijoy.movie.StemSolo
import com.pixijoy.movie.Timeline
import java.io.File

/** R8 / P4 — stem buses, ADR record, slip, LUFS target. */
@Composable
fun MovieSoundPanel(
    timeline: Timeline,
    projectDir: File,
    playheadMs: Long,
    onTimeline: (Timeline) -> Unit,
    onSettled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var slate by remember { mutableStateOf("ADR") }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startAdr(context, projectDir, slate) { rec, file ->
            recorder = rec
            recording = true
            // file reserved until stop
            @Suppress("UNUSED_VARIABLE") val path = file
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
            recorder = null
            recording = false
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StudioHint(stringResource(R.string.studio_sound_hint), "movie_sound_hint")

        Text(stringResource(R.string.studio_stem_buses), color = PixiText)
        StemBusRow(
            buses = timeline.stemBuses,
            onChange = { onTimeline(timeline.copy(stemBuses = it)) }
        )

        Text(stringResource(R.string.studio_adr), color = PixiText)
        Text(
            stringResource(R.string.studio_adr_slate, slate),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_adr_slate")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { slate = "ADR_${playheadMs}" },
                modifier = Modifier.testTag("movie_adr_slate_btn")
            ) {
                Text(stringResource(R.string.studio_adr_slate_set), color = PixiText)
            }
            if (!recording) {
                TextButton(
                    onClick = {
                        val ok = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (ok) {
                            startAdr(context, projectDir, slate) { rec, _ ->
                                recorder = rec
                                recording = true
                            }
                        } else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.testTag("movie_adr_record")
                ) {
                    Text(stringResource(R.string.studio_adr_record), color = PixiText)
                }
            } else {
                TextButton(
                    onClick = {
                        val file = File(projectDir, "voice.m4a")
                        runCatching { recorder?.stop() }
                        runCatching { recorder?.release() }
                        recorder = null
                        recording = false
                        if (file.exists() && file.length() > 0) {
                            onTimeline(
                                timeline.copy(
                                    voice = AudioBed(
                                        fileName = file.name,
                                        volume = 1f,
                                        fadeInMs = 40,
                                        fadeOutMs = 80,
                                        muted = false,
                                        duck = 1f
                                    )
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("movie_adr_stop")
                ) {
                    Text(stringResource(R.string.studio_adr_stop), color = PixiText)
                }
            }
        }

        val voice = timeline.voice
        if (voice != null) {
            Text(
                stringResource(R.string.studio_slip_voice, voice.slipMs.toInt()),
                color = PixiTextMuted
            )
            PixiSlider(
                value = voice.slipMs.toFloat(),
                onValueChange = {
                    onTimeline(
                        timeline.copy(
                            voice = voice.copy(slipMs = AudioSync.clampSlip(it.toLong()))
                        )
                    )
                },
                onValueChangeFinished = onSettled,
                valueRange = AudioSync.SlipMinMs.toFloat()..AudioSync.SlipMaxMs.toFloat(),
                modifier = Modifier.testTag("movie_voice_slip")
            )
            TextButton(
                onClick = {
                    // Heuristic clap: assume clap near start → nudge VO to playhead
                    val clap = 120L
                    val slip = AudioSync.suggestVoiceSlip(clap, playheadMs)
                    onTimeline(timeline.copy(voice = voice.copy(slipMs = slip)))
                },
                modifier = Modifier.testTag("movie_clap_align")
            ) {
                Text(stringResource(R.string.studio_clap_align), color = PixiText)
            }
        }

        val bed = timeline.bed
        if (bed != null) {
            Text(
                stringResource(R.string.studio_slip_music, bed.slipMs.toInt()),
                color = PixiTextMuted
            )
            PixiSlider(
                value = bed.slipMs.toFloat(),
                onValueChange = {
                    onTimeline(
                        timeline.copy(bed = bed.copy(slipMs = AudioSync.clampSlip(it.toLong())))
                    )
                },
                onValueChangeFinished = onSettled,
                valueRange = AudioSync.SlipMinMs.toFloat()..AudioSync.SlipMaxMs.toFloat(),
                modifier = Modifier.testTag("movie_bed_slip")
            )
        }

        Text(stringResource(R.string.studio_loudness), color = PixiText)
        Text(
            timeline.loudness.meterLabel(),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_loudness_meter")
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_loudness_targets")
        ) {
            LoudnessTarget.entries.forEach { target ->
                val label = when (target) {
                    LoudnessTarget.Off -> stringResource(R.string.studio_loudness_off)
                    LoudnessTarget.Streaming -> stringResource(R.string.studio_loudness_14)
                    LoudnessTarget.Podcast -> stringResource(R.string.studio_loudness_16)
                    LoudnessTarget.Broadcast -> stringResource(R.string.studio_loudness_24)
                }
                TextButton(
                    onClick = {
                        onTimeline(
                            timeline.copy(
                                loudness = target,
                                masterFx = if (target.isOff()) {
                                    timeline.masterFx
                                } else {
                                    target.applyMaster(timeline.masterFx)
                                }
                            )
                        )
                    },
                    modifier = Modifier.testTag("movie_loudness_${target.name.lowercase()}")
                ) {
                    Text(label, color = PixiText)
                }
            }
        }
    }
}

@Composable
private fun StemBusRow(
    buses: StemBuses,
    onChange: (StemBuses) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            StemSolo.Dialogue to R.string.studio_bus_dialogue,
            StemSolo.Music to R.string.studio_bus_music,
            StemSolo.Sfx to R.string.studio_bus_sfx
        ).forEach { (bus, labelRes) ->
            val muted = when (bus) {
                StemSolo.Dialogue -> buses.dialogueMuted
                StemSolo.Music -> buses.musicMuted
                StemSolo.Sfx -> buses.sfxMuted
                StemSolo.None -> false
            }
            val soloed = buses.solo == bus
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("movie_bus_${bus.name.lowercase()}")
            ) {
                Text(stringResource(labelRes), color = PixiText, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        onChange(
                            when (bus) {
                                StemSolo.Dialogue -> buses.copy(dialogueMuted = !muted)
                                StemSolo.Music -> buses.copy(musicMuted = !muted)
                                StemSolo.Sfx -> buses.copy(sfxMuted = !muted)
                                StemSolo.None -> buses
                            }
                        )
                    },
                    modifier = Modifier.testTag("movie_bus_mute_${bus.name.lowercase()}")
                ) {
                    Text(
                        if (muted) stringResource(R.string.studio_unmute)
                        else stringResource(R.string.studio_mute),
                        color = PixiText
                    )
                }
                TextButton(
                    onClick = {
                        onChange(
                            buses.copy(solo = if (soloed) StemSolo.None else bus)
                        )
                    },
                    modifier = Modifier.testTag("movie_bus_solo_${bus.name.lowercase()}")
                ) {
                    Text(
                        if (soloed) stringResource(R.string.studio_unsolo)
                        else stringResource(R.string.studio_solo),
                        color = PixiText
                    )
                }
            }
        }
    }
}

private fun startAdr(
    context: Context,
    projectDir: File,
    slate: String,
    onStarted: (MediaRecorder, File) -> Unit
) {
    projectDir.mkdirs()
    val dest = File(projectDir, "voice.m4a")
    if (dest.exists()) dest.delete()
    val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    runCatching {
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(128_000)
        rec.setAudioSamplingRate(44_100)
        rec.setOutputFile(dest.absolutePath)
        // Slate is stored in filename epoch; slate label is UI-only for now.
        @Suppress("UNUSED_VARIABLE") val note = slate
        rec.prepare()
        rec.start()
        onStarted(rec, dest)
    }.onFailure {
        runCatching { rec.release() }
    }
}
