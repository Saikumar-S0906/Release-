package com.pixijoy.ai.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.ml.pack.PackId
import com.pixijoy.movie.ClipTrack
import com.pixijoy.movie.TrackEngine
import com.pixijoy.movie.TimelineClip

/** F1 tracking / roto-lite contract — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieTrackPanel(
    selected: TimelineClip?,
    onTrack: (ClipTrack) -> Unit
) {
    val track = selected?.track ?: ClipTrack.None
    val ready = rememberPackEngineReady(PackId.Track)
    val toggle = trackToggle(engineReady = ready, trackActive = track.isActive)
    val coming = stringResource(R.string.studio_hold_coming)
    val label = when (toggle) {
        HoldControl.ComingWithPack -> coming
        HoldControl.ClearTrack -> stringResource(R.string.studio_track_clear)
        else -> stringResource(R.string.studio_track_enable)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_track_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_f1_hint")
        )
        PackStatusRow(
            packId = PackId.Track,
            statusTag = "movie_track_pack_status",
            downloadTag = "movie_track_pack_download"
        )
        PixiPrimaryButton(
            label,
            {
                onTrack(
                    if (track.isActive) ClipTrack.None
                    else ClipTrack(engine = TrackEngine.Ml)
                )
            },
            Modifier.testTag("movie_track_toggle"),
            enabled = toggle.enabled,
            contentDescription = label
        )
        if (!ready) {
            Text(
                coming,
                color = PixiTextMuted,
                modifier = Modifier
                    .testTag("movie_track_locked")
                    .semantics { contentDescription = coming }
            )
        }
    }
}
