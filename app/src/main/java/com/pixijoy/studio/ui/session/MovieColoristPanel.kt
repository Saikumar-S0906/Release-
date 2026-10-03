package com.pixijoy.studio.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSlider
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.GradeMatchRef
import com.pixijoy.movie.GradePipeline
import com.pixijoy.movie.GradeSecondary
import com.pixijoy.movie.MovieGrade
import com.pixijoy.movie.MovieHdrMode
import com.pixijoy.movie.ShotMatch
import com.pixijoy.movie.TimelineGrade

/** R8 / P3 — secondary qualify, shot match, pipeline + scopes truth. */
@Composable
fun MovieColoristPanel(
    grade: TimelineGrade,
    hdrMode: MovieHdrMode,
    onGrade: (TimelineGrade) -> Unit,
    onSettled: () -> Unit = {}
) {
    val g = grade.normalized()
    val sec = g.secondary.normalized()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_grade_pipeline, GradePipeline.ORDER_LABEL),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_grade_pipeline")
        )
        Text(
            GradePipeline.scopesTruthLabel(!hdrMode.isDefault()),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_scopes_truth")
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.studio_secondary), color = PixiText)
            Switch(
                checked = sec.enabled,
                onCheckedChange = {
                    onGrade(g.copy(secondary = sec.copy(enabled = it)))
                    onSettled()
                },
                modifier = Modifier.testTag("movie_secondary_enable")
            )
        }
        if (sec.enabled) {
            Text(stringResource(R.string.studio_secondary_exposure, sec.exposure), color = PixiTextMuted)
            PixiSlider(
                value = sec.exposure,
                onValueChange = { onGrade(g.copy(secondary = sec.copy(exposure = it))) },
                onValueChangeFinished = onSettled,
                valueRange = -1.5f..1.5f,
                modifier = Modifier.testTag("movie_secondary_exposure")
            )
            Text(stringResource(R.string.studio_secondary_sat, sec.saturation), color = PixiTextMuted)
            PixiSlider(
                value = sec.saturation,
                onValueChange = { onGrade(g.copy(secondary = sec.copy(saturation = it))) },
                onValueChangeFinished = onSettled,
                valueRange = 0.2f..2f,
                modifier = Modifier.testTag("movie_secondary_sat")
            )
            Text(stringResource(R.string.studio_secondary_hue, sec.hueCenter.toInt()), color = PixiTextMuted)
            PixiSlider(
                value = sec.hueCenter,
                onValueChange = { onGrade(g.copy(secondary = sec.copy(hueCenter = it))) },
                onValueChangeFinished = onSettled,
                valueRange = 0f..360f,
                modifier = Modifier.testTag("movie_secondary_hue")
            )
            Text(stringResource(R.string.studio_secondary_width, sec.hueWidth.toInt()), color = PixiTextMuted)
            PixiSlider(
                value = sec.hueWidth,
                onValueChange = { onGrade(g.copy(secondary = sec.copy(hueWidth = it))) },
                onValueChangeFinished = onSettled,
                valueRange = 10f..180f,
                modifier = Modifier.testTag("movie_secondary_width")
            )
            Text(stringResource(R.string.studio_window_size), color = PixiTextMuted)
            PixiSlider(
                value = sec.halfW,
                onValueChange = {
                    onGrade(g.copy(secondary = sec.copy(halfW = it, halfH = it)))
                },
                onValueChangeFinished = onSettled,
                valueRange = 0.15f..1f,
                modifier = Modifier.testTag("movie_secondary_window")
            )
        }

        Text(stringResource(R.string.studio_shot_match), color = PixiText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixiPrimaryButton(
                stringResource(R.string.studio_shot_match_store),
                {
                    val meters = MovieGrade.scopeMeters(g)
                    onGrade(g.copy(matchRef = GradeMatchRef.fromMeters(meters)))
                    onSettled()
                },
                Modifier.testTag("movie_match_store")
            )
            PixiPrimaryButton(
                stringResource(R.string.studio_shot_match_apply),
                {
                    if (g.matchRef.isSet()) {
                        onGrade(ShotMatch.suggest(g, g.matchRef))
                        onSettled()
                    }
                },
                Modifier.testTag("movie_match_apply")
            )
            TextButton(
                onClick = {
                    onGrade(g.copy(matchRef = GradeMatchRef.Unset))
                    onSettled()
                },
                modifier = Modifier.testTag("movie_match_clear")
            ) {
                Text(stringResource(R.string.studio_shot_match_clear), color = PixiTextMuted)
            }
        }
        if (g.matchRef.isSet()) {
            Text(
                stringResource(
                    R.string.studio_shot_match_ref,
                    g.matchRef.luma
                ),
                color = PixiTextMuted,
                modifier = Modifier.testTag("movie_match_ref")
            )
        }

        TextButton(
            onClick = {
                onGrade(TimelineGrade.Neutral.copy(matchRef = g.matchRef))
                onSettled()
            },
            modifier = Modifier.testTag("movie_grade_reset")
        ) {
            Text(stringResource(R.string.studio_grade_reset), color = PixiText)
        }
    }
}
