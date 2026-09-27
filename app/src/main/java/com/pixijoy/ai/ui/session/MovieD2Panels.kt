package com.pixijoy.ai.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pixijoy.designsystem.PixiSlider
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.ClipTransition
import com.pixijoy.movie.MovieSpeed
import com.pixijoy.movie.TimelineClip

private enum class MovieD2Mode { Roll, Transition, Speed }

/**
 * D2 controls kept outside [MovieSessionScreen] to limit method size.
 */
@Composable
fun MovieEditOpsPanel(
    selected: TimelineClip?,
    canRoll: Boolean,
    onRoll: (Long) -> Unit,
    onTransition: (ClipTransition, Long) -> Unit,
    onSpeedRamp: (Float, Float) -> Unit,
    onJCut: () -> Unit = {},
    onLCut: () -> Unit = {},
    onSettled: () -> Unit = {}
) {
    var focusedMode by remember { mutableStateOf(MovieD2Mode.Transition) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StudioHint(stringResource(R.string.studio_movie_d2_hint), "movie_d2_hint")
        StudioHScrollRow {
            StudioChip(
                stringResource(R.string.studio_roll),
                focusedMode == MovieD2Mode.Roll,
                "movie_d2_roll"
            ) {
                focusedMode = MovieD2Mode.Roll
            }
            StudioChip(
                stringResource(R.string.studio_transition),
                focusedMode == MovieD2Mode.Transition,
                "movie_d2_transition"
            ) {
                focusedMode = MovieD2Mode.Transition
            }
            StudioChip(
                stringResource(R.string.studio_speed_ramp),
                focusedMode == MovieD2Mode.Speed,
                "movie_d2_speed"
            ) {
                focusedMode = MovieD2Mode.Speed
            }
            StudioChip(
                stringResource(R.string.studio_jl_cut),
                false,
                "movie_d2_jl"
            ) {
                focusedMode = MovieD2Mode.Roll
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onJCut,
                enabled = selected != null,
                modifier = Modifier.testTag("movie_j_cut")
            ) {
                Text(stringResource(R.string.studio_j_cut), color = PixiText)
            }
            TextButton(
                onClick = onLCut,
                enabled = selected != null,
                modifier = Modifier.testTag("movie_l_cut")
            ) {
                Text(stringResource(R.string.studio_l_cut), color = PixiText)
            }
        }
        when (focusedMode) {
            MovieD2Mode.Roll -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onRoll(-250L) },
                        enabled = canRoll && selected != null,
                        modifier = Modifier.testTag("movie_roll_left")
                    ) {
                        Text(stringResource(R.string.studio_roll_left), color = PixiText)
                    }
                    TextButton(
                        onClick = { onRoll(250L) },
                        enabled = canRoll && selected != null,
                        modifier = Modifier.testTag("movie_roll_right")
                    ) {
                        Text(stringResource(R.string.studio_roll_right), color = PixiText)
                    }
                }
            }
            MovieD2Mode.Transition -> {
                StudioHScrollRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransitionChip(
                        label = stringResource(R.string.studio_transition_none),
                        selected = selected?.transition == ClipTransition.None || selected == null,
                        tag = "movie_transition_none"
                    ) { onTransition(ClipTransition.None, 0L); onSettled() }
                    TransitionChip(
                        label = stringResource(R.string.studio_transition_soft),
                        selected = selected?.transition == ClipTransition.Soft,
                        tag = "movie_transition_soft"
                    ) {
                        onTransition(
                            ClipTransition.Soft,
                            (selected?.transitionMs ?: 400L).coerceAtLeast(200L)
                        )
                        onSettled()
                    }
                }
                if (selected?.transition == ClipTransition.Soft) {
                    Text(
                        stringResource(R.string.studio_transition_ms, selected.transitionMs.toInt()),
                        color = PixiTextMuted
                    )
                    PixiSlider(
                        value = selected.transitionMs.toFloat().coerceIn(80f, 1200f),
                        onValueChange = { onTransition(ClipTransition.Soft, it.toLong()) },
                        onValueChangeFinished = onSettled,
                        valueRange = 80f..1200f,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("movie_transition_ms")
                    )
                }
            }
            MovieD2Mode.Speed -> {
                val start = MovieSpeed.normalize(selected?.speed ?: 1f)
                val end = MovieSpeed.normalize(selected?.speedEnd ?: start)
                Text(
                    stringResource(R.string.studio_speed_ramp_values, start, end),
                    color = PixiTextMuted
                )
                PixiSlider(
                    value = start,
                    onValueChange = { onSpeedRamp(MovieSpeed.normalize(it), end) },
                    onValueChangeFinished = onSettled,
                    valueRange = 0.5f..2f,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("movie_speed_start")
                )
                PixiSlider(
                    value = end,
                    onValueChange = { onSpeedRamp(start, MovieSpeed.normalize(it)) },
                    onValueChangeFinished = onSettled,
                    valueRange = 0.5f..2f,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("movie_speed_end")
                )
            }
        }
    }
}

@Composable
private fun TransitionChip(
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
