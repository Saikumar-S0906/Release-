package com.pixijoy.studio.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiCapabilityChip
import com.pixijoy.designsystem.PixiCapabilityKind
import com.pixijoy.designsystem.PixiHint
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.ml.pack.PackId
import com.pixijoy.movie.CgHost
import com.pixijoy.movie.CgMatte
import com.pixijoy.movie.CgPreset
import com.pixijoy.movie.OptFeatureModules
import com.pixijoy.movie.TimelineCgLayer
import kotlin.math.roundToInt

/**
 * Which plate controls the classical renderer actually reads.
 * Sky ignores scale, yaw, pitch, and position. Ground ignores yaw and pitch.
 * Dust ignores pitch. Cube and a loaded mesh use the full prop set.
 */
internal enum class CgOption {
    Scale,
    Yaw,
    Pitch,
    Place,
    Follow,
    CamKey,
    Matte,
    Z
}

internal fun cgOptions(preset: CgPreset): Set<CgOption> = when (preset) {
    CgPreset.PropCube, CgPreset.PropGltf -> CgOption.entries.toSet()
    CgPreset.GroundPlane -> setOf(
        CgOption.Scale,
        CgOption.Place,
        CgOption.Follow,
        CgOption.CamKey,
        CgOption.Matte,
        CgOption.Z
    )
    CgPreset.SkyDome -> setOf(CgOption.Matte, CgOption.Z)
    CgPreset.DustTrail -> setOf(
        CgOption.Scale,
        CgOption.Yaw,
        CgOption.Place,
        CgOption.Follow,
        CgOption.CamKey,
        CgOption.Matte,
        CgOption.Z
    )
    CgPreset.None -> emptySet()
}

internal fun cgScaleLabelRes(preset: CgPreset): Int = when (preset) {
    CgPreset.GroundPlane -> R.string.studio_cg_horizon
    CgPreset.DustTrail -> R.string.studio_cg_amount
    else -> R.string.studio_cg_scale
}

internal fun cgYawLabelRes(preset: CgPreset): Int = when (preset) {
    CgPreset.DustTrail -> R.string.studio_cg_drift
    else -> R.string.studio_cg_yaw
}

internal fun cgNoteRes(preset: CgPreset): Int = when (preset) {
    CgPreset.PropCube -> R.string.studio_cg_cube_note
    CgPreset.GroundPlane -> R.string.studio_cg_ground_note
    CgPreset.SkyDome -> R.string.studio_cg_sky_note
    CgPreset.DustTrail -> R.string.studio_cg_dust_note
    CgPreset.PropGltf -> R.string.studio_cg_gltf_note
    CgPreset.None -> R.string.studio_cg_hint
}

/** R7 / P1 — classical plates, plus Filament prop lighting when `:feature_cg` is installed. */
@Composable
fun MovieCgPanel(
    layers: List<TimelineCgLayer>,
    @Suppress("UNUSED_PARAMETER") totalMs: Long,
    onAdd: (CgPreset) -> Unit,
    onLoadProp: () -> Unit,
    onUpdate: (TimelineCgLayer) -> Unit,
    onSettled: () -> Unit = {},
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var filament by remember { mutableStateOf(CgHost.filamentAvailable()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var knownIds by remember { mutableStateOf(layers.map { it.id }.toSet()) }
    LaunchedEffect(Unit) {
        OptFeatureModules.ensureInstalled(context, OptFeatureModules.TITLES3D)
        OptFeatureModules.ensureInstalled(context, OptFeatureModules.CG)
        OptFeatureModules.tryBindCg()
        filament = CgHost.filamentAvailable()
    }
    LaunchedEffect(layers.map { it.id }) {
        val now = layers.map { it.id }
        val added = now.filter { it !in knownIds }
        val current = selectedId
        knownIds = now.toSet()
        selectedId = when {
            added.isNotEmpty() -> added.last()
            current != null && current in now -> current
            else -> now.lastOrNull()
        }
    }
    val capLabel = stringResource(
        if (filament) R.string.capability_live_filament else R.string.capability_live_classical
    )
    val selected = layers.lastOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PixiCapabilityChip(
            kind = PixiCapabilityKind.Live,
            label = capLabel,
            testTag = "movie_cg_capability"
        )
        val hint = stringResource(
            if (filament) R.string.studio_cg_hint_filament else R.string.studio_cg_hint
        )
        Text(
            hint,
            color = PixiTextMuted,
            style = PixiHint,
            modifier = Modifier
                .testTag("movie_cg_hint")
                .semantics { contentDescription = hint }
        )
        PackStatusRow(
            packId = PackId.Depth,
            statusTag = "movie_cg_depth_status",
            downloadTag = "movie_cg_depth_download"
        )
        StudioHScrollRow(modifier = Modifier.testTag("movie_cg_presets")) {
            listOf(
                CgPreset.PropCube to R.string.studio_cg_prop_cube,
                CgPreset.GroundPlane to R.string.studio_cg_ground,
                CgPreset.SkyDome to R.string.studio_cg_sky,
                CgPreset.DustTrail to R.string.studio_cg_dust
            ).forEach { (preset, labelRes) ->
                val label = stringResource(labelRes)
                StudioChip(
                    label = label,
                    selected = selected?.preset == preset,
                    tag = "movie_cg_${preset.name.lowercase()}"
                ) {
                    focusPreset(layers, preset, selectedId, onAdd) { selectedId = it }
                }
            }
            val loadLabel = stringResource(R.string.studio_cg_load_gltf)
            StudioChip(
                label = loadLabel,
                selected = selected?.preset == CgPreset.PropGltf,
                tag = "movie_cg_load_gltf"
            ) {
                val existing = layers.lastOrNull { it.preset == CgPreset.PropGltf }
                if (existing != null) {
                    selectedId = existing.id
                } else {
                    onLoadProp()
                }
            }
        }
        if (selected != null) {
            CgLayerOptions(
                layer = selected,
                onUpdate = onUpdate,
                onSettled = onSettled,
                onLoadProp = onLoadProp
            )
        }
        if (layers.isNotEmpty()) {
            StudioChip(
                label = stringResource(R.string.studio_cg_clear),
                selected = false,
                tag = "movie_cg_clear"
            ) {
                selectedId = null
                knownIds = emptySet()
                onClear()
            }
        }
    }
}

private fun focusPreset(
    layers: List<TimelineCgLayer>,
    preset: CgPreset,
    selectedId: String?,
    onAdd: (CgPreset) -> Unit,
    onSelect: (String) -> Unit
) {
    val existing = layers.lastOrNull { it.preset == preset }
    if (existing != null) {
        onSelect(existing.id)
    } else if (selectedId != "cg${layers.size}") {
        onSelect("cg${layers.size}")
        onAdd(preset)
    }
}

@Composable
private fun CgLayerOptions(
    layer: TimelineCgLayer,
    onUpdate: (TimelineCgLayer) -> Unit,
    onSettled: () -> Unit,
    onLoadProp: () -> Unit
) {
    val options = cgOptions(layer.preset)
    val title = if (layer.propFile.isBlank()) {
        stringResource(presetLabelRes(layer.preset))
    } else {
        layer.propFile
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag("movie_cg_options_${layer.preset.name.lowercase()}")
    ) {
        StudioHint(title, "movie_cg_layer_label")
        Text(
            stringResource(cgNoteRes(layer.preset)),
            color = PixiTextMuted,
            style = PixiHint,
            modifier = Modifier.testTag("movie_cg_preset_note")
        )
        if (CgOption.Scale in options) {
            StudioLabeledSlider(
                label = stringResource(cgScaleLabelRes(layer.preset)),
                value = layer.scale,
                range = 0.35f..2.5f,
                tag = "movie_cg_scale",
                onChange = { onUpdate(layer.copy(scale = it)) },
                onSettled = onSettled
            )
        }
        if (CgOption.Yaw in options) {
            StudioLabeledSlider(
                label = stringResource(cgYawLabelRes(layer.preset)),
                value = layer.yaw,
                range = -180f..180f,
                tag = "movie_cg_yaw",
                onChange = { onUpdate(layer.copy(yaw = it)) },
                onSettled = onSettled
            )
        }
        if (CgOption.Pitch in options) {
            StudioLabeledSlider(
                label = stringResource(R.string.studio_cg_pitch),
                value = layer.pitch,
                range = -45f..45f,
                tag = "movie_cg_pitch",
                onChange = { onUpdate(layer.copy(pitch = it)) },
                onSettled = onSettled
            )
        }
        if (CgOption.Z in options) {
            StudioLabeledSlider(
                label = stringResource(R.string.studio_cg_z),
                value = layer.zOrder.toFloat(),
                range = -4f..4f,
                tag = "movie_cg_z",
                onChange = { onUpdate(layer.copy(zOrder = it.roundToInt().coerceIn(-4, 4))) },
                onSettled = onSettled
            )
        }
        if (CgOption.Place in options) {
            StudioLabeledSlider(
                label = stringResource(R.string.studio_cg_pos_x),
                value = layer.x,
                range = -0.8f..0.8f,
                tag = "movie_cg_x",
                onChange = { onUpdate(layer.copy(x = it)) },
                onSettled = onSettled
            )
            StudioLabeledSlider(
                label = stringResource(R.string.studio_cg_pos_y),
                value = layer.y,
                range = -0.8f..0.8f,
                tag = "movie_cg_y",
                onChange = { onUpdate(layer.copy(y = it)) },
                onSettled = onSettled
            )
        }
        if (CgOption.Follow in options || CgOption.CamKey in options || layer.preset == CgPreset.PropGltf) {
            StudioHScrollRow {
                if (CgOption.Follow in options) {
                    val follow = stringResource(
                        if (layer.followTrack) R.string.studio_cg_unfollow else R.string.studio_cg_follow
                    )
                    StudioChip(follow, layer.followTrack, "movie_cg_follow_track") {
                        onUpdate(layer.copy(followTrack = !layer.followTrack))
                        onSettled()
                    }
                }
                if (CgOption.CamKey in options) {
                    val keyed = !layer.endYaw.isNaN()
                    val key = stringResource(
                        if (keyed) R.string.studio_cg_cam_key_off else R.string.studio_cg_cam_key_on
                    )
                    StudioChip(key, keyed, "movie_cg_cam_key") {
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
                        onSettled()
                    }
                }
                if (layer.preset == CgPreset.PropGltf) {
                    StudioChip(
                        stringResource(R.string.studio_cg_load_another),
                        false,
                        "movie_cg_load_another"
                    ) { onLoadProp() }
                }
            }
        }
        if (CgOption.Matte in options) {
            StudioHint(stringResource(R.string.studio_cg_matte))
            StudioHScrollRow(modifier = Modifier.testTag("movie_cg_mattes")) {
                listOf(
                    CgMatte.None to R.string.studio_cg_matte_none,
                    CgMatte.SoftOval to R.string.studio_cg_matte_oval,
                    CgMatte.ChromaEdge to R.string.studio_cg_matte_chroma,
                    CgMatte.RectWindow to R.string.studio_cg_matte_rect
                ).forEach { (matte, res) ->
                    StudioChip(
                        label = stringResource(res),
                        selected = layer.matte == matte,
                        tag = "movie_cg_matte_${matte.name.lowercase()}"
                    ) {
                        onUpdate(layer.copy(matte = matte))
                        onSettled()
                    }
                }
            }
        }
    }
}

private fun presetLabelRes(preset: CgPreset): Int = when (preset) {
    CgPreset.PropCube -> R.string.studio_cg_prop_cube
    CgPreset.GroundPlane -> R.string.studio_cg_ground
    CgPreset.SkyDome -> R.string.studio_cg_sky
    CgPreset.DustTrail -> R.string.studio_cg_dust
    CgPreset.PropGltf -> R.string.studio_cg_load_gltf
    CgPreset.None -> R.string.studio_cg_clear
}
