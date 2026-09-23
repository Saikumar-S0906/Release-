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
import androidx.compose.material3.TextButton
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
import com.pixijoy.movie.TimelineTitle
import com.pixijoy.movie.TitleRole

/** D4 titles/captions — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieTitlesPanel(
    titles: List<TimelineTitle>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAddTitle: () -> Unit,
    onAddCaption: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d4_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d4_hint")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onAddTitle,
                modifier = Modifier.testTag("movie_title_add")
            ) {
                Text(stringResource(R.string.studio_title_add), color = PixiText)
            }
            TextButton(
                onClick = onAddCaption,
                modifier = Modifier.testTag("movie_caption_add")
            ) {
                Text(stringResource(R.string.studio_caption_add), color = PixiText)
            }
            if (titles.isNotEmpty()) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("movie_titles_clear")
                ) {
                    Text(stringResource(R.string.studio_titles_clear), color = PixiText)
                }
            }
        }
        if (titles.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .testTag("movie_titles_row")
            ) {
                titles.forEach { title ->
                    val label = when (title.role) {
                        TitleRole.Title -> stringResource(R.string.studio_title_chip, title.text)
                        TitleRole.Caption -> stringResource(R.string.studio_caption_chip, title.text)
                    }
                    Text(
                        label,
                        color = PixiText,
                        fontWeight = if (title.id == selectedId) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (title.id == selectedId) PixiAccent else PixiBg)
                            .clickable { onSelect(title.id) }
                            .testTag("movie_title_${title.id}")
                            .semantics {
                                role = Role.RadioButton
                                selected = title.id == selectedId
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
