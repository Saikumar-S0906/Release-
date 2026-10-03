package com.pixijoy.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    val shortHeight = LocalConfiguration.current.screenHeightDp < 560
    val gap = if (shortHeight) 6.dp else 12.dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixiSurface2,
        // ModalBottomSheet is its own window, so MainActivity's testTagsAsResourceId
        // does not reach studio_use_sample. The flag is not spoken by TalkBack.
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("media_pick_sheet")
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (shortHeight) 12.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
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
