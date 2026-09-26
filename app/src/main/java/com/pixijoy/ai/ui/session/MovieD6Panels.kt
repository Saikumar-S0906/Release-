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
import com.pixijoy.designsystem.PixiSlider
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.AudioBed
import com.pixijoy.movie.DuckPreset

/** D6 audio finish — duck presets + attack/release. */
@Composable
fun MovieAudioFinishPanel(
    bed: AudioBed?,
    hasVoice: Boolean,
    onApplyPreset: (DuckPreset) -> Unit,
    onBedChange: (AudioBed) -> Unit,
    onSettled: () -> Unit = {}
) {
    val matched = DuckPreset.matching(bed)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d6_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d6_hint")
        )
        if (hasVoice && bed != null) {
            Text(
                stringResource(R.string.studio_movie_d6_vo_active),
                color = PixiTextMuted,
                modifier = Modifier.testTag("movie_d6_vo_active")
            )
        }
        Text(stringResource(R.string.studio_duck_preset), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_duck_presets")
        ) {
            DuckChip(
                label = stringResource(R.string.studio_duck_soft),
                selected = matched == DuckPreset.Soft,
                tag = "movie_duck_soft"
            ) { onApplyPreset(DuckPreset.Soft) }
            DuckChip(
                label = stringResource(R.string.studio_duck_medium),
                selected = matched == DuckPreset.Medium,
                tag = "movie_duck_medium"
            ) { onApplyPreset(DuckPreset.Medium) }
            DuckChip(
                label = stringResource(R.string.studio_duck_hard),
                selected = matched == DuckPreset.Hard,
                tag = "movie_duck_hard"
            ) { onApplyPreset(DuckPreset.Hard) }
        }
        if (bed != null) {
            Text(
                stringResource(R.string.studio_duck_attack, bed.attackMs.toInt()),
                color = PixiTextMuted
            )
            PixiSlider(
                value = bed.attackMs.toFloat(),
                onValueChange = { onBedChange(bed.copy(attackMs = it.toLong().coerceAtLeast(0))) },
                onValueChangeFinished = onSettled,
                valueRange = 0f..600f,
                modifier = Modifier.testTag("movie_bed_duck_attack")
            )
            Text(
                stringResource(R.string.studio_duck_release, bed.releaseMs.toInt()),
                color = PixiTextMuted
            )
            PixiSlider(
                value = bed.releaseMs.toFloat(),
                onValueChange = { onBedChange(bed.copy(releaseMs = it.toLong().coerceAtLeast(0))) },
                onValueChangeFinished = onSettled,
                valueRange = 0f..800f,
                modifier = Modifier.testTag("movie_bed_duck_release")
            )
        }
    }
}

@Composable
private fun DuckChip(
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
