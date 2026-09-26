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
import com.pixijoy.movie.MovieHdrMode

/** D11 HDR export intent — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieHdrPanel(
    hdrMode: MovieHdrMode,
    onHdrMode: (MovieHdrMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d11_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d11_hint")
        )
        Text(stringResource(R.string.studio_export_hdr_label), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_hdr_presets")
        ) {
            HdrChip(
                label = stringResource(R.string.studio_export_hdr_sdr),
                selected = hdrMode == MovieHdrMode.Sdr,
                tag = "movie_hdr_sdr"
            ) { onHdrMode(MovieHdrMode.Sdr) }
            HdrChip(
                label = stringResource(R.string.studio_export_hdr_keep),
                selected = hdrMode == MovieHdrMode.KeepHdr,
                tag = "movie_hdr_keep"
            ) { onHdrMode(MovieHdrMode.KeepHdr) }
        }
    }
}

@Composable
private fun HdrChip(
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
