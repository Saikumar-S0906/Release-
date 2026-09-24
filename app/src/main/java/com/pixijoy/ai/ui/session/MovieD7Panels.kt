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
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.VideoLayout

/** D7 PiP / multicam layout presets — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieLayoutPanel(
    layout: VideoLayout,
    onLayout: (VideoLayout) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_layout_presets")
        ) {
            LayoutChip(
                label = stringResource(R.string.studio_layout_single),
                selected = layout == VideoLayout.Single,
                tag = "movie_layout_single"
            ) { onLayout(VideoLayout.Single) }
            LayoutChip(
                label = stringResource(R.string.studio_layout_pip),
                selected = layout == VideoLayout.Pip,
                tag = "movie_layout_pip"
            ) { onLayout(VideoLayout.Pip) }
            LayoutChip(
                label = stringResource(R.string.studio_layout_split),
                selected = layout == VideoLayout.Split,
                tag = "movie_layout_split"
            ) { onLayout(VideoLayout.Split) }
            LayoutChip(
                label = stringResource(R.string.studio_layout_grid),
                selected = layout == VideoLayout.Grid,
                tag = "movie_layout_grid"
            ) { onLayout(VideoLayout.Grid) }
        }
    }
}

@Composable
private fun LayoutChip(
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
            .background(if (selected) PixiAccent else PixiBg)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
