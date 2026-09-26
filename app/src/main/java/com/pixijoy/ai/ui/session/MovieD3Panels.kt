package com.pixijoy.ai.ui.session

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
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiAccentSoft
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.ClipMotion
import com.pixijoy.movie.ClipMotionPreset
import com.pixijoy.movie.TimelineClip

/** D3 motion presets — kept outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieMotionPanel(
    selected: TimelineClip?,
    onMotion: (ClipMotion) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d3_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d3_hint")
        )
        Text(stringResource(R.string.studio_motion), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_motion_presets")
        ) {
            MotionChip(
                label = stringResource(R.string.studio_motion_none),
                selected = selected?.motion?.isIdentity != false,
                tag = "movie_motion_none"
            ) { onMotion(ClipMotion.None) }
            MotionChip(
                label = stringResource(R.string.studio_motion_zoom_in),
                selected = selected?.motion?.matchesPreset(ClipMotionPreset.ZoomIn) == true,
                tag = "movie_motion_zoom_in"
            ) { onMotion(ClipMotionPreset.ZoomIn.toMotion()) }
            MotionChip(
                label = stringResource(R.string.studio_motion_zoom_out),
                selected = selected?.motion?.matchesPreset(ClipMotionPreset.ZoomOut) == true,
                tag = "movie_motion_zoom_out"
            ) { onMotion(ClipMotionPreset.ZoomOut.toMotion()) }
            MotionChip(
                label = stringResource(R.string.studio_motion_pan_left),
                selected = selected?.motion?.matchesPreset(ClipMotionPreset.PanLeft) == true,
                tag = "movie_motion_pan_left"
            ) { onMotion(ClipMotionPreset.PanLeft.toMotion()) }
            MotionChip(
                label = stringResource(R.string.studio_motion_pan_right),
                selected = selected?.motion?.matchesPreset(ClipMotionPreset.PanRight) == true,
                tag = "movie_motion_pan_right"
            ) { onMotion(ClipMotionPreset.PanRight.toMotion()) }
            MotionChip(
                label = stringResource(R.string.studio_motion_drift),
                selected = selected?.motion?.matchesPreset(ClipMotionPreset.Drift) == true,
                tag = "movie_motion_drift"
            ) { onMotion(ClipMotionPreset.Drift.toMotion()) }
        }
    }
}

@Composable
private fun MotionChip(
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
