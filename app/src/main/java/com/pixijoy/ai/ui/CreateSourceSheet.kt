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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CreateSourceSheet(
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onCamera: () -> Unit,
    onSample: () -> Unit,
    onOpenFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // ModalBottomSheet is its own window. MainActivity's testTagsAsResourceId does not apply
    // here, same as ExportSheet and ProjectRenameDialog. Without this, create_source_sheet and
    // source_* never become UIAutomator resource-ids. The flag is not spoken by TalkBack.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixiSurface2,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("create_source_sheet")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .testTag("create_source_sheet_body"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.create_source_title),
                color = PixiText,
                fontWeight = FontWeight.SemiBold
            )
            PixiPrimaryButton(
                text = stringResource(R.string.studio_photo),
                onClick = onPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("source_photo")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.create_source_files),
                onClick = onOpenFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("source_files")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.studio_camera_video),
                onClick = onVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("source_video")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.create_source_camera),
                onClick = onCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("source_camera")
            )
            PixiSecondaryButton(
                text = stringResource(R.string.studio_use_sample),
                onClick = onSample,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("source_sample")
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
