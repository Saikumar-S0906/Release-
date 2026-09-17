package com.pixijoy.ai.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@Composable
fun ExportSheet(
    title: String,
    formats: List<Pair<String, String>>,
    selectedKey: String,
    quality: Int,
    showQuality: Boolean,
    destination: String,
    exporting: Boolean,
    onSelectFormat: (String) -> Unit,
    onQuality: (Int) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
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
