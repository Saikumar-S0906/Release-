package com.pixijoy.studio.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiAccentSoft
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.ClipFrame
import com.pixijoy.movie.ReframePreset
import com.pixijoy.movie.StabilizePreset
import com.pixijoy.movie.TimelineClip

/** D8 stabilize + reframe — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieFramePanel(
    selected: TimelineClip?,
    onFrame: (ClipFrame) -> Unit
) {
    val frame = selected?.frame ?: ClipFrame.None
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d8_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d8_hint")
        )
        Text(stringResource(R.string.studio_stabilize), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_stabilize_presets")
        ) {
            FrameChip(
                label = stringResource(R.string.studio_stabilize_off),
                selected = StabilizePreset.matching(frame) == StabilizePreset.Off,
                tag = "movie_stabilize_off"
            ) { onFrame(frame.withStabilize(StabilizePreset.Off)) }
            FrameChip(
                label = stringResource(R.string.studio_stabilize_light),
                selected = StabilizePreset.matching(frame) == StabilizePreset.Light,
                tag = "movie_stabilize_light"
            ) { onFrame(frame.withStabilize(StabilizePreset.Light)) }
            FrameChip(
                label = stringResource(R.string.studio_stabilize_medium),
                selected = StabilizePreset.matching(frame) == StabilizePreset.Medium,
                tag = "movie_stabilize_medium"
            ) { onFrame(frame.withStabilize(StabilizePreset.Medium)) }
            FrameChip(
                label = stringResource(R.string.studio_stabilize_strong),
                selected = StabilizePreset.matching(frame) == StabilizePreset.Strong,
                tag = "movie_stabilize_strong"
            ) { onFrame(frame.withStabilize(StabilizePreset.Strong)) }
        }
        Text(stringResource(R.string.studio_reframe), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_reframe_presets")
        ) {
            FrameChip(
                label = stringResource(R.string.studio_reframe_original),
                selected = frame.matchesReframe(ReframePreset.Original),
                tag = "movie_reframe_original"
            ) { onFrame(frame.withReframe(ReframePreset.Original)) }
            FrameChip(
                label = stringResource(R.string.studio_reframe_close),
                selected = frame.matchesReframe(ReframePreset.Close),
                tag = "movie_reframe_close"
            ) { onFrame(frame.withReframe(ReframePreset.Close)) }
            FrameChip(
                label = stringResource(R.string.studio_reframe_left),
                selected = frame.matchesReframe(ReframePreset.Left),
                tag = "movie_reframe_left"
            ) { onFrame(frame.withReframe(ReframePreset.Left)) }
            FrameChip(
                label = stringResource(R.string.studio_reframe_right),
                selected = frame.matchesReframe(ReframePreset.Right),
                tag = "movie_reframe_right"
            ) { onFrame(frame.withReframe(ReframePreset.Right)) }
            FrameChip(
                label = stringResource(R.string.studio_reframe_vertical),
                selected = frame.matchesReframe(ReframePreset.Vertical),
                tag = "movie_reframe_vertical"
            ) { onFrame(frame.withReframe(ReframePreset.Vertical)) }
        }
    }
}

@Composable
private fun FrameChip(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Text(
        label,
        color = PixiText,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccentSoft else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
