package com.pixijoy.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@Composable
fun MediaSourceSheet(
    title: String,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onSample: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(PixiSurface2)
                .padding(24.dp)
                .testTag("media_pick_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = PixiText, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.studio_pick_body),
                color = PixiTextMuted
            )
            PixiPrimaryButton(
                text = stringResource(R.string.studio_gallery),
                onClick = onGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_gallery")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.studio_take_photo),
                onClick = onCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_camera")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.studio_use_sample),
                onClick = onSample,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_use_sample")
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
        }
    }
}
