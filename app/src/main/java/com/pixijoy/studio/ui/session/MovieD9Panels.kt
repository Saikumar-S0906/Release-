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
import com.pixijoy.designsystem.PixiSlider
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.ChromaKey
import com.pixijoy.movie.ChromaSimilarityPreset
import com.pixijoy.movie.ClipChroma
import com.pixijoy.movie.TimelineClip

/** D9 chroma key — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieChromaPanel(
    selected: TimelineClip?,
    onChroma: (ClipChroma) -> Unit,
    onSettled: () -> Unit = {}
) {
    val chroma = selected?.chroma ?: ClipChroma.None
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d9_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d9_hint")
        )
        Text(stringResource(R.string.studio_chroma_key), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_chroma_keys")
        ) {
            ChromaChip(
                label = stringResource(R.string.studio_chroma_off),
                selected = chroma.key == ChromaKey.Off,
                tag = "movie_chroma_off"
            ) { onChroma(ClipChroma.None); onSettled() }
            ChromaChip(
                label = stringResource(R.string.studio_chroma_green),
                selected = chroma.key == ChromaKey.Green,
                tag = "movie_chroma_green"
            ) { onChroma(chroma.withKey(ChromaKey.Green)); onSettled() }
            ChromaChip(
                label = stringResource(R.string.studio_chroma_blue),
                selected = chroma.key == ChromaKey.Blue,
                tag = "movie_chroma_blue"
            ) { onChroma(chroma.withKey(ChromaKey.Blue)); onSettled() }
        }
        if (chroma.key != ChromaKey.Off) {
            Text(stringResource(R.string.studio_chroma_similarity_preset), color = PixiTextMuted)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .testTag("movie_chroma_similarity_presets")
            ) {
                ChromaChip(
                    label = stringResource(R.string.studio_chroma_tight),
                    selected = ChromaSimilarityPreset.matching(chroma) == ChromaSimilarityPreset.Tight,
                    tag = "movie_chroma_tight"
                ) { onChroma(chroma.withSimilarity(ChromaSimilarityPreset.Tight.value)); onSettled() }
                ChromaChip(
                    label = stringResource(R.string.studio_chroma_medium),
                    selected = ChromaSimilarityPreset.matching(chroma) == ChromaSimilarityPreset.Medium,
                    tag = "movie_chroma_medium"
                ) { onChroma(chroma.withSimilarity(ChromaSimilarityPreset.Medium.value)); onSettled() }
                ChromaChip(
                    label = stringResource(R.string.studio_chroma_wide),
                    selected = ChromaSimilarityPreset.matching(chroma) == ChromaSimilarityPreset.Wide,
                    tag = "movie_chroma_wide"
                ) { onChroma(chroma.withSimilarity(ChromaSimilarityPreset.Wide.value)); onSettled() }
            }
            Text(
                stringResource(R.string.studio_chroma_softness, (chroma.softness * 100).toInt()),
                color = PixiTextMuted
            )
            PixiSlider(
                value = chroma.softness,
                onValueChange = { onChroma(chroma.withSoftness(it)) },
                onValueChangeFinished = onSettled,
                valueRange = 0f..0.4f,
                modifier = Modifier.testTag("movie_chroma_softness")
            )
        }
    }
}

@Composable
private fun ChromaChip(
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
