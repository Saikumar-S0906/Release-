package com.pixijoy.ai.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.ml.pack.PackId
import com.pixijoy.movie.ClipTrack
import com.pixijoy.movie.TrackEngine
import com.pixijoy.movie.TrackFacade
import com.pixijoy.movie.TimelineClip

/** F1 tracking / roto-lite contract — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieTrackPanel(
    selected: TimelineClip?,
    onTrack: (ClipTrack) -> Unit
) {
    val track = selected?.track ?: ClipTrack.None
    val ready = TrackFacade.mlAvailable()
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
            stringResource(
                if (track.isActive) R.string.studio_track_clear else R.string.studio_track_enable
            ),
            {
                onTrack(
                    if (track.isActive) ClipTrack.None
                    else ClipTrack(engine = TrackEngine.Ml)
                )
            },
            Modifier.testTag("movie_track_toggle"),
            enabled = ready || track.isActive
        )
        if (!ready) {
            Text(
                stringResource(R.string.studio_track_locked),
                color = PixiTextMuted,
                modifier = Modifier.testTag("movie_track_locked")
            )
        }
    }
}
