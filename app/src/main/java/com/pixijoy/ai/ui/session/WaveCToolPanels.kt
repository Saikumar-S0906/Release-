package com.pixijoy.ai.ui.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.ml.pack.PackId
import com.pixijoy.photo.PhotoPreset

/**
 * C5/C8 tool panels kept outside [PhotoSessionScreen] to avoid DEX VerifyError
 * from an oversized composable method.
 */
@Composable
private fun SessionLabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onChange: (Float) -> Unit
) {
    Text(label, color = PixiTextMuted)
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
fun WarpToolPanel(
    onApplyPerspective: (Float, Float) -> Unit,
    onApplyLiquify: (Float, Float, Float, Float) -> Unit
) {
    var horizontal by remember { mutableFloatStateOf(0.08f) }
    var vertical by remember { mutableFloatStateOf(0.05f) }
    var liquifyAmount by remember { mutableFloatStateOf(0.35f) }
    var liquifyRadius by remember { mutableFloatStateOf(0.18f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_warp_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("session_warp_hint")
        )
        SessionLabeledSlider(
            stringResource(R.string.studio_perspective_h),
            horizontal,
            -0.35f..0.35f,
            "warp_perspective_h"
        ) { horizontal = it }
        SessionLabeledSlider(
            stringResource(R.string.studio_perspective_v),
            vertical,
            -0.35f..0.35f,
            "warp_perspective_v"
        ) { vertical = it }
        PixiPrimaryButton(
            stringResource(R.string.studio_apply_perspective),
            { onApplyPerspective(horizontal, vertical) },
            Modifier.testTag("session_apply_perspective")
        )
        SessionLabeledSlider(
            stringResource(R.string.studio_liquify_amount),
            liquifyAmount,
            -1f..1f,
            "warp_liquify_amount"
        ) { liquifyAmount = it }
        SessionLabeledSlider(
            stringResource(R.string.studio_liquify_radius),
            liquifyRadius,
            0.05f..0.4f,
            "warp_liquify_radius"
        ) { liquifyRadius = it }
        PixiPrimaryButton(
            stringResource(R.string.studio_apply_liquify),
            { onApplyLiquify(0.5f, 0.5f, liquifyRadius, liquifyAmount) },
            Modifier.testTag("session_apply_liquify")
        )
    }
}

@Composable
fun SoftenToolPanel(onApply: (Float) -> Unit) {
    var amount by remember { mutableFloatStateOf(0.4f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_soften_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("session_soften_hint")
        )
        SessionLabeledSlider(
            stringResource(R.string.studio_soften_amount),
            amount,
            0f..1f,
            "soften_amount"
        ) { amount = it }
        PixiPrimaryButton(
            stringResource(R.string.studio_apply),
            { onApply(amount) },
            Modifier.testTag("session_apply_soften")
        )
    }
}

@Composable
fun SuperResToolPanel(onApply: (Int) -> Unit) {
    var scale by remember { mutableIntStateOf(2) }
    val ready = rememberPackEngineReady(PackId.SuperRes)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_super_res_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("session_super_res_hint")
        )
        PackStatusRow(
            packId = PackId.SuperRes,
            statusTag = "session_super_res_pack_status",
            downloadTag = "session_super_res_pack_download"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { scale = 2 }, modifier = Modifier.testTag("session_super_res_2x")) {
                Text(stringResource(R.string.studio_super_res_2x), color = PixiText)
            }
            TextButton(onClick = { scale = 4 }, modifier = Modifier.testTag("session_super_res_4x")) {
                Text(stringResource(R.string.studio_super_res_4x), color = PixiText)
            }
        }
        PixiPrimaryButton(
            stringResource(R.string.studio_apply),
            { onApply(scale) },
            Modifier.testTag("session_apply_super_res"),
            enabled = ready
        )
    }
}

@Composable
fun DenoiseToolPanel(onApply: (Float) -> Unit) {
    var amount by remember { mutableFloatStateOf(0.4f) }
    val ready = rememberPackEngineReady(PackId.Denoise)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_denoise_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("session_denoise_hint")
        )
        PackStatusRow(
            packId = PackId.Denoise,
            statusTag = "session_denoise_pack_status",
            downloadTag = "session_denoise_pack_download"
        )
        SessionLabeledSlider(
            stringResource(R.string.studio_denoise_amount),
            amount,
            0f..1f,
            "denoise_amount"
        ) { amount = it }
        PixiPrimaryButton(
            stringResource(R.string.studio_apply),
            { onApply(amount) },
            Modifier.testTag("session_apply_denoise"),
            enabled = ready
        )
    }
}

@Composable
fun PresetToolPanel(
    presets: List<PhotoPreset>,
    onSave: (String) -> Unit,
    onApply: (String) -> Unit
) {
    var name by remember { mutableStateOf("My look") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.studio_presets_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("session_presets_hint")
        )
        PixiPrimaryButton(
            stringResource(R.string.studio_preset_save),
            { onSave(name) },
            Modifier.testTag("session_preset_save")
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            presets.take(8).forEach { preset ->
                TextButton(
                    onClick = { onApply(preset.id) },
                    modifier = Modifier.testTag("session_preset_${preset.id}")
                ) {
                    Text(preset.name, color = PixiText)
                }
            }
        }
    }
}

@Composable
fun FillToolPanel(
    brush: Float,
    onBrush: (Float) -> Unit,
    onUndoStroke: () -> Unit,
    onClear: () -> Unit,
    onApply: (neural: Boolean) -> Unit
) {
    val neuralReady = rememberPackEngineReady(PackId.Inpaint)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SessionLabeledSlider(
            stringResource(R.string.studio_brush),
            brush,
            0.02f..0.12f,
            "session_brush_size",
            onBrush
        )
        Text(
            stringResource(R.string.studio_fill_hint),
            color = PixiTextMuted,
            modifier = Modifier.testTag("session_fill_hint")
        )
        PackStatusRow(
            packId = PackId.Inpaint,
            statusTag = "session_fill_pack_status",
            downloadTag = "session_fill_pack_download"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onUndoStroke, modifier = Modifier.testTag("session_fill_undo")) {
                Text(stringResource(R.string.studio_fill_undo_stroke), color = PixiTextMuted)
            }
            TextButton(onClick = onClear, modifier = Modifier.testTag("session_fill_clear")) {
                Text(stringResource(R.string.studio_fill_clear_strokes), color = PixiTextMuted)
            }
        }
        PixiPrimaryButton(
            stringResource(R.string.studio_fill_apply_nearby),
            { onApply(false) },
            Modifier.testTag("session_apply_fill")
        )
        PixiPrimaryButton(
            stringResource(R.string.studio_fill_apply_neural),
            { onApply(true) },
            Modifier.testTag("session_apply_fill_neural"),
            enabled = neuralReady
        )
    }
}
