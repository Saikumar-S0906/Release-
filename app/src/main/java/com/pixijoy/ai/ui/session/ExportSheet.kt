package com.pixijoy.ai.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

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
    onKeepExif: (Boolean) -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .clip(RoundedCornerShape(24.dp))
                .background(PixiSurface2)
                .padding(24.dp)
                .testTag("export_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = PixiText, fontWeight = FontWeight.SemiBold)
            Text(destination, color = PixiTextMuted)
            Text(stringResource(R.string.studio_export_no_watermark), color = PixiTextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEach { (key, label) ->
                    Text(
                        label,
                        color = PixiText,
                        fontWeight = if (selectedKey == key) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selectedKey == key) PixiAccent else PixiBg)
                            .clickable { onSelectFormat(key) }
                            .testTag("export_format_$key")
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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
            if (resolutions.isNotEmpty() && selectedKey != "gif") {
                Text(stringResource(R.string.studio_export_resolution), color = PixiTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    resolutions.forEach { (height, label) ->
                        val selected = selectedHeight == height
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) PixiAccent else PixiBg)
                                .clickable { onSelectHeight(height) }
                                .testTag("export_height_$height")
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
                Text(stringResource(R.string.studio_export_quality, quality), color = PixiTextMuted)
                Slider(
                    value = quality.toFloat(),
                    onValueChange = { onQuality(it.toInt()) },
                    valueRange = 60f..100f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("export_quality")
                )
            }
            if (exporting) {
                val pct = (progress * 100f).toInt().coerceIn(0, 100)
                Text(
                    if (pct > 0) stringResource(R.string.studio_export_pct, pct)
                    else stringResource(R.string.studio_exporting),
                    color = PixiTextMuted,
                    modifier = Modifier.testTag("export_sheet_progress_label")
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_sheet_progress"),
                    color = PixiAccent
                )
            }
            PixiPrimaryButton(
                text = if (exporting) stringResource(R.string.studio_exporting) else stringResource(R.string.studio_export),
                onClick = onExport,
                enabled = !exporting,
                modifier = Modifier.testTag("export_sheet_confirm")
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
        }
    }
}
