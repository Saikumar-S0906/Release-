package com.pixijoy.ai.ui.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.TimelineVfx
import com.pixijoy.movie.VfxPreset

/** F2 classical particle VFX on the composition overlay. */
@Composable
fun MovieVfxPanel(
    layers: List<TimelineVfx>,
    totalMs: Long,
    onAdd: (VfxPreset) -> Unit,
    onIntensity: (String, Float) -> Unit,
    onFollowTrack: (String, Boolean) -> Unit,
    onClear: () -> Unit
) {
    val hint = stringResource(R.string.studio_vfx_hint)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            hint,
            color = PixiTextMuted,
            modifier = Modifier
                .testTag("movie_f2_hint")
                .semantics { contentDescription = hint }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_vfx_presets")
        ) {
            listOf(
                VfxPreset.Sparkle to R.string.studio_vfx_sparkle,
                VfxPreset.Ember to R.string.studio_vfx_ember,
                VfxPreset.Confetti to R.string.studio_vfx_confetti,
                VfxPreset.Snow to R.string.studio_vfx_snow,
                VfxPreset.Bokeh to R.string.studio_vfx_bokeh
            ).forEach { (preset, labelRes) ->
                val label = stringResource(labelRes)
                PixiPrimaryButton(
                    label,
                    { onAdd(preset) },
                    Modifier.testTag("movie_vfx_${preset.name.lowercase()}"),
                    contentDescription = label
                )
            }
        }
        if (layers.isNotEmpty()) {
            layers.forEach { layer ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("movie_vfx_layer_${layer.id}")
                ) {
                    Text(
                        "${layer.preset.name} · ${(layer.intensity * 100).toInt()}%",
                        color = PixiText,
                        modifier = Modifier.testTag("movie_vfx_layer_label")
                    )
                    Slider(
                        value = layer.intensity,
                        onValueChange = { onIntensity(layer.id, it) },
                        valueRange = 0.1f..1f,
                        modifier = Modifier.testTag("movie_vfx_intensity")
                    )
                    val followLabel = if (layer.followTrack) {
                        stringResource(R.string.studio_vfx_unfollow_track)
                    } else {
                        stringResource(R.string.studio_vfx_follow_track)
                    }
                    PixiPrimaryButton(
                        followLabel,
                        { onFollowTrack(layer.id, !layer.followTrack) },
                        Modifier.testTag("movie_vfx_follow_${layer.id}"),
                        contentDescription = followLabel
                    )
                }
            }
            PixiPrimaryButton(
                stringResource(R.string.studio_vfx_clear),
                onClear,
                Modifier.testTag("movie_vfx_clear"),
                contentDescription = stringResource(R.string.studio_vfx_clear)
            )
        } else {
            Text(
                stringResource(R.string.studio_vfx_empty),
                color = PixiTextMuted,
                modifier = Modifier.testTag("movie_vfx_empty")
            )
        }
    }
}

fun TimelineVfx.withPreset(preset: VfxPreset, totalMs: Long, index: Int): TimelineVfx =
    TimelineVfx(
        id = "vx$index",
        preset = preset,
        startMs = 0,
        endMs = totalMs.coerceAtLeast(1000),
        intensity = 0.7f
    )
