package com.pixijoy.ai.ui.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiCapabilityChip
import com.pixijoy.designsystem.PixiCapabilityKind
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.CgHost
import com.pixijoy.movie.CgMatte
import com.pixijoy.movie.CgPreset
import com.pixijoy.movie.TimelineCgLayer

/** R7 / P1+P5 — CG lane: classical plates, Z/matte/camera until Filament binds. */
@Composable
fun MovieCgPanel(
    layers: List<TimelineCgLayer>,
    @Suppress("UNUSED_PARAMETER") totalMs: Long,
    onAdd: (CgPreset) -> Unit,
    onUpdate: (TimelineCgLayer) -> Unit,
    onClear: () -> Unit
) {
    val filament = CgHost.filamentAvailable()
    val capLabel = stringResource(
        if (filament) R.string.capability_live else R.string.capability_live_classical
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PixiCapabilityChip(
            kind = PixiCapabilityKind.Live,
            label = capLabel,
            testTag = "movie_cg_capability"
        )
        val hint = stringResource(R.string.studio_cg_hint)
        Text(
            hint,
            color = PixiTextMuted,
            modifier = Modifier
                .testTag("movie_cg_hint")
                .semantics { contentDescription = hint }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_cg_presets")
        ) {
            listOf(
                CgPreset.PropCube to R.string.studio_cg_prop_cube,
                CgPreset.GroundPlane to R.string.studio_cg_ground,
                CgPreset.SkyDome to R.string.studio_cg_sky,
                CgPreset.DustTrail to R.string.studio_cg_dust
            ).forEach { (preset, labelRes) ->
                val label = stringResource(labelRes)
                PixiPrimaryButton(
                    label,
                    { onAdd(preset) },
                    Modifier.testTag("movie_cg_${preset.name.lowercase()}"),
                    contentDescription = label
                )
            }
        }
        if (layers.isNotEmpty()) {
            layers.forEach { layer ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("movie_cg_layer_${layer.id}")
                ) {
                    Text(
                        "${layer.preset.name} · z ${layer.zOrder} · ${layer.matte.name}",
                        color = PixiText,
                        modifier = Modifier.testTag("movie_cg_layer_label")
                    )
                    Text(stringResource(R.string.studio_cg_scale), color = PixiTextMuted)
                    Slider(
                        value = layer.scale,
                        onValueChange = { onUpdate(layer.copy(scale = it)) },
                        valueRange = 0.35f..2.5f,
                        modifier = Modifier.testTag("movie_cg_scale")
                    )
                    Text(stringResource(R.string.studio_cg_yaw), color = PixiTextMuted)
                    Slider(
                        value = layer.yaw,
                        onValueChange = { onUpdate(layer.copy(yaw = it)) },
                        valueRange = -180f..180f,
                        modifier = Modifier.testTag("movie_cg_yaw")
                    )
                    Text(stringResource(R.string.studio_cg_pitch), color = PixiTextMuted)
                    Slider(
                        value = layer.pitch,
                        onValueChange = { onUpdate(layer.copy(pitch = it)) },
                        valueRange = -45f..45f,
                        modifier = Modifier.testTag("movie_cg_pitch")
                    )
                    Text(stringResource(R.string.studio_cg_z), color = PixiTextMuted)
                    Slider(
                        value = layer.zOrder.toFloat(),
                        onValueChange = { onUpdate(layer.copy(zOrder = it.toInt())) },
                        valueRange = -4f..4f,
                        steps = 7,
                        modifier = Modifier.testTag("movie_cg_z")
                    )
                    Text(stringResource(R.string.studio_cg_pos_x), color = PixiTextMuted)
                    Slider(
                        value = layer.x,
                        onValueChange = { onUpdate(layer.copy(x = it)) },
                        valueRange = -0.8f..0.8f,
                        modifier = Modifier.testTag("movie_cg_x")
                    )
                    Text(stringResource(R.string.studio_cg_pos_y), color = PixiTextMuted)
                    Slider(
                        value = layer.y,
                        onValueChange = { onUpdate(layer.copy(y = it)) },
                        valueRange = -0.8f..0.8f,
                        modifier = Modifier.testTag("movie_cg_y")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                onUpdate(layer.copy(followTrack = !layer.followTrack))
                            },
                            modifier = Modifier.testTag("movie_cg_follow_track")
                        ) {
                            Text(
                                if (layer.followTrack) {
                                    stringResource(R.string.studio_cg_unfollow)
                                } else {
                                    stringResource(R.string.studio_cg_follow)
                                },
                                color = PixiText
                            )
                        }
                        TextButton(
                            onClick = {
                                onUpdate(
                                    layer.copy(
                                        endYaw = if (layer.endYaw.isNaN()) layer.yaw + 40f else Float.NaN,
                                        endScale = if (layer.endScale.isNaN()) {
                                            (layer.scale * 1.25f).coerceAtMost(2.5f)
                                        } else {
                                            Float.NaN
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.testTag("movie_cg_cam_key")
                        ) {
                            Text(
                                if (layer.endYaw.isNaN()) {
                                    stringResource(R.string.studio_cg_cam_key_on)
                                } else {
                                    stringResource(R.string.studio_cg_cam_key_off)
                                },
                                color = PixiText
                            )
                        }
                    }
                    Text(stringResource(R.string.studio_cg_matte), color = PixiTextMuted)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .testTag("movie_cg_mattes")
                    ) {
                        listOf(
                            CgMatte.None to R.string.studio_cg_matte_none,
                            CgMatte.SoftOval to R.string.studio_cg_matte_oval,
                            CgMatte.ChromaEdge to R.string.studio_cg_matte_chroma,
                            CgMatte.RectWindow to R.string.studio_cg_matte_rect
                        ).forEach { (matte, res) ->
                            TextButton(
                                onClick = { onUpdate(layer.copy(matte = matte)) },
                                modifier = Modifier.testTag(
                                    "movie_cg_matte_${matte.name.lowercase()}"
                                )
                            ) {
                                Text(stringResource(res), color = PixiText)
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = onClear,
                modifier = Modifier.testTag("movie_cg_clear")
            ) {
                Text(stringResource(R.string.studio_cg_clear), color = PixiTextMuted)
            }
        }
    }
}
