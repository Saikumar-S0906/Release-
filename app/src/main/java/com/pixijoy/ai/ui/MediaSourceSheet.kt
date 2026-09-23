package com.pixijoy.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSourceSheet(
    title: String,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onSample: () -> Unit,
    onDismiss: () -> Unit,
    isMovie: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixiSurface2,
        modifier = Modifier.testTag("media_pick_sheet")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = PixiText, fontWeight = FontWeight.SemiBold)
            PixiPrimaryButton(
                text = stringResource(R.string.studio_gallery),
                onClick = onGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("studio_gallery")
            )
            PixiSecondaryButton(
                text = stringResource(
                    if (isMovie) R.string.studio_camera_video else R.string.studio_take_photo
                ),
                onClick = onCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("studio_camera")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.studio_use_sample),
                onClick = onSample,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("studio_use_sample")
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
