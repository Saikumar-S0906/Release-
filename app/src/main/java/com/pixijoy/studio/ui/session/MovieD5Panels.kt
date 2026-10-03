package com.pixijoy.studio.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.pixijoy.movie.MovieGrade
import com.pixijoy.movie.MovieLooks
import com.pixijoy.movie.TimelineGrade

/** D5 grade / look / scopes — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieGradePanel(
    grade: TimelineGrade,
    onGrade: (TimelineGrade) -> Unit,
    onSettled: () -> Unit = {}
) {
    val g = grade.normalized()
    val meters = MovieGrade.scopeMeters(g)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_movie_d5_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d5_hint")
        )
        Text(stringResource(R.string.studio_movie_look), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_look_presets")
        ) {
            MovieLooks.keys.forEach { key ->
                val selected = g.lookKey.equals(key, ignoreCase = true)
                Text(
                    lookLabel(key),
                    color = PixiText,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) PixiAccentSoft else Color.White.copy(alpha = 0.08f))
                        .clickable { onGrade(g.copy(lookKey = key)); onSettled() }
                        .testTag("movie_look_$key")
                        .semantics {
                            role = Role.RadioButton
                            this.selected = selected
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
        Text(
            stringResource(R.string.studio_movie_exposure, g.exposure),
            color = PixiTextMuted
        )
        PixiSlider(
            value = g.exposure,
            onValueChange = { onGrade(g.copy(exposure = it)) },
            onValueChangeFinished = onSettled,
            valueRange = -2f..2f,
            modifier = Modifier.testTag("movie_grade_exposure")
        )
        Text(
            stringResource(R.string.studio_movie_contrast, g.contrast),
            color = PixiTextMuted
        )
        PixiSlider(
            value = g.contrast,
            onValueChange = { onGrade(g.copy(contrast = it)) },
            onValueChangeFinished = onSettled,
                valueRange = 0.5f..1.8f,
            modifier = Modifier.testTag("movie_grade_contrast")
        )
        Text(
            stringResource(R.string.studio_movie_saturation, g.saturation),
            color = PixiTextMuted
        )
        PixiSlider(
            value = g.saturation,
            onValueChange = { onGrade(g.copy(saturation = it)) },
            onValueChangeFinished = onSettled,
            valueRange = 0f..2f,
            modifier = Modifier.testTag("movie_grade_saturation")
        )
        Text(stringResource(R.string.studio_movie_scopes), color = PixiTextMuted)
        Text(
            stringResource(R.string.studio_scopes_display_referred),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_scopes_label")
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .testTag("movie_scopes")
                .padding(vertical = 4.dp)
        ) {
            ScopeBar("R", meters.red, Color(0xFFE53935), Modifier.weight(1f).testTag("movie_scope_r"))
            ScopeBar("G", meters.green, Color(0xFF43A047), Modifier.weight(1f).testTag("movie_scope_g"))
            ScopeBar("B", meters.blue, Color(0xFF1E88E5), Modifier.weight(1f).testTag("movie_scope_b"))
            ScopeBar("Y", meters.luma, Color(0xFFEEEEEE), Modifier.weight(1f).testTag("movie_scope_y"))
        }
    }
}

@Composable
private fun ScopeBar(label: String, level: Float, tint: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PixiBg),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(level.coerceIn(0.05f, 1f))
                    .background(tint)
            )
        }
        Text(label, color = PixiTextMuted)
    }
}

@Composable
private fun lookLabel(key: String): String = when (key.lowercase()) {
    MovieLooks.CLEAN -> stringResource(R.string.studio_look_clean)
    MovieLooks.WARM -> stringResource(R.string.studio_look_warm)
    MovieLooks.COOL -> stringResource(R.string.studio_look_cool)
    MovieLooks.FILM -> stringResource(R.string.studio_look_film)
    MovieLooks.NIGHT -> stringResource(R.string.studio_look_night)
    else -> stringResource(R.string.studio_look_original)
}
