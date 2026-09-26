package com.pixijoy.ai.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiAccentSoft
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiChip
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

enum class ExportQualityPreset(val quality: Int) {
    Balanced(78),
    High(90),
    Maximum(100);

    companion object {
        fun fromQuality(q: Int): ExportQualityPreset = when {
            q <= 84 -> Balanced
            q <= 95 -> High
            else -> Maximum
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExportSheet(
    title: String,
    formats: List<Pair<String, String>>,
    selectedKey: String,
    quality: Int,
    showQuality: Boolean,
    destination: String,
    exporting: Boolean,
    progress: Float = 0f,
    onSelectFormat: (String) -> Unit,
    onQuality: (Int) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
    resolutions: List<Pair<Int, String>> = emptyList(),
    selectedHeight: Int = 720,
    onSelectHeight: (Int) -> Unit = {},
    showKeepExif: Boolean = false,
    keepExif: Boolean = false,
    onKeepExif: (Boolean) -> Unit = {},
    showColorIntent: Boolean = false,
    colorIntentKey: String = "srgb",
    onColorIntent: (String) -> Unit = {},
    showHdrMode: Boolean = false,
    hdrModeKey: String = "sdr",
    onHdrMode: (String) -> Unit = {},
    successUri: String? = null,
    onView: () -> Unit = {},
    onShare: () -> Unit = {},
    onDone: () -> Unit = {},
    onRenameProject: (() -> Unit)? = null,
    showLeaveSafeHint: Boolean = false
) {
    Dialog(onDismissRequest = { if (!exporting) onDismiss() }) {
        Column(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .clip(RoundedCornerShape(24.dp))
                .background(PixiSurface2)
                .padding(24.dp)
                .testTag("export_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (successUri != null) {
                Text(
                    stringResource(R.string.export_success_title),
                    color = PixiText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("export_success_title")
                )
                Text(destination, color = PixiTextMuted)
                val viewLabel = stringResource(R.string.export_view)
                val shareLabel = stringResource(R.string.export_share)
                val renameLabel = stringResource(R.string.export_name_project)
                val doneLabel = stringResource(R.string.export_done)
                PixiPrimaryButton(
                    text = viewLabel,
                    onClick = onView,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_view")
                )
                TextButton(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("export_share")
                        .semantics { contentDescription = shareLabel }
                ) {
                    Text(shareLabel, color = PixiAccent, modifier = Modifier.clearAndSetSemantics { })
                }
                if (onRenameProject != null) {
                    TextButton(
                        onClick = onRenameProject,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("export_rename")
                            .semantics { contentDescription = renameLabel }
                    ) {
                        Text(renameLabel, color = PixiText, modifier = Modifier.clearAndSetSemantics { })
                    }
                }
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("export_done")
                        .semantics { contentDescription = doneLabel }
                ) {
                    Text(doneLabel, color = PixiTextMuted, modifier = Modifier.clearAndSetSemantics { })
                }
                return@Column
            }

            Text(title, color = PixiText, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(destination, color = PixiTextMuted)
                Text(stringResource(R.string.studio_export_no_watermark), color = PixiTextMuted)
                if (showLeaveSafeHint) {
                    Text(
                        stringResource(R.string.export_leave_safe),
                        color = PixiTextMuted,
                        modifier = Modifier.testTag("export_leave_safe")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { (key, label) ->
                        val selected = selectedKey == key
                        PixiChip(
                            label = label,
                            selected = selected,
                            onClick = { onSelectFormat(key) },
                            modifier = Modifier.testTag("export_format_$key")
                        )
                    }
                }
                if (showKeepExif) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_keep_exif_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(
                                    if (keepExif) R.string.studio_export_exif_keep
                                    else R.string.studio_export_exif_strip
                                ),
                                color = PixiText
                            )
                            Text(
                                stringResource(R.string.studio_export_exif_gps_hint),
                                color = PixiTextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = keepExif,
                            onCheckedChange = onKeepExif,
                            colors = SwitchDefaults.colors(checkedTrackColor = PixiAccent),
                            modifier = Modifier.testTag("export_keep_exif")
                        )
                    }
                }
                if (showColorIntent) {
                    Text(stringResource(R.string.studio_export_color_label), color = PixiTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "srgb" to stringResource(R.string.studio_export_color_srgb),
                            "display_p3" to stringResource(R.string.studio_export_color_p3)
                        ).forEach { (key, label) ->
                            PixiChip(
                                label = label,
                                selected = colorIntentKey == key,
                                onClick = { onColorIntent(key) },
                                modifier = Modifier.testTag("export_color_$key")
                            )
                        }
                    }
                }
                if (showHdrMode && selectedKey != "gif") {
                    Text(stringResource(R.string.studio_export_hdr_label), color = PixiTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "sdr" to stringResource(R.string.studio_export_hdr_sdr),
                            "keep" to stringResource(R.string.studio_export_hdr_keep)
                        ).forEach { (key, label) ->
                            PixiChip(
                                label = label,
                                selected = hdrModeKey == key,
                                onClick = { onHdrMode(key) },
                                modifier = Modifier.testTag("export_hdr_$key")
                            )
                        }
                    }
                    if (hdrModeKey == "keep") {
                        Text(
                            stringResource(R.string.studio_export_hdr_keep_hint),
                            color = PixiTextMuted,
                            modifier = Modifier.testTag("export_hdr_keep_hint")
                        )
                    }
                }
                if (resolutions.isNotEmpty() && selectedKey != "gif") {
                    Text(stringResource(R.string.studio_export_resolution), color = PixiTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        resolutions.forEach { (height, label) ->
                            val selected = selectedHeight == height
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) PixiAccentSoft else PixiBg)
                                    .clickable { onSelectHeight(height) }
                                    .testTag("export_height_$height")
                                    .semantics {
                                        role = Role.RadioButton
                                        this.selected = selected
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    label,
                                    color = PixiText,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    height.toString(),
                                    color = PixiTextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("export_height_px_$height")
                                )
                            }
                        }
                    }
                    if (selectedHeight >= 2160) {
                        Text(
                            stringResource(R.string.studio_export_4k_cost),
                            color = PixiTextMuted,
                            modifier = Modifier.testTag("export_4k_cost_hint")
                        )
                    }
                }
                if (showQuality) {
                    Text(stringResource(R.string.studio_export_quality_label), color = PixiTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportQualityPreset.entries.forEach { preset ->
                            val selected = ExportQualityPreset.fromQuality(quality) == preset
                            val label = when (preset) {
                                ExportQualityPreset.Balanced -> stringResource(R.string.export_quality_balanced)
                                ExportQualityPreset.High -> stringResource(R.string.export_quality_high)
                                ExportQualityPreset.Maximum -> stringResource(R.string.export_quality_maximum)
                            }
                            PixiChip(
                                label = label,
                                selected = selected,
                                onClick = { onQuality(preset.quality) },
                                modifier = Modifier.testTag("export_quality_${preset.name.lowercase()}")
                            )
                        }
                    }
                }
                if (exporting) {
                    val pct = (progress * 100f).toInt().coerceIn(0, 100)
                    val progressLabel = if (pct > 0) {
                        stringResource(R.string.studio_export_pct, pct)
                    } else {
                        stringResource(R.string.studio_exporting)
                    }
                    Text(
                        progressLabel,
                        color = PixiTextMuted,
                        modifier = Modifier.testTag("export_sheet_progress_label")
                    )
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_sheet_progress")
                            .semantics { contentDescription = progressLabel },
                        color = PixiAccent
                    )
                }
            }
            PixiPrimaryButton(
                text = if (exporting) stringResource(R.string.studio_exporting) else stringResource(R.string.studio_export),
                onClick = onExport,
                enabled = !exporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_sheet_confirm")
            )
            TextButton(
                onClick = onDismiss,
                enabled = !exporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
        }
    }
}
